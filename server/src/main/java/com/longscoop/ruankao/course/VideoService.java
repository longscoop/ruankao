package com.longscoop.ruankao.course;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.course.persistence.CourseChapterEntity;
import com.longscoop.ruankao.course.persistence.CourseChapterMapper;
import com.longscoop.ruankao.course.persistence.CourseEntity;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.course.persistence.VideoEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationMapper;
import com.longscoop.ruankao.course.persistence.VideoMapper;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideoService {

    private final VideoMapper videoMapper;
    private final VideoKnowledgeRelationMapper videoKnowledgeRelationMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public VideoService(
            VideoMapper videoMapper,
            VideoKnowledgeRelationMapper videoKnowledgeRelationMapper,
            CourseChapterMapper chapterMapper,
            CourseMapper courseMapper,
            KnowledgePointMapper knowledgePointMapper) {
        this.videoMapper = videoMapper;
        this.videoKnowledgeRelationMapper = videoKnowledgeRelationMapper;
        this.chapterMapper = chapterMapper;
        this.courseMapper = courseMapper;
        this.knowledgePointMapper = knowledgePointMapper;
    }

    @Transactional
    public long createVideo(
            long chapterId,
            String title,
            String description,
            String objectKey,
            int durationSeconds,
            boolean freeFlag,
            VideoStatus status,
            int sortOrder) {
        requireChapter(chapterId);
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status == VideoStatus.PUBLISHED) {
            throw new IllegalStateException("published video must have a knowledge link");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder cannot be negative");
        }

        VideoEntity entity = new VideoEntity();
        entity.setChapterId(chapterId);
        entity.setTitle(requireText(title, "title"));
        entity.setDescription(normalizeOptional(description));
        entity.setObjectKey(requireText(objectKey, "objectKey"));
        entity.setDurationSeconds(durationSeconds);
        entity.setFreeFlag(freeFlag);
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        videoMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void linkKnowledge(long videoId, long knowledgeId) {
        VideoEntity video = requireVideo(videoId);
        KnowledgePointEntity knowledgePoint = knowledgePointMapper.selectById(knowledgeId);
        if (knowledgePoint == null) {
            throw new IllegalArgumentException("knowledge point must exist");
        }
        CourseEntity course = requireCourseForChapter(video.getChapterId());
        if (!course.getExamId().equals(knowledgePoint.getExamId())) {
            throw new IllegalArgumentException("knowledge point must belong to the course exam");
        }

        Long duplicateCount = videoKnowledgeRelationMapper.selectCount(
                Wrappers.<VideoKnowledgeRelationEntity>lambdaQuery()
                        .eq(VideoKnowledgeRelationEntity::getVideoId, videoId)
                        .eq(VideoKnowledgeRelationEntity::getKnowledgeId, knowledgeId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new DataIntegrityViolationException("video knowledge link already exists");
        }

        VideoKnowledgeRelationEntity relation = new VideoKnowledgeRelationEntity();
        relation.setVideoId(videoId);
        relation.setKnowledgeId(knowledgeId);
        videoKnowledgeRelationMapper.insert(relation);
    }

    @Transactional
    public void updateStatus(long videoId, VideoStatus status) {
        VideoEntity video = requireVideo(videoId);
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status == VideoStatus.PUBLISHED && !hasKnowledgeLink(videoId)) {
            throw new IllegalStateException("published video must have a knowledge link");
        }
        video.setStatus(status);
        videoMapper.updateById(video);
    }

    @Transactional(readOnly = true)
    public VideoEntity find(long videoId) {
        return requireVideo(videoId);
    }

    @Transactional(readOnly = true)
    public List<VideoEntity> listForChapter(long chapterId) {
        return videoMapper.selectList(Wrappers.<VideoEntity>lambdaQuery()
                .eq(VideoEntity::getChapterId, chapterId)
                .orderByAsc(VideoEntity::getSortOrder)
                .orderByAsc(VideoEntity::getId));
    }

    @Transactional(readOnly = true)
    public List<VideoEntity> listForKnowledgePoint(long knowledgeId) {
        return videoMapper.selectByKnowledgePointId(knowledgeId);
    }

    private boolean hasKnowledgeLink(long videoId) {
        Long count = videoKnowledgeRelationMapper.selectCount(
                Wrappers.<VideoKnowledgeRelationEntity>lambdaQuery()
                        .eq(VideoKnowledgeRelationEntity::getVideoId, videoId));
        return count != null && count > 0;
    }

    private VideoEntity requireVideo(long videoId) {
        if (videoId <= 0) {
            throw new IllegalArgumentException("video must exist");
        }
        VideoEntity video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new IllegalArgumentException("video must exist");
        }
        return video;
    }

    private CourseChapterEntity requireChapter(long chapterId) {
        if (chapterId <= 0) {
            throw new IllegalArgumentException("chapter must exist");
        }
        CourseChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("chapter must exist");
        }
        return chapter;
    }

    private CourseEntity requireCourseForChapter(long chapterId) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        CourseEntity course = courseMapper.selectById(chapter.getCourseId());
        if (course == null) {
            throw new IllegalStateException("course must exist for chapter");
        }
        return course;
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
