package com.longscoop.ruankao.course;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.content.model.LessonStatus;
import com.longscoop.ruankao.content.persistence.LessonBlockEntity;
import com.longscoop.ruankao.content.persistence.LessonBlockMapper;
import com.longscoop.ruankao.content.persistence.LessonEntity;
import com.longscoop.ruankao.content.persistence.LessonKnowledgeEntity;
import com.longscoop.ruankao.content.persistence.LessonKnowledgeMapper;
import com.longscoop.ruankao.content.persistence.LessonMapper;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.model.VideoStatus;
import com.longscoop.ruankao.course.persistence.CourseChapterEntity;
import com.longscoop.ruankao.course.persistence.CourseChapterMapper;
import com.longscoop.ruankao.course.persistence.CourseEntity;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.course.persistence.UserVideoProgressEntity;
import com.longscoop.ruankao.course.persistence.VideoEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationEntity;
import com.longscoop.ruankao.course.persistence.VideoKnowledgeRelationMapper;
import com.longscoop.ruankao.course.persistence.VideoMapper;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryEntity;
import com.longscoop.ruankao.learning.persistence.UserKnowledgeMasteryMapper;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.user.UserExamProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LearnerContentService {

    private static final Duration VIDEO_URL_TTL = Duration.ofHours(2);

    private final UserExamProfileService profileService;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final VideoMapper videoMapper;
    private final LessonMapper lessonMapper;
    private final LessonBlockMapper lessonBlockMapper;
    private final LessonKnowledgeMapper lessonKnowledgeMapper;
    private final VideoKnowledgeRelationMapper relationMapper;
    private final KnowledgePointMapper knowledgeMapper;
    private final UserKnowledgeMasteryMapper masteryMapper;
    private final VideoTranscriptService transcriptService;
    private final VideoProgressService videoProgressService;
    private final StorageProvider storageProvider;

    public LearnerContentService(
            UserExamProfileService profileService,
            CourseMapper courseMapper,
            CourseChapterMapper chapterMapper,
            VideoMapper videoMapper,
            LessonMapper lessonMapper,
            LessonBlockMapper lessonBlockMapper,
            LessonKnowledgeMapper lessonKnowledgeMapper,
            VideoKnowledgeRelationMapper relationMapper,
            KnowledgePointMapper knowledgeMapper,
            UserKnowledgeMasteryMapper masteryMapper,
            VideoTranscriptService transcriptService,
            VideoProgressService videoProgressService,
            StorageProvider storageProvider) {
        this.profileService = profileService;
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.videoMapper = videoMapper;
        this.lessonMapper = lessonMapper;
        this.lessonBlockMapper = lessonBlockMapper;
        this.lessonKnowledgeMapper = lessonKnowledgeMapper;
        this.relationMapper = relationMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.masteryMapper = masteryMapper;
        this.transcriptService = transcriptService;
        this.videoProgressService = videoProgressService;
        this.storageProvider = storageProvider;
    }

    @Transactional(readOnly = true)
    public List<CourseSummary> listCourses(long userId, Long requestedExamId) {
        long examId = resolveExamId(userId, requestedExamId);
        return courseMapper.selectList(
                        Wrappers.<CourseEntity>lambdaQuery()
                                .eq(CourseEntity::getExamId, examId)
                                .eq(CourseEntity::getStatus, CourseStatus.PUBLISHED)
                                .orderByAsc(CourseEntity::getSortOrder)
                                .orderByAsc(CourseEntity::getId))
                .stream()
                .map(this::toCourseSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CourseDetail> getCourse(long userId, long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (!visibleCourse(userId, course)) {
            return Optional.empty();
        }
        List<ChapterView> chapters = chapterMapper.selectList(
                        Wrappers.<CourseChapterEntity>lambdaQuery()
                                .eq(CourseChapterEntity::getCourseId, courseId)
                                .orderByAsc(CourseChapterEntity::getSortOrder)
                                .orderByAsc(CourseChapterEntity::getId))
                .stream()
                .map(chapter -> new ChapterView(
                        chapter.getId(),
                        chapter.getCourseId(),
                        chapter.getTitle(),
                        chapter.getDescription(),
                        publishedVideos(chapter.getId()),
                        publishedLessons(chapter.getId())))
                .toList();
        return Optional.of(new CourseDetail(
                course.getId(),
                course.getExamId(),
                course.getTitle(),
                course.getDescription(),
                chapters));
    }

    @Transactional(readOnly = true)
    public Optional<VideoDetail> getVideo(long userId, long videoId) {
        VideoEntity video = videoMapper.selectById(videoId);
        if (!visibleVideo(userId, video)) {
            return Optional.empty();
        }
        UserVideoProgressEntity progress = videoProgressService
                .find(userId, videoId)
                .orElse(null);
        List<TranscriptSegment> transcript = transcriptService.list(videoId).stream()
                .map(segment -> new TranscriptSegment(
                        segment.getStartSecond().doubleValue(),
                        segment.getEndSecond().doubleValue(),
                        segment.getContent()))
                .toList();
        List<Long> knowledgeIds = relationMapper.selectList(
                        Wrappers.<VideoKnowledgeRelationEntity>lambdaQuery()
                                .eq(VideoKnowledgeRelationEntity::getVideoId, videoId)
                                .orderByAsc(VideoKnowledgeRelationEntity::getId))
                .stream()
                .map(VideoKnowledgeRelationEntity::getKnowledgeId)
                .toList();

        return Optional.of(new VideoDetail(
                video.getId(),
                video.getChapterId(),
                video.getTitle(),
                video.getDescription(),
                video.getDurationSeconds(),
                Boolean.TRUE.equals(video.getFreeFlag()),
                storageProvider.generateAccessUrl(video.getObjectKey(), VIDEO_URL_TTL).toString(),
                progress == null ? 0 : progress.getProgressSeconds(),
                progress != null && Boolean.TRUE.equals(progress.getCompleted()),
                transcript,
                knowledgeIds));
    }

    @Transactional
    public boolean updateVideoProgress(long userId, long videoId, int progressSeconds) {
        VideoEntity video = videoMapper.selectById(videoId);
        if (!visibleVideo(userId, video)) {
            return false;
        }
        videoProgressService.update(userId, videoId, progressSeconds);
        return true;
    }


    @Transactional(readOnly = true)
    public Optional<LessonDetail> getLesson(long userId, long lessonId) {
        LessonEntity lesson = lessonMapper.selectById(lessonId);
        if (!visibleLesson(userId, lesson)) {
            return Optional.empty();
        }

        List<LessonBlockView> blocks = lessonBlockMapper.selectList(
                        Wrappers.<LessonBlockEntity>lambdaQuery()
                                .eq(LessonBlockEntity::getLessonId, lessonId)
                                .orderByAsc(LessonBlockEntity::getSortOrder)
                                .orderByAsc(LessonBlockEntity::getId))
                .stream()
                .map(block -> new LessonBlockView(
                        block.getId(),
                        block.getBlockType().name(),
                        block.getTextContent(),
                        block.getImageObjectKey() == null
                                ? null
                                : storageProvider.generateAccessUrl(
                                        block.getImageObjectKey(), VIDEO_URL_TTL).toString(),
                        block.getSourcePage(),
                        block.getSortOrder()))
                .toList();

        List<Long> knowledgeIds = lessonKnowledgeMapper.selectList(
                        Wrappers.<LessonKnowledgeEntity>lambdaQuery()
                                .eq(LessonKnowledgeEntity::getLessonId, lessonId)
                                .orderByAsc(LessonKnowledgeEntity::getKnowledgeId))
                .stream()
                .map(LessonKnowledgeEntity::getKnowledgeId)
                .toList();

        return Optional.of(new LessonDetail(
                lesson.getId(),
                lesson.getChapterId(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getSourcePageStart(),
                lesson.getSourcePageEnd(),
                blocks,
                knowledgeIds));
    }

    @Transactional(readOnly = true)
    public Optional<KnowledgeDetail> getKnowledge(long userId, long knowledgeId) {
        KnowledgePointEntity knowledge = knowledgeMapper.selectById(knowledgeId);
        if (!visibleKnowledge(userId, knowledge)) {
            return Optional.empty();
        }
        UserKnowledgeMasteryEntity mastery = masteryMapper.selectOne(
                Wrappers.<UserKnowledgeMasteryEntity>lambdaQuery()
                        .eq(UserKnowledgeMasteryEntity::getUserId, userId)
                        .eq(UserKnowledgeMasteryEntity::getKnowledgeId, knowledgeId));
        List<VideoSummary> videos = videoMapper.selectByKnowledgePointId(knowledgeId).stream()
                .filter(video -> video.getStatus() == VideoStatus.PUBLISHED)
                .map(this::toVideoSummary)
                .toList();
        return Optional.of(new KnowledgeDetail(
                knowledge.getId(),
                knowledge.getName(),
                knowledge.getDescription(),
                mastery == null || mastery.getEvidenceCount() == null || mastery.getEvidenceCount() == 0
                        ? null
                        : mastery.getMasteryScore(),
                mastery == null || mastery.getEvidenceCount() == null ? 0 : mastery.getEvidenceCount(),
                videos));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeNode> knowledgeTree(long userId) {
        long examId = resolveExamId(userId, null);
        List<KnowledgePointEntity> points = knowledgeMapper.selectList(
                Wrappers.<KnowledgePointEntity>lambdaQuery()
                        .eq(KnowledgePointEntity::getExamId, examId)
                        .eq(KnowledgePointEntity::getStatus, KnowledgeStatus.ACTIVE)
                        .orderByAsc(KnowledgePointEntity::getLevel)
                        .orderByAsc(KnowledgePointEntity::getSortOrder)
                        .orderByAsc(KnowledgePointEntity::getId));

        Map<Long, MutableKnowledgeNode> nodes = new LinkedHashMap<>();
        for (KnowledgePointEntity point : points) {
            nodes.put(point.getId(), new MutableKnowledgeNode(point));
        }
        List<MutableKnowledgeNode> roots = new ArrayList<>();
        for (MutableKnowledgeNode node : nodes.values()) {
            Long parentId = node.point.getParentId();
            MutableKnowledgeNode parent = parentId == null ? null : nodes.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        Comparator<MutableKnowledgeNode> comparator = Comparator
                .comparingInt((MutableKnowledgeNode n) -> n.point.getSortOrder())
                .thenComparingLong(n -> n.point.getId());
        return roots.stream().sorted(comparator).map(node -> node.toView(comparator)).toList();
    }

    private long resolveExamId(long userId, Long requestedExamId) {
        var profile = profileService.find(userId)
                .orElseThrow(() -> new IllegalStateException("exam profile is required"));
        if (requestedExamId != null && requestedExamId.longValue() != profile.getExamId()) {
            throw new IllegalArgumentException("requested exam does not match user profile");
        }
        return profile.getExamId();
    }

    private boolean visibleCourse(long userId, CourseEntity course) {
        if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
            return false;
        }
        return profileService.find(userId)
                .map(profile -> profile.getExamId().equals(course.getExamId()))
                .orElse(false);
    }

    private boolean visibleVideo(long userId, VideoEntity video) {
        if (video == null || video.getStatus() != VideoStatus.PUBLISHED) {
            return false;
        }
        CourseChapterEntity chapter = chapterMapper.selectById(video.getChapterId());
        CourseEntity course = chapter == null ? null : courseMapper.selectById(chapter.getCourseId());
        return visibleCourse(userId, course);
    }

    private boolean visibleKnowledge(long userId, KnowledgePointEntity knowledge) {
        if (knowledge == null || knowledge.getStatus() != KnowledgeStatus.ACTIVE) {
            return false;
        }
        return profileService.find(userId)
                .map(profile -> profile.getExamId().equals(knowledge.getExamId()))
                .orElse(false);
    }


    private boolean visibleLesson(long userId, LessonEntity lesson) {
        if (lesson == null || lesson.getStatus() != LessonStatus.PUBLISHED) {
            return false;
        }
        CourseChapterEntity chapter = chapterMapper.selectById(lesson.getChapterId());
        CourseEntity course = chapter == null ? null : courseMapper.selectById(chapter.getCourseId());
        return visibleCourse(userId, course);
    }

    private List<LessonSummary> publishedLessons(long chapterId) {
        return lessonMapper.selectList(
                        Wrappers.<LessonEntity>lambdaQuery()
                                .eq(LessonEntity::getChapterId, chapterId)
                                .eq(LessonEntity::getStatus, LessonStatus.PUBLISHED)
                                .orderByAsc(LessonEntity::getSortOrder)
                                .orderByAsc(LessonEntity::getId))
                .stream()
                .map(lesson -> new LessonSummary(
                        lesson.getId(),
                        lesson.getChapterId(),
                        lesson.getTitle(),
                        lesson.getSummary(),
                        lesson.getSourcePageStart(),
                        lesson.getSourcePageEnd()))
                .toList();
    }

    private List<VideoSummary> publishedVideos(long chapterId) {
        return videoMapper.selectList(
                        Wrappers.<VideoEntity>lambdaQuery()
                                .eq(VideoEntity::getChapterId, chapterId)
                                .eq(VideoEntity::getStatus, VideoStatus.PUBLISHED)
                                .orderByAsc(VideoEntity::getSortOrder)
                                .orderByAsc(VideoEntity::getId))
                .stream()
                .map(this::toVideoSummary)
                .toList();
    }

    private CourseSummary toCourseSummary(CourseEntity course) {
        return new CourseSummary(
                course.getId(),
                course.getExamId(),
                course.getTitle(),
                course.getDescription());
    }

    private VideoSummary toVideoSummary(VideoEntity video) {
        return new VideoSummary(
                video.getId(),
                video.getChapterId(),
                video.getTitle(),
                video.getDescription(),
                video.getDurationSeconds(),
                Boolean.TRUE.equals(video.getFreeFlag()));
    }

    public record CourseSummary(long id, long examId, String title, String description) {
    }

    public record CourseDetail(
            long id,
            long examId,
            String title,
            String description,
            List<ChapterView> chapters) {
    }

    public record ChapterView(
            long id,
            long courseId,
            String title,
            String description,
            List<VideoSummary> videos,
            List<LessonSummary> lessons) {
    }

    public record LessonSummary(
            long id,
            long chapterId,
            String title,
            String summary,
            Integer sourcePageStart,
            Integer sourcePageEnd) {
    }

    public record LessonBlockView(
            long id,
            String blockType,
            String textContent,
            String imageUrl,
            Integer sourcePage,
            int sortOrder) {
    }

    public record LessonDetail(
            long id,
            long chapterId,
            String title,
            String summary,
            Integer sourcePageStart,
            Integer sourcePageEnd,
            List<LessonBlockView> blocks,
            List<Long> knowledgeIds) {
    }

    public record VideoSummary(
            long id,
            long chapterId,
            String title,
            String description,
            int durationSeconds,
            boolean freeFlag) {
    }

    public record TranscriptSegment(double startSeconds, double endSeconds, String text) {
    }

    public record VideoDetail(
            long id,
            long chapterId,
            String title,
            String description,
            int durationSeconds,
            boolean freeFlag,
            String playUrl,
            int progressSeconds,
            boolean completed,
            List<TranscriptSegment> transcript,
            List<Long> knowledgeIds) {
    }

    public record KnowledgeDetail(
            long id,
            String name,
            String description,
            Double masteryScore,
            int evidenceCount,
            List<VideoSummary> videos) {
    }

    public record KnowledgeNode(
            long id,
            Long parentId,
            int level,
            String code,
            String name,
            String description,
            int importance,
            double examFrequency,
            int estimatedMinutes,
            List<KnowledgeNode> children) {
    }

    private static class MutableKnowledgeNode {
        private final KnowledgePointEntity point;
        private final List<MutableKnowledgeNode> children = new ArrayList<>();

        private MutableKnowledgeNode(KnowledgePointEntity point) {
            this.point = point;
        }

        private KnowledgeNode toView(Comparator<MutableKnowledgeNode> comparator) {
            return new KnowledgeNode(
                    point.getId(),
                    point.getParentId(),
                    point.getLevel(),
                    point.getCode(),
                    point.getName(),
                    point.getDescription(),
                    point.getImportance(),
                    point.getExamFrequency(),
                    point.getEstimatedMinutes(),
                    children.stream().sorted(comparator).map(child -> child.toView(comparator)).toList());
        }
    }
}
