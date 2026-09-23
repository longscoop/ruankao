package com.longscoop.ruankao.knowledge;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;

    public KnowledgePointService(KnowledgePointMapper knowledgePointMapper) {
        this.knowledgePointMapper = knowledgePointMapper;
    }

    @Transactional
    public long create(
            long examId,
            Long parentId,
            int level,
            String code,
            String name,
            String description,
            int importance,
            double examFrequency,
            int estimatedMinutes,
            int sortOrder,
            KnowledgeStatus status) {
        validate(level, importance, examFrequency, estimatedMinutes, status);
        String normalizedCode = requireText(code, "code");
        String normalizedName = requireText(name, "name");

        if (parentId != null) {
            KnowledgePointEntity parent = knowledgePointMapper.selectById(parentId);
            if (parent == null || !parent.getExamId().equals(examId)) {
                throw new IllegalArgumentException("parent must belong to the same exam");
            }
        }

        Long duplicateCount = knowledgePointMapper.selectCount(
                Wrappers.<KnowledgePointEntity>lambdaQuery()
                        .eq(KnowledgePointEntity::getExamId, examId)
                        .eq(KnowledgePointEntity::getCode, normalizedCode));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new DataIntegrityViolationException("knowledge point code already exists in exam");
        }

        KnowledgePointEntity entity = new KnowledgePointEntity();
        entity.setExamId(examId);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setCode(normalizedCode);
        entity.setName(normalizedName);
        entity.setDescription(description);
        entity.setImportance(importance);
        entity.setExamFrequency(examFrequency);
        entity.setEstimatedMinutes(estimatedMinutes);
        entity.setSortOrder(sortOrder);
        entity.setStatus(status);

        knowledgePointMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public List<KnowledgePointEntity> listForExam(long examId) {
        return knowledgePointMapper.selectList(
                Wrappers.<KnowledgePointEntity>lambdaQuery()
                        .eq(KnowledgePointEntity::getExamId, examId)
                        .orderByAsc(KnowledgePointEntity::getLevel)
                        .orderByAsc(KnowledgePointEntity::getSortOrder)
                        .orderByAsc(KnowledgePointEntity::getId));
    }

    private void validate(
            int level,
            int importance,
            double examFrequency,
            int estimatedMinutes,
            KnowledgeStatus status) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (importance < 1 || importance > 5) {
            throw new IllegalArgumentException("importance must be between 1 and 5");
        }
        if (!Double.isFinite(examFrequency) || examFrequency < 0.0 || examFrequency > 100.0) {
            throw new IllegalArgumentException("examFrequency must be between 0 and 100");
        }
        if (estimatedMinutes <= 0) {
            throw new IllegalArgumentException("estimatedMinutes must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
