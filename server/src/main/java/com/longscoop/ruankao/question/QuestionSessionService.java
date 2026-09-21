package com.longscoop.ruankao.question;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.question.model.PracticeSource;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeMapper;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import com.longscoop.ruankao.question.persistence.QuestionSessionEntity;
import com.longscoop.ruankao.question.persistence.QuestionSessionItemEntity;
import com.longscoop.ruankao.question.persistence.QuestionSessionItemMapper;
import com.longscoop.ruankao.question.persistence.QuestionSessionMapper;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import com.longscoop.ruankao.question.persistence.WrongQuestionMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuestionSessionService {

    private static final int DEFAULT_SESSION_SIZE = 20;

    private final UserExamProfileService profileService;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgeMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionSessionMapper sessionMapper;
    private final QuestionSessionItemMapper itemMapper;
    private final QuestionAnswerService answerService;
    private final ObjectMapper objectMapper;

    public QuestionSessionService(
            UserExamProfileService profileService,
            QuestionMapper questionMapper,
            QuestionKnowledgeMapper questionKnowledgeMapper,
            KnowledgePointMapper knowledgeMapper,
            WrongQuestionMapper wrongQuestionMapper,
            QuestionSessionMapper sessionMapper,
            QuestionSessionItemMapper itemMapper,
            QuestionAnswerService answerService,
            ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.questionMapper = questionMapper;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.sessionMapper = sessionMapper;
        this.itemMapper = itemMapper;
        this.answerService = answerService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID start(long userId, PracticeSource source, Long knowledgeId) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        var profile = profileService.find(userId)
                .orElseThrow(() -> new IllegalStateException("exam profile is required"));
        long examId = profile.getExamId();

        if (knowledgeId != null) {
            KnowledgePointEntity knowledge = knowledgeMapper.selectById(knowledgeId);
            if (knowledge == null || !knowledge.getExamId().equals(examId)) {
                throw new IllegalArgumentException("knowledge point must belong to user exam");
            }
        }

        QuestionSessionEntity session = new QuestionSessionEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setExamId(examId);
        session.setSource(source);
        session.setKnowledgeId(knowledgeId);
        sessionMapper.insert(session);

        List<Long> questionIds = selectQuestionIds(userId, examId, source, knowledgeId);
        int order = 0;
        for (Long questionId : questionIds.stream().limit(DEFAULT_SESSION_SIZE).toList()) {
            QuestionSessionItemEntity item = new QuestionSessionItemEntity();
            item.setSessionId(session.getId());
            item.setQuestionId(questionId);
            item.setSortOrder(order++);
            itemMapper.insert(item);
        }
        return session.getId();
    }

    @Transactional(readOnly = true)
    public Optional<QuestionSessionEntity> findOwned(long userId, UUID sessionId) {
        QuestionSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Transactional(readOnly = true)
    public List<QuestionView> listQuestions(long userId, UUID sessionId) {
        requireOwned(userId, sessionId);
        return itemMapper.selectList(
                        Wrappers.<QuestionSessionItemEntity>lambdaQuery()
                                .eq(QuestionSessionItemEntity::getSessionId, sessionId)
                                .orderByAsc(QuestionSessionItemEntity::getSortOrder))
                .stream()
                .map(item -> toQuestionView(questionMapper.selectById(item.getQuestionId())))
                .toList();
    }

    @Transactional
    public AnswerFeedback answer(
            long userId,
            UUID sessionId,
            long questionId,
            String answer,
            Integer durationSeconds,
            AnswerConfidence confidence) {
        QuestionSessionEntity session = requireOwned(userId, sessionId);
        Long count = itemMapper.selectCount(
                Wrappers.<QuestionSessionItemEntity>lambdaQuery()
                        .eq(QuestionSessionItemEntity::getSessionId, sessionId)
                        .eq(QuestionSessionItemEntity::getQuestionId, questionId));
        if (count == null || count != 1) {
            throw new IllegalArgumentException("question is not part of session");
        }

        String idempotencyKey = "question-session:" + sessionId + ":" + questionId;
        QuestionAnswerResult result = answerService.submit(
                userId,
                questionId,
                sessionId,
                idempotencyKey,
                answer,
                durationSeconds,
                confidence,
                session.getSource().toAnswerSource());

        QuestionEntity question = questionMapper.selectById(questionId);
        return new AnswerFeedback(
                questionId,
                result.correct(),
                result.answerId(),
                result.correctAnswer(),
                question == null ? null : question.getExplanation());
    }

    @Transactional(readOnly = true)
    public Optional<QuestionView> findQuestionView(long questionId) {
        QuestionEntity question = questionMapper.selectById(questionId);
        if (question == null || question.getStatus() != QuestionStatus.PUBLISHED) {
            return Optional.empty();
        }
        return Optional.of(toQuestionView(question));
    }

    private QuestionSessionEntity requireOwned(long userId, UUID sessionId) {
        return findOwned(userId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("question session not found"));
    }

    private List<Long> selectQuestionIds(
            long userId,
            long examId,
            PracticeSource source,
            Long knowledgeId) {
        if (source == PracticeSource.WRONG_REVIEW) {
            List<Long> wrongIds = wrongQuestionMapper.selectList(
                            Wrappers.<WrongQuestionEntity>lambdaQuery()
                                    .eq(WrongQuestionEntity::getUserId, userId)
                                    .eq(WrongQuestionEntity::getStatus,
                                            com.longscoop.ruankao.question.model.WrongQuestionStatus.ACTIVE)
                                    .orderByDesc(WrongQuestionEntity::getLastWrongAt))
                    .stream()
                    .map(WrongQuestionEntity::getQuestionId)
                    .toList();
            return filterPublishedObjective(examId, wrongIds);
        }

        if (knowledgeId != null) {
            List<Long> linkedIds = questionKnowledgeMapper.selectList(
                            Wrappers.<QuestionKnowledgeEntity>lambdaQuery()
                                    .eq(QuestionKnowledgeEntity::getKnowledgeId, knowledgeId)
                                    .orderByDesc(QuestionKnowledgeEntity::getPrimaryFlag)
                                    .orderByAsc(QuestionKnowledgeEntity::getId))
                    .stream()
                    .map(QuestionKnowledgeEntity::getQuestionId)
                    .toList();
            return filterPublishedObjective(examId, linkedIds);
        }

        var query = Wrappers.<QuestionEntity>lambdaQuery()
                .eq(QuestionEntity::getExamId, examId)
                .eq(QuestionEntity::getStatus, QuestionStatus.PUBLISHED)
                .in(QuestionEntity::getType, QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE)
                .orderByAsc(QuestionEntity::getId);

        if (source == PracticeSource.REAL_EXAM) {
            query.eq(QuestionEntity::getSource, QuestionSource.REAL_EXAM);
        } else if (source == PracticeSource.AI_QUIZ) {
            query.eq(QuestionEntity::getSource, QuestionSource.AI_GENERATED);
        } else {
            query.in(QuestionEntity::getSource, QuestionSource.CHAPTER, QuestionSource.MANUAL);
        }
        return questionMapper.selectList(query).stream().map(QuestionEntity::getId).toList();
    }

    private List<Long> filterPublishedObjective(long examId, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return questionMapper.selectList(
                        Wrappers.<QuestionEntity>lambdaQuery()
                                .in(QuestionEntity::getId, ids)
                                .eq(QuestionEntity::getExamId, examId)
                                .eq(QuestionEntity::getStatus, QuestionStatus.PUBLISHED)
                                .in(QuestionEntity::getType,
                                        QuestionType.SINGLE_CHOICE,
                                        QuestionType.MULTIPLE_CHOICE))
                .stream()
                .map(QuestionEntity::getId)
                .toList();
    }

    private QuestionView toQuestionView(QuestionEntity question) {
        if (question == null) {
            throw new IllegalStateException("session question not found");
        }
        return new QuestionView(
                question.getId(),
                question.getType().name(),
                question.getDifficulty().name(),
                question.getContent(),
                question.getSource().name(),
                parseOptions(question.getOptionsJson()));
    }

    private List<QuestionOption> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(optionsJson);
            List<QuestionOption> options = new ArrayList<>();
            if (root.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    options.add(new QuestionOption(field.getKey(), field.getValue().asText()));
                }
            } else if (root.isArray()) {
                int index = 0;
                for (JsonNode node : root) {
                    if (node.isObject() && node.hasNonNull("key") && node.hasNonNull("text")) {
                        options.add(new QuestionOption(node.get("key").asText(), node.get("text").asText()));
                    } else if (node.isTextual()) {
                        options.add(new QuestionOption(String.valueOf((char) ('A' + index)), node.asText()));
                    }
                    index++;
                }
            }
            return options;
        } catch (Exception e) {
            throw new IllegalStateException("invalid question options_json", e);
        }
    }

    public record QuestionOption(String key, String text) {
    }

    public record QuestionView(
            long id,
            String type,
            String difficulty,
            String content,
            String source,
            List<QuestionOption> options) {
    }

    public record AnswerFeedback(
            long questionId,
            boolean correct,
            UUID answerRecordId,
            String standardAnswer,
            String explanation) {
    }
}
