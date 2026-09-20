package com.longscoop.ruankao.course;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.course.persistence.UserVideoProgressEntity;
import com.longscoop.ruankao.course.persistence.UserVideoProgressMapper;
import com.longscoop.ruankao.course.persistence.VideoEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationMapper;
import com.longscoop.ruankao.course.persistence.VideoMapper;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class VideoProgressService {

    private static final BigDecimal COMPLETION_THRESHOLD = BigDecimal.valueOf(85);

    private final VideoMapper videoMapper;
    private final UserVideoProgressMapper progressMapper;
    private final VideoKnowledgeRelationMapper relationMapper;
    private final UserKnowledgeMasteryMapper masteryMapper;

    public VideoProgressService(
            VideoMapper videoMapper,
            UserVideoProgressMapper progressMapper,
            VideoKnowledgeRelationMapper relationMapper,
            UserKnowledgeMasteryMapper masteryMapper) {
        this.videoMapper = videoMapper;
        this.progressMapper = progressMapper;
        this.relationMapper = relationMapper;
        this.masteryMapper = masteryMapper;
    }

    @Transactional
    public UserVideoProgressEntity update(long userId, long videoId, int progressSeconds) {
        if (userId <= 0 || videoId <= 0) {
            throw new IllegalArgumentException("userId and videoId must be positive");
        }
        if (progressSeconds < 0) {
            throw new IllegalArgumentException("progressSeconds cannot be negative");
        }
        VideoEntity video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new IllegalArgumentException("video must exist");
        }

        UserVideoProgressEntity existing = progressMapper.select(userId, videoId);
        int previousProgress = existing == null ? 0 : existing.getProgressSeconds();
        int effectiveProgress = Math.min(video.getDurationSeconds(), Math.max(previousProgress, progressSeconds));
        BigDecimal completionRate = BigDecimal.valueOf(effectiveProgress)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(video.getDurationSeconds()), 2, RoundingMode.HALF_UP);
        boolean completed = completionRate.compareTo(COMPLETION_THRESHOLD) >= 0;

        progressMapper.upsert(userId, videoId, effectiveProgress, completionRate.doubleValue(), completed);
        if (completed && progressMapper.claimMastery(userId, videoId) == 1) {
            List<VideoKnowledgeRelationEntity> relations = relationMapper.selectList(
                    Wrappers.<VideoKnowledgeRelationEntity>lambdaQuery()
                            .eq(VideoKnowledgeRelationEntity::getVideoId, videoId));
            for (VideoKnowledgeRelationEntity relation : relations) {
                masteryMapper.applyVideoEvidence(userId, relation.getKnowledgeId());
            }
        }
        return progressMapper.select(userId, videoId);
    }

    @Transactional(readOnly = true)
    public Optional<UserVideoProgressEntity> find(long userId, long videoId) {
        if (userId <= 0 || videoId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(progressMapper.select(userId, videoId));
    }
}
