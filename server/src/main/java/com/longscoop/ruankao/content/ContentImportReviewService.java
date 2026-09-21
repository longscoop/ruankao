package com.longscoop.ruankao.content;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.ai.AiService;
import com.longscoop.ruankao.content.model.*;
import com.longscoop.ruankao.content.pdf.ParsedKnowledgeCandidate;
import com.longscoop.ruankao.content.pdf.ParsedQuestion;
import com.longscoop.ruankao.content.persistence.*;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.persistence.CourseChapterEntity;
import com.longscoop.ruankao.course.persistence.CourseChapterMapper;
import com.longscoop.ruankao.course.persistence.CourseEntity;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointEntity;
import com.longscoop.ruankao.knowledge.persistence.KnowledgePointMapper;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.QuestionService;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.question.model.QuestionStatus;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import com.longscoop.ruankao.question.persistence.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContentImportReviewService {

    private final ContentImportBatchMapper batchMapper;
    private final ContentImportItemMapper itemMapper;
    private final ContentImportIssueMapper issueMapper;
    private final LessonMapper lessonMapper;
    private final LessonKnowledgeMapper lessonKnowledgeMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionService questionService;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;
    private final AiService aiService;

    public ContentImportReviewService(
            ContentImportBatchMapper batchMapper,
            ContentImportItemMapper itemMapper,
            ContentImportIssueMapper issueMapper,
            LessonMapper lessonMapper,
            LessonKnowledgeMapper lessonKnowledgeMapper,
            CourseChapterMapper chapterMapper,
            CourseMapper courseMapper,
            KnowledgePointService knowledgePointService,
            KnowledgePointMapper knowledgePointMapper,
            QuestionService questionService,
            QuestionMapper questionMapper,
            ObjectMapper objectMapper,
            AiService aiService) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.issueMapper = issueMapper;
        this.lessonMapper = lessonMapper;
        this.lessonKnowledgeMapper = lessonKnowledgeMapper;
        this.chapterMapper = chapterMapper;
        this.courseMapper = courseMapper;
        this.knowledgePointService = knowledgePointService;
        this.knowledgePointMapper = knowledgePointMapper;
        this.questionService = questionService;
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
    }

    @Transactional(readOnly = true)
    public List<BatchSummary> list(Long examId) {
        var query = Wrappers.<ContentImportBatchEntity>lambdaQuery()
                .orderByDesc(ContentImportBatchEntity::getCreatedAt);
        if (examId != null) {
            query.eq(ContentImportBatchEntity::getExamId, examId);
        }
        return batchMapper.selectList(query).stream()
                .map(x -> new BatchSummary(
                        x.getId(), x.getExamId(), x.getFilename(), x.getTitle(),
                        x.getDetectedType(), x.getStatus(), x.getPageCount(),
                        x.getMaterializedCourseId(), x.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void approveItem(UUID batchId, long itemId) {
        ContentImportItemEntity item = requireItem(batchId, itemId);
        if (item.getStatus() == ImportItemStatus.REJECTED
                || item.getStatus() == ImportItemStatus.PUBLISHED) {
            throw new IllegalStateException("item cannot be approved in current status");
        }
        item.setStatus(ImportItemStatus.APPROVED);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
    }

    @Transactional
    public void rejectItem(UUID batchId, long itemId) {
        ContentImportItemEntity item = requireItem(batchId, itemId);
        if (item.getStatus() == ImportItemStatus.MATERIALIZED
                || item.getStatus() == ImportItemStatus.PUBLISHED) {
            throw new IllegalStateException("materialized item cannot be rejected");
        }
        item.setStatus(ImportItemStatus.REJECTED);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
    }

    @Transactional
    public void updateItem(UUID batchId, long itemId, String title, String contentJson) {
        ContentImportItemEntity item = requireItem(batchId, itemId);
        if (item.getStatus() == ImportItemStatus.MATERIALIZED
                || item.getStatus() == ImportItemStatus.PUBLISHED) {
            throw new IllegalStateException("materialized item cannot be edited");
        }
        if (contentJson == null || contentJson.isBlank()) {
            throw new IllegalArgumentException("contentJson is required");
        }
        try {
            objectMapper.readTree(contentJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("contentJson must be valid JSON", e);
        }
        if (title != null && !title.isBlank()) {
            item.setTitle(title.trim());
        }
        item.setContentJson(contentJson.trim());
        item.setStatus(ImportItemStatus.PENDING);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
    }

    @Transactional(readOnly = true)
    public AiService.AiResult suggestStructure(long userId, UUID batchId, long itemId) {
        ContentImportItemEntity item = requireItem(batchId, itemId);
        return aiService.suggestContentImportStructure(userId, item.getContentJson());
    }

    @Transactional
    public void resolveIssue(UUID batchId, long issueId) {
        ContentImportIssueEntity issue = issueMapper.selectById(issueId);
        if (issue == null || !issue.getBatchId().equals(batchId)) {
            throw new IllegalArgumentException("issue not found");
        }
        issue.setStatus(ImportIssueStatus.RESOLVED);
        issue.setResolvedAt(OffsetDateTime.now());
        issueMapper.updateById(issue);
    }

    @Transactional
    public long publishKnowledge(
            UUID batchId,
            long itemId,
            String code,
            Long parentId,
            int importance,
            double examFrequency,
            int estimatedMinutes,
            int sortOrder) {
        ContentImportBatchEntity batch = requireBatch(batchId);
        ContentImportItemEntity item = requireApproved(batchId, itemId, ImportItemType.KNOWLEDGE);
        requireNoOpenErrors(batchId, itemId);
        ParsedKnowledgeCandidate parsed = read(item.getContentJson(), ParsedKnowledgeCandidate.class);

        int level = 1;
        if (parentId != null) {
            KnowledgePointEntity parent = knowledgePointMapper.selectById(parentId);
            if (parent == null || !parent.getExamId().equals(batch.getExamId())) {
                throw new IllegalArgumentException("parent knowledge must belong to import exam");
            }
            level = parent.getLevel() + 1;
        }

        long knowledgeId = knowledgePointService.create(
                batch.getExamId(), parentId, level, code, parsed.name(), null,
                importance, examFrequency, estimatedMinutes, sortOrder, KnowledgeStatus.ACTIVE);
        item.setTargetId(knowledgeId);
        item.setStatus(ImportItemStatus.PUBLISHED);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
        refreshBatchPublished(batch);
        return knowledgeId;
    }

    @Transactional
    public long publishQuestion(
            UUID batchId,
            long itemId,
            QuestionDifficulty difficulty,
            QuestionSource source,
            List<QuestionKnowledgeLink> knowledgeLinks) {
        ContentImportBatchEntity batch = requireBatch(batchId);
        ContentImportItemEntity item = requireApproved(batchId, itemId, ImportItemType.QUESTION);
        requireNoOpenErrors(batchId, itemId);
        ParsedQuestion parsed = read(item.getContentJson(), ParsedQuestion.class);
        if (parsed.answer() == null || parsed.answer().isBlank()) {
            throw new IllegalStateException("source answer is missing");
        }
        if (parsed.options() == null || parsed.options().size() < 2) {
            throw new IllegalStateException("source options are incomplete");
        }
        if (difficulty == null || source == null) {
            throw new IllegalArgumentException("difficulty and source are required");
        }

        long questionId = questionService.create(
                batch.getExamId(),
                parsed.questionType(),
                source,
                QuestionStatus.PUBLISHED,
                difficulty,
                parsed.content(),
                parsed.answer(),
                knowledgeLinks);

        QuestionEntity question = questionMapper.selectById(questionId);
        question.setOptionsJson(write(parsed.options()));
        question.setExplanation(parsed.explanation());
        question.setImportBatchId(batchId);
        question.setSourcePage(parsed.sourcePageStart());
        question.setSourceQuestionNo(parsed.questionNo());
        question.setSourceLabel(parsed.sourceLabel());
        questionMapper.updateById(question);

        item.setTargetId(questionId);
        item.setStatus(ImportItemStatus.PUBLISHED);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
        refreshBatchPublished(batch);
        return questionId;
    }

    @Transactional
    public void publishLesson(UUID batchId, long itemId, List<Long> knowledgeIds) {
        ContentImportBatchEntity batch = requireBatch(batchId);
        ContentImportItemEntity item = requireItem(batchId, itemId);
        if (item.getItemType() != ImportItemType.LESSON
                || item.getStatus() != ImportItemStatus.MATERIALIZED
                || item.getTargetId() == null) {
            throw new IllegalStateException("lesson must be confirmed before publish");
        }
        requireNoOpenErrors(batchId, itemId);
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            throw new IllegalArgumentException("at least one knowledgeId is required");
        }

        for (Long knowledgeId : knowledgeIds.stream().distinct().toList()) {
            KnowledgePointEntity knowledge = knowledgePointMapper.selectById(knowledgeId);
            if (knowledge == null || !knowledge.getExamId().equals(batch.getExamId())) {
                throw new IllegalArgumentException("knowledge must belong to import exam");
            }
            Long duplicate = lessonKnowledgeMapper.selectCount(
                    Wrappers.<LessonKnowledgeEntity>lambdaQuery()
                            .eq(LessonKnowledgeEntity::getLessonId, item.getTargetId())
                            .eq(LessonKnowledgeEntity::getKnowledgeId, knowledgeId));
            if (duplicate == null || duplicate == 0) {
                LessonKnowledgeEntity relation = new LessonKnowledgeEntity();
                relation.setLessonId(item.getTargetId());
                relation.setKnowledgeId(knowledgeId);
                lessonKnowledgeMapper.insert(relation);
            }
        }

        LessonEntity lesson = lessonMapper.selectById(item.getTargetId());
        lesson.setStatus(LessonStatus.PUBLISHED);
        lesson.setUpdatedAt(OffsetDateTime.now());
        lessonMapper.updateById(lesson);

        CourseChapterEntity chapter = chapterMapper.selectById(lesson.getChapterId());
        CourseEntity course = chapter == null ? null : courseMapper.selectById(chapter.getCourseId());
        if (course != null && course.getStatus() != CourseStatus.PUBLISHED) {
            course.setStatus(CourseStatus.PUBLISHED);
            course.setUpdatedAt(OffsetDateTime.now());
            courseMapper.updateById(course);
        }

        item.setStatus(ImportItemStatus.PUBLISHED);
        item.setUpdatedAt(OffsetDateTime.now());
        itemMapper.updateById(item);
        refreshBatchPublished(batch);
    }

    private ContentImportBatchEntity requireBatch(UUID batchId) {
        ContentImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("import batch not found");
        return batch;
    }

    private ContentImportItemEntity requireItem(UUID batchId, long itemId) {
        ContentImportItemEntity item = itemMapper.selectById(itemId);
        if (item == null || !item.getBatchId().equals(batchId)) {
            throw new IllegalArgumentException("import item not found");
        }
        return item;
    }

    private ContentImportItemEntity requireApproved(UUID batchId, long itemId, ImportItemType type) {
        ContentImportItemEntity item = requireItem(batchId, itemId);
        if (item.getItemType() != type || item.getStatus() != ImportItemStatus.APPROVED) {
            throw new IllegalStateException(type + " item must be approved before publish");
        }
        return item;
    }

    private void requireNoOpenErrors(UUID batchId, long itemId) {
        Long count = issueMapper.selectCount(
                Wrappers.<ContentImportIssueEntity>lambdaQuery()
                        .eq(ContentImportIssueEntity::getBatchId, batchId)
                        .eq(ContentImportIssueEntity::getSeverity, ImportIssueSeverity.ERROR)
                        .eq(ContentImportIssueEntity::getStatus, ImportIssueStatus.OPEN)
                        .and(q -> q.isNull(ContentImportIssueEntity::getItemId)
                                .or()
                                .eq(ContentImportIssueEntity::getItemId, itemId)));
        if (count != null && count > 0) {
            throw new IllegalStateException("open ERROR issues must be resolved before publish");
        }
    }

    private void refreshBatchPublished(ContentImportBatchEntity batch) {
        Long remaining = itemMapper.selectCount(
                Wrappers.<ContentImportItemEntity>lambdaQuery()
                        .eq(ContentImportItemEntity::getBatchId, batch.getId())
                        .notIn(ContentImportItemEntity::getStatus,
                                ImportItemStatus.REJECTED,
                                ImportItemStatus.PUBLISHED));
        if (remaining != null && remaining == 0) {
            batch.setStatus(ImportBatchStatus.PUBLISHED);
            batch.setPublishedAt(OffsetDateTime.now());
            batch.setUpdatedAt(OffsetDateTime.now());
            batchMapper.updateById(batch);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("invalid import item payload", e);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize content", e);
        }
    }

    public record BatchSummary(
            UUID batchId,
            long examId,
            String filename,
            String title,
            ImportDocumentType detectedType,
            ImportBatchStatus status,
            int pageCount,
            Long courseId,
            OffsetDateTime createdAt) {
    }
}
