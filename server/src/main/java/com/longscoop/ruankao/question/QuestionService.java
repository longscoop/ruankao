package com.longscoop.ruankao.question;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.model.QuestionType;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeEntity;
import com.longscoop.ruankao.question.persistence.QuestionKnowledgeMapper;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class QuestionService {

    private static final double WEIGHT_TOLERANCE = 0.000001;

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public QuestionService(
            QuestionMapper questionMapper,
            QuestionKnowledgeMapper questionKnowledgeMapper,
            KnowledgePointMapper knowledgePointMapper) {
        this.questionMapper = questionMapper;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.knowledgePointMapper = knowledgePointMapper;
    }

    @Transactional
    public long create(
            long examId,
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            String content,
            List<QuestionKnowledgeLink> links) {
        return create(examId, type, source, status, difficulty, content, null, links);
    }

    @Transactional
    public long create(
            long examId,
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            String content,
            String standardAnswer,
            List<QuestionKnowledgeLink> links) {
        requireEnums(type, source, status, difficulty);
        String normalizedContent = requireText(content);
        String normalizedStandardAnswer = normalizeStandardAnswer(status, standardAnswer);
        validateLinks(examId, links);

        QuestionEntity question = new QuestionEntity();
        question.setExamId(examId);
        question.setType(type);
        question.setSource(source);
        question.setStatus(status);
        question.setDifficulty(difficulty);
        question.setContent(normalizedContent);
        question.setStandardAnswer(normalizedStandardAnswer);
        questionMapper.insert(question);

        for (QuestionKnowledgeLink link : links) {
            QuestionKnowledgeEntity entity = new QuestionKnowledgeEntity();
            entity.setQuestionId(question.getId());
            entity.setKnowledgeId(link.knowledgeId());
            entity.setWeight(link.weight());
            entity.setPrimaryFlag(link.primary());
            questionKnowledgeMapper.insert(entity);
        }

        return question.getId();
    }

    @Transactional(readOnly = true)
    public Optional<QuestionEntity> findById(long id) {
        return Optional.ofNullable(questionMapper.selectById(id));
    }

    @Transactional(readOnly = true)
    public List<QuestionKnowledgeEntity> listKnowledgeLinks(long questionId) {
        return questionKnowledgeMapper.selectList(
                Wrappers.<QuestionKnowledgeEntity>lambdaQuery()
                        .eq(QuestionKnowledgeEntity::getQuestionId, questionId)
                        .orderByDesc(QuestionKnowledgeEntity::getPrimaryFlag)
                        .orderByAsc(QuestionKnowledgeEntity::getId));
    }

    private void requireEnums(
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            QuestionDifficulty difficulty) {
        if (type == null || source == null || status == null || difficulty == null) {
            throw new IllegalArgumentException("question type, source, status and difficulty are required");
        }
    }

    private String requireText(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
        return content.trim();
    }

    private String normalizeStandardAnswer(QuestionStatus status, String standardAnswer) {
        if (standardAnswer == null || standardAnswer.trim().isEmpty()) {
            if (status == QuestionStatus.PUBLISHED) {
                throw new IllegalArgumentException("published question requires a standard answer");
            }
            return null;
        }
        return standardAnswer.trim();
    }

    private void validateLinks(long examId, List<QuestionKnowledgeLink> links) {
        if (links == null || links.isEmpty()) {
            throw new IllegalArgumentException("at least one knowledge link is required");
        }

        Set<Long> knowledgeIds = new HashSet<>();
        double totalWeight = 0.0;
        int primaryCount = 0;

        for (QuestionKnowledgeLink link : links) {
            if (link == null) {
                throw new IllegalArgumentException("knowledge link is required");
            }
            if (!Double.isFinite(link.weight()) || link.weight() <= 0.0 || link.weight() > 1.0) {
                throw new IllegalArgumentException("knowledge weight must be greater than 0 and at most 1");
            }
            if (!knowledgeIds.add(link.knowledgeId())) {
                throw new IllegalArgumentException("duplicate knowledge link");
            }

            KnowledgePointEntity knowledge = knowledgePointMapper.selectById(link.knowledgeId());
            if (knowledge == null || !knowledge.getExamId().equals(examId)) {
                throw new IllegalArgumentException("knowledge point must belong to the same exam");
            }

            totalWeight += link.weight();
            if (link.primary()) {
                primaryCount++;
            }
        }

        if (Math.abs(totalWeight - 1.0) > WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException("knowledge weights must sum to 1.0");
        }
        if (primaryCount != 1) {
            throw new IllegalArgumentException("exactly one primary knowledge point is required");
        }
    }
}
