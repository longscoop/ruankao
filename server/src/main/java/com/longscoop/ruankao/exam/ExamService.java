package com.longscoop.ruankao.exam;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.exam.persistence.ExamEntity;
import com.longscoop.ruankao.exam.persistence.ExamMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExamService {

    private final ExamMapper examMapper;

    public ExamService(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    @Transactional
    public long create(String code, String name, ExamStatus status) {
        String normalizedCode = requireText(code, "code");
        String normalizedName = requireText(name, "name");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }

        ExamEntity entity = new ExamEntity();
        entity.setCode(normalizedCode);
        entity.setName(normalizedName);
        entity.setStatus(status);
        examMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public Optional<ExamEntity> findById(long id) {
        return Optional.ofNullable(examMapper.selectById(id));
    }

    @Transactional(readOnly = true)
    public List<ExamEntity> listActive() {
        return examMapper.selectList(
                Wrappers.<ExamEntity>lambdaQuery()
                        .eq(ExamEntity::getStatus, ExamStatus.ACTIVE)
                        .orderByAsc(ExamEntity::getId));
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
