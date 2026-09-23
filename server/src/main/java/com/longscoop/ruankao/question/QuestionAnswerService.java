package com.longscoop.ruankao.question;

import com.longscoop.ruankao.learning.engine.MasteryScoreCalculator;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.learning.model.AnswerSource;
import com.longscoop.ruankao.learning.model.MasteryUpdateInput;
import com.longscoop.ruankao.learning.persistence.AnswerRecordEntity;
import com.longscoop.ruankao.learning.persistence.IdempotentAnswerRecord;
import com.longscoop.ruankao.learning.persistence.MasteryApplicationService;
import com.longscoop.ruankao.learning.persistence.MasteryDelta;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuestionAnswerService {

    private final QuestionService questionService;
    private final MasteryApplicationService masteryApplicationService;
    private final WrongQuestionService wrongQuestionService;
    private final MasteryScoreCalculator masteryScoreCalculator = new MasteryScoreCalculator();

    public QuestionAnswerService(
            QuestionService questionService,
            MasteryApplicationService masteryApplicationService,
            WrongQuestionService wrongQuestionService) {
        this.questionService = questionService;
        this.masteryApplicationService = masteryApplicationService;
        this.wrongQuestionService = wrongQuestionService;
    }

    @Transactional
    public QuestionAnswerResult submit(
            long userId,
            long questionId,
            UUID sessionId,
            String idempotencyKey,
            String submittedAnswer,
            Integer durationSeconds,
            AnswerConfidence confidence,
            AnswerSource source) {
        if (userId <= 0 || questionId <= 0) {
            throw new IllegalArgumentException("userId and questionId must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        var existing = masteryApplicationService.findByIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            AnswerRecordEntity answer = existing.get();
            QuestionEntity persistedQuestion = questionService.findById(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalStateException("persisted question not found"));
            return new QuestionAnswerResult(
                    answer.getId(),
                    answer.getCorrect(),
                    persistedQuestion.getStandardAnswer());
        }

        QuestionEntity question = questionService.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("question not found"));
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalStateException("question is not published");
        }
        if (question.getStandardAnswer() == null || question.getStandardAnswer().trim().isEmpty()) {
            throw new IllegalStateException("published question has no standard answer");
        }

        String normalizedSubmittedAnswer = normalizeAnswer(submittedAnswer);
        boolean correct = normalizeAnswer(question.getStandardAnswer())
                .equals(normalizedSubmittedAnswer);

        IdempotentAnswerRecord write = masteryApplicationService.recordAnswerIdempotent(
                userId,
                questionId,
                sessionId,
                idempotencyKey,
                submittedAnswer,
                correct,
                durationSeconds,
                confidence,
                source);

        if (!write.created()) {
            AnswerRecordEntity answer = write.answer();
            QuestionEntity persistedQuestion = questionService.findById(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalStateException("persisted question not found"));
            return new QuestionAnswerResult(
                    answer.getId(),
                    answer.getCorrect(),
                    persistedQuestion.getStandardAnswer());
        }

        List<QuestionKnowledgeEntity> links = questionService.listKnowledgeLinks(questionId);
        List<MasteryDelta> deltas = new ArrayList<>(links.size());

        for (QuestionKnowledgeEntity link : links) {
            UserKnowledgeMasteryEntity current = masteryApplicationService
                    .findMastery(userId, link.getKnowledgeId())
                    .orElse(null);
            double currentScore = current == null ? 0.0 : current.getMasteryScore();
            int streakLength = current == null
                    ? 1
                    : correct
                        ? current.getCorrectStreak() + 1
                        : current.getWrongStreak() + 1;

            double nextScore = masteryScoreCalculator.calculate(
                    currentScore,
                    new MasteryUpdateInput(
                            correct,
                            question.getDifficulty(),
                            confidence,
                            streakLength,
                            link.getWeight()));

            deltas.add(new MasteryDelta(
                    link.getKnowledgeId(),
                    nextScore - currentScore,
                    correct));
        }

        if (!masteryApplicationService.apply(write.answer().getId(), deltas)) {
            throw new IllegalStateException("mastery was not applied to new answer");
        }

        if (!correct) {
            wrongQuestionService.recordWrong(userId, questionId);
        } else if (source == AnswerSource.WRONG_REVIEW) {
            double effectiveMastery = links.stream()
                    .map(link -> masteryApplicationService
                            .findMastery(userId, link.getKnowledgeId())
                            .orElseThrow(() -> new IllegalStateException("updated mastery not found"))
                            .getMasteryScore())
                    .min(Double::compareTo)
                    .orElse(0.0);
            wrongQuestionService.recordCorrectReview(userId, questionId, effectiveMastery);
        }

        return new QuestionAnswerResult(
                write.answer().getId(),
                correct,
                question.getStandardAnswer());
    }

    private String normalizeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("submittedAnswer is required");
        }
        return answer.trim().toUpperCase();
    }
}
