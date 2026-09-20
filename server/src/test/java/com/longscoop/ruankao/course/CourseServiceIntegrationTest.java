package com.longscoop.ruankao.course;

import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.persistence.CourseChapterEntity;
import com.longscoop.ruankao.course.persistence.CourseEntity;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CourseServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private CourseService courseService;

    @Test
    void createsTrimmedCourseAndListsCoursesInStableOrder() {
        long examId = examService.create("course-list-exam", "Course Exam", ExamStatus.ACTIVE);

        long second = courseService.createCourse(
                examId, "  Second  ", null, CourseStatus.DRAFT, 20);
        long first = courseService.createCourse(
                examId, "  First  ", " intro ", CourseStatus.PUBLISHED, 10);

        List<CourseEntity> courses = courseService.listCourses(examId);

        assertEquals(List.of(first, second), courses.stream().map(CourseEntity::getId).toList());
        assertEquals("First", courses.get(0).getTitle());
        assertEquals("intro", courses.get(0).getDescription());
    }

    @Test
    void createsAndListsChaptersBySortOrder() {
        long examId = examService.create("chapter-list-exam", "Chapter Exam", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(
                examId, "Course", null, CourseStatus.DRAFT, 0);

        long later = courseService.createChapter(courseId, "Later", null, 20);
        long first = courseService.createChapter(courseId, " First ", " desc ", 10);

        List<CourseChapterEntity> chapters = courseService.listChapters(courseId);

        assertEquals(List.of(first, later), chapters.stream().map(CourseChapterEntity::getId).toList());
        assertEquals("First", chapters.get(0).getTitle());
        assertEquals("desc", chapters.get(0).getDescription());
    }

    @Test
    void rejectsInvalidCourseAndChapterArguments() {
        long examId = examService.create("course-validation-exam", "Validation", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(
                examId, "Course", null, CourseStatus.DRAFT, 0);

        assertThrows(IllegalArgumentException.class, () ->
                courseService.createCourse(examId, " ", null, CourseStatus.DRAFT, 0));
        assertThrows(IllegalArgumentException.class, () ->
                courseService.createCourse(examId, "Title", null, null, 0));
        assertThrows(IllegalArgumentException.class, () ->
                courseService.createCourse(examId, "Title", null, CourseStatus.DRAFT, -1));
        assertThrows(IllegalArgumentException.class, () ->
                courseService.createCourse(999999L, "Title", null, CourseStatus.DRAFT, 0));

        assertThrows(IllegalArgumentException.class, () ->
                courseService.createChapter(courseId, " ", null, 0));
        assertThrows(IllegalArgumentException.class, () ->
                courseService.createChapter(courseId, "Chapter", null, -1));
        assertThrows(IllegalArgumentException.class, () ->
                courseService.createChapter(999999L, "Chapter", null, 0));
    }

    @Test
    void duplicateChapterSortOrderIsRejected() {
        long examId = examService.create("chapter-unique-exam", "Unique", ExamStatus.ACTIVE);
        long courseId = courseService.createCourse(
                examId, "Course", null, CourseStatus.DRAFT, 0);

        courseService.createChapter(courseId, "First", null, 1);

        assertThrows(DataIntegrityViolationException.class, () ->
                courseService.createChapter(courseId, "Duplicate", null, 1));
    }
}
