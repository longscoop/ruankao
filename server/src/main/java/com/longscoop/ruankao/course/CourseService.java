package com.longscoop.ruankao.course;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.persistence.CourseChapterEntity;
import com.longscoop.ruankao.course.persistence.CourseChapterMapper;
import com.longscoop.ruankao.course.persistence.CourseEntity;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.exam.persistence.ExamMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final ExamMapper examMapper;

    public CourseService(
            CourseMapper courseMapper,
            CourseChapterMapper chapterMapper,
            ExamMapper examMapper) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.examMapper = examMapper;
    }

    @Transactional
    public long createCourse(
            long examId,
            String title,
            String description,
            CourseStatus status,
            int sortOrder) {
        if (examId <= 0 || examMapper.selectById(examId) == null) {
            throw new IllegalArgumentException("exam must exist");
        }
        String normalizedTitle = requireText(title, "title");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder cannot be negative");
        }

        CourseEntity entity = new CourseEntity();
        entity.setExamId(examId);
        entity.setTitle(normalizedTitle);
        entity.setDescription(normalizeOptional(description));
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        courseMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public long createChapter(
            long courseId,
            String title,
            String description,
            int sortOrder) {
        if (courseId <= 0 || courseMapper.selectById(courseId) == null) {
            throw new IllegalArgumentException("course must exist");
        }
        String normalizedTitle = requireText(title, "title");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder cannot be negative");
        }

        Long duplicate = chapterMapper.selectCount(
                Wrappers.<CourseChapterEntity>lambdaQuery()
                        .eq(CourseChapterEntity::getCourseId, courseId)
                        .eq(CourseChapterEntity::getSortOrder, sortOrder));
        if (duplicate != null && duplicate > 0) {
            throw new DataIntegrityViolationException(
                    "chapter sort order already exists in course");
        }

        CourseChapterEntity entity = new CourseChapterEntity();
        entity.setCourseId(courseId);
        entity.setTitle(normalizedTitle);
        entity.setDescription(normalizeOptional(description));
        entity.setSortOrder(sortOrder);
        chapterMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public List<CourseEntity> listCourses(long examId) {
        return courseMapper.selectList(
                Wrappers.<CourseEntity>lambdaQuery()
                        .eq(CourseEntity::getExamId, examId)
                        .orderByAsc(CourseEntity::getSortOrder)
                        .orderByAsc(CourseEntity::getId));
    }

    @Transactional(readOnly = true)
    public List<CourseChapterEntity> listChapters(long courseId) {
        return chapterMapper.selectList(
                Wrappers.<CourseChapterEntity>lambdaQuery()
                        .eq(CourseChapterEntity::getCourseId, courseId)
                        .orderByAsc(CourseChapterEntity::getSortOrder)
                        .orderByAsc(CourseChapterEntity::getId));
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
