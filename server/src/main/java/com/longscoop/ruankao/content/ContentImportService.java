package com.longscoop.ruankao.content;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.content.model.*;
import com.longscoop.ruankao.content.pdf.*;
import com.longscoop.ruankao.content.persistence.*;
import com.longscoop.ruankao.course.CourseService;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.exam.persistence.ExamMapper;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.storage.StorageUploadRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.HexFormat;

@Service
public class ContentImportService {

    private static final int SOURCE_LESSON_PAGE_COUNT = 12;
    private static final String SOURCE_LESSON_KEY_PREFIX = "source-pages-";

    private final ExamMapper examMapper;
    private final StorageProvider storageProvider;
    private final PdfDocumentExtractor extractor;
    private final PdfContentParser parser;
    private final ObjectMapper objectMapper;
    private final ContentImportBatchMapper batchMapper;
    private final ContentImportPageMapper pageMapper;
    private final ContentImportItemMapper itemMapper;
    private final ContentImportIssueMapper issueMapper;
    private final CourseService courseService;
    private final LessonMapper lessonMapper;
    private final LessonBlockMapper lessonBlockMapper;

    public ContentImportService(
            ExamMapper examMapper,
            StorageProvider storageProvider,
            PdfDocumentExtractor extractor,
            PdfContentParser parser,
            ObjectMapper objectMapper,
            ContentImportBatchMapper batchMapper,
            ContentImportPageMapper pageMapper,
            ContentImportItemMapper itemMapper,
            ContentImportIssueMapper issueMapper,
            CourseService courseService,
            LessonMapper lessonMapper,
            LessonBlockMapper lessonBlockMapper) {
        this.examMapper = examMapper;
        this.storageProvider = storageProvider;
        this.extractor = extractor;
        this.parser = parser;
        this.objectMapper = objectMapper;
        this.batchMapper = batchMapper;
        this.pageMapper = pageMapper;
        this.itemMapper = itemMapper;
        this.issueMapper = issueMapper;
        this.courseService = courseService;
        this.lessonMapper = lessonMapper;
        this.lessonBlockMapper = lessonBlockMapper;
    }

    @Transactional
    public DryRunResult dryRun(long examId, String filename, byte[] pdfBytes, long createdBy) {
        if (examId <= 0 || examMapper.selectById(examId) == null) {
            throw new IllegalArgumentException("exam must exist");
        }
        if (filename == null || filename.isBlank() || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("a PDF filename is required");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF content is required");
        }
        if (createdBy <= 0) {
            throw new IllegalArgumentException("createdBy must be positive");
        }

        UUID batchId = UUID.randomUUID();
        String prefix = "content-imports/" + batchId;
        String originalKey = prefix + "/source.pdf";
        storageProvider.upload(new StorageUploadRequest(
                originalKey,
                "application/pdf",
                pdfBytes.length,
                new ByteArrayInputStream(pdfBytes)));

        List<ExtractedPdfPage> extracted = extractor.extract(pdfBytes);
        List<ParsedPage> parsedPages = new ArrayList<>();
        for (ExtractedPdfPage page : extracted) {
            String imageKey = prefix + "/pages/" + page.pageNumber() + ".png";
            storageProvider.upload(new StorageUploadRequest(
                    imageKey,
                    "image/png",
                    page.pngBytes().length,
                    new ByteArrayInputStream(page.pngBytes())));
            parsedPages.add(new ParsedPage(page.pageNumber(), page.text(), imageKey));
        }

        ParsedDocument parsed = parser.parse(parsedPages);

        ContentImportBatchEntity batch = new ContentImportBatchEntity();
        batch.setId(batchId);
        batch.setExamId(examId);
        batch.setFilename(filename.trim());
        batch.setOriginalObjectKey(originalKey);
        batch.setSha256(sha256(pdfBytes));
        batch.setDetectedType(parsed.documentType());
        batch.setStatus(ImportBatchStatus.REVIEWING);
        batch.setTitle(effectiveTitle(parsed.title(), filename));
        batch.setPageCount(extracted.size());
        batch.setCreatedBy(createdBy);
        batchMapper.insert(batch);

        for (ParsedPage page : parsedPages) {
            ContentImportPageEntity entity = new ContentImportPageEntity();
            entity.setBatchId(batchId);
            entity.setPageNumber(page.pageNumber());
            entity.setTextContent(page.text());
            entity.setImageObjectKey(page.imageObjectKey());
            pageMapper.insert(entity);
        }

        Map<String, Long> itemIds = new HashMap<>();
        int order = 0;
        for (ParsedLesson lesson : parsed.lessons()) {
            long id = insertItem(batchId, ImportItemType.LESSON, lesson.key(),
                    lesson.sourcePageStart(), lesson.sourcePageEnd(), lesson.title(), lesson, order++);
            itemIds.put(lesson.key(), id);
        }
        for (ParsedKnowledgeCandidate knowledge : parsed.knowledgeCandidates()) {
            long id = insertItem(batchId, ImportItemType.KNOWLEDGE, knowledge.key(),
                    knowledge.sourcePage(), knowledge.sourcePage(), knowledge.name(), knowledge, order++);
            itemIds.put(knowledge.key(), id);
        }
        for (ParsedQuestion question : parsed.questions()) {
            String title = question.questionNo() == null
                    ? truncate(question.content(), 120)
                    : "Question " + question.questionNo();
            long id = insertItem(batchId, ImportItemType.QUESTION, question.key(),
                    question.sourcePageStart(), question.sourcePageEnd(), title, question, order++);
            itemIds.put(question.key(), id);
        }

        for (ParsedIssue issue : parsed.issues()) {
            ContentImportIssueEntity entity = new ContentImportIssueEntity();
            entity.setBatchId(batchId);
            entity.setItemId(issue.itemKey() == null ? null : itemIds.get(issue.itemKey()));
            entity.setSeverity(issue.severity());
            entity.setCode(issue.code());
            entity.setMessage(issue.message());
            entity.setSourcePage(issue.sourcePage());
            entity.setStatus(ImportIssueStatus.OPEN);
            issueMapper.insert(entity);
        }

        return new DryRunResult(
                batchId,
                parsed.documentType(),
                ImportBatchStatus.REVIEWING,
                extracted.size(),
                order,
                parsed.issues().size());
    }

    @Transactional(readOnly = true)
    public Optional<ImportDetail> get(UUID batchId) {
        ContentImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) return Optional.empty();

        List<PageView> pages = pageMapper.selectList(
                        Wrappers.<ContentImportPageEntity>lambdaQuery()
                                .eq(ContentImportPageEntity::getBatchId, batchId)
                                .orderByAsc(ContentImportPageEntity::getPageNumber))
                .stream().map(x -> new PageView(
                        x.getPageNumber(), x.getTextContent(), x.getImageObjectKey())).toList();

        List<ItemView> items = itemMapper.selectList(
                        Wrappers.<ContentImportItemEntity>lambdaQuery()
                                .eq(ContentImportItemEntity::getBatchId, batchId)
                                .orderByAsc(ContentImportItemEntity::getSortOrder)
                                .orderByAsc(ContentImportItemEntity::getId))
                .stream().map(x -> new ItemView(
                        x.getId(), x.getItemType(), x.getItemKey(),
                        x.getSourcePageStart(), x.getSourcePageEnd(),
                        x.getTitle(), x.getContentJson(), x.getStatus(), x.getTargetId())).toList();

        List<IssueView> issues = issueMapper.selectList(
                        Wrappers.<ContentImportIssueEntity>lambdaQuery()
                                .eq(ContentImportIssueEntity::getBatchId, batchId)
                                .orderByDesc(ContentImportIssueEntity::getSeverity)
                                .orderByAsc(ContentImportIssueEntity::getId))
                .stream().map(x -> new IssueView(
                        x.getId(), x.getItemId(), x.getSeverity(), x.getCode(),
                        x.getMessage(), x.getSourcePage(), x.getStatus())).toList();

        return Optional.of(new ImportDetail(
                batch.getId(), batch.getExamId(), batch.getFilename(),
                batch.getDetectedType(), batch.getStatus(), batch.getTitle(),
                batch.getPageCount(), batch.getMaterializedCourseId(),
                pages, items, issues));
    }

    @Transactional
    public List<Long> createSourceLessons(UUID batchId) {
        ContentImportBatchEntity batch = requireBatch(batchId);
        List<ContentImportItemEntity> existing = itemMapper.selectList(
                Wrappers.<ContentImportItemEntity>lambdaQuery()
                        .eq(ContentImportItemEntity::getBatchId, batchId)
                        .likeRight(ContentImportItemEntity::getItemKey, SOURCE_LESSON_KEY_PREFIX)
                        .orderByAsc(ContentImportItemEntity::getSourcePageStart));
        int expectedCount = (batch.getPageCount() + SOURCE_LESSON_PAGE_COUNT - 1)
                / SOURCE_LESSON_PAGE_COUNT;
        if (!existing.isEmpty()) {
            if (existing.size() != expectedCount) {
                throw new IllegalStateException("source lessons are incomplete");
            }
            return existing.stream().map(ContentImportItemEntity::getId).toList();
        }
        if (batch.getStatus() != ImportBatchStatus.REVIEWING) {
            throw new IllegalStateException("source lessons must be created before confirm");
        }

        List<ContentImportPageEntity> pages = pageMapper.selectList(
                Wrappers.<ContentImportPageEntity>lambdaQuery()
                        .eq(ContentImportPageEntity::getBatchId, batchId)
                        .orderByAsc(ContentImportPageEntity::getPageNumber));
        if (pages.size() != batch.getPageCount()) {
            throw new IllegalStateException("source page count does not match PDF");
        }
        for (int index = 0; index < pages.size(); index++) {
            ContentImportPageEntity page = pages.get(index);
            if (page.getPageNumber() != index + 1
                    || page.getImageObjectKey() == null
                    || page.getImageObjectKey().isBlank()) {
                throw new IllegalStateException("source page evidence is incomplete");
            }
        }

        List<Long> itemIds = new ArrayList<>();
        for (int offset = 0; offset < pages.size(); offset += SOURCE_LESSON_PAGE_COUNT) {
            List<ContentImportPageEntity> group = pages.subList(
                    offset, Math.min(offset + SOURCE_LESSON_PAGE_COUNT, pages.size()));
            int first = group.get(0).getPageNumber();
            int last = group.get(group.size() - 1).getPageNumber();
            List<ParsedBlock> blocks = new ArrayList<>();
            for (ContentImportPageEntity page : group) {
                if (page.getTextContent() != null && !page.getTextContent().isBlank()) {
                    blocks.add(new ParsedBlock(LessonBlockType.TEXT,
                            page.getTextContent(), null, page.getPageNumber()));
                }
                blocks.add(new ParsedBlock(LessonBlockType.IMAGE,
                        null, page.getImageObjectKey(), page.getPageNumber()));
            }
            String key = SOURCE_LESSON_KEY_PREFIX + first + "-" + last;
            String title = first == last ? "原始资料 · 第 " + first + " 页"
                    : "原始资料 · 第 " + first + "–" + last + " 页";
            itemIds.add(insertItem(batchId, ImportItemType.LESSON, key,
                    first, last, title,
                    new ParsedLesson(key, title, first, last, List.copyOf(blocks)),
                    offset / SOURCE_LESSON_PAGE_COUNT));
        }
        return List.copyOf(itemIds);
    }

    @Transactional
    public void updateBatchTitle(UUID batchId, String title) {
        ContentImportBatchEntity batch = requireBatch(batchId);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        batch.setTitle(title.trim());
        batch.setUpdatedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);
    }

    @Transactional
    public void approveAll(UUID batchId) {
        requireBatch(batchId);
        List<ContentImportItemEntity> items = itemMapper.selectList(
                Wrappers.<ContentImportItemEntity>lambdaQuery()
                        .eq(ContentImportItemEntity::getBatchId, batchId)
                        .eq(ContentImportItemEntity::getStatus, ImportItemStatus.PENDING));
        for (ContentImportItemEntity item : items) {
            Long blockingErrors = issueMapper.selectCount(
                    Wrappers.<ContentImportIssueEntity>lambdaQuery()
                            .eq(ContentImportIssueEntity::getBatchId, batchId)
                            .eq(ContentImportIssueEntity::getSeverity, ImportIssueSeverity.ERROR)
                            .eq(ContentImportIssueEntity::getStatus, ImportIssueStatus.OPEN)
                            .and(q -> q.isNull(ContentImportIssueEntity::getItemId)
                                    .or()
                                    .eq(ContentImportIssueEntity::getItemId, item.getId())));
            if (blockingErrors != null && blockingErrors > 0) {
                continue;
            }
            item.setStatus(ImportItemStatus.APPROVED);
            itemMapper.updateById(item);
        }
    }

    @Transactional
    public ConfirmResult confirm(UUID batchId, String confirmKey) {
        if (confirmKey == null || confirmKey.isBlank()) {
            throw new IllegalArgumentException("confirmKey is required");
        }
        ContentImportBatchEntity batch = requireBatch(batchId);
        if (batch.getConfirmKey() != null) {
            if (!batch.getConfirmKey().equals(confirmKey.trim())) {
                throw new IllegalStateException("batch was already confirmed with a different key");
            }
            return new ConfirmResult(batchId, batch.getMaterializedCourseId());
        }

        Long duplicateKey = batchMapper.selectCount(
                Wrappers.<ContentImportBatchEntity>lambdaQuery()
                        .eq(ContentImportBatchEntity::getConfirmKey, confirmKey.trim())
                        .ne(ContentImportBatchEntity::getId, batchId));
        if (duplicateKey != null && duplicateKey > 0) {
            throw new IllegalArgumentException("confirmKey was already used");
        }

        List<ContentImportItemEntity> approvedLessons = itemMapper.selectList(
                Wrappers.<ContentImportItemEntity>lambdaQuery()
                        .eq(ContentImportItemEntity::getBatchId, batchId)
                        .eq(ContentImportItemEntity::getItemType, ImportItemType.LESSON)
                        .eq(ContentImportItemEntity::getStatus, ImportItemStatus.APPROVED)
                        .orderByAsc(ContentImportItemEntity::getSortOrder));

        Long courseId = null;
        if (!approvedLessons.isEmpty()) {
            courseId = courseService.createCourse(
                    batch.getExamId(),
                    batch.getTitle() == null || batch.getTitle().isBlank()
                            ? batch.getFilename()
                            : batch.getTitle(),
                    "Imported from " + batch.getFilename(),
                    CourseStatus.DRAFT,
                    0);
            long chapterId = courseService.createChapter(
                    courseId,
                    "PDF 导入讲义",
                    null,
                    0);

            int lessonOrder = 0;
            for (ContentImportItemEntity item : approvedLessons) {
                ParsedLesson parsed = read(item.getContentJson(), ParsedLesson.class);
                LessonEntity lesson = new LessonEntity();
                lesson.setChapterId(chapterId);
                lesson.setTitle(parsed.title());
                lesson.setStatus(LessonStatus.REVIEW);
                lesson.setSourceImportBatchId(batchId);
                lesson.setSourcePageStart(parsed.sourcePageStart());
                lesson.setSourcePageEnd(parsed.sourcePageEnd());
                lesson.setSortOrder(lessonOrder++);
                lessonMapper.insert(lesson);

                int blockOrder = 0;
                for (ParsedBlock block : parsed.blocks()) {
                    LessonBlockEntity entity = new LessonBlockEntity();
                    entity.setLessonId(lesson.getId());
                    entity.setBlockType(block.blockType());
                    entity.setTextContent(block.textContent());
                    entity.setImageObjectKey(block.imageObjectKey());
                    entity.setSourcePage(block.sourcePage());
                    entity.setSortOrder(blockOrder++);
                    lessonBlockMapper.insert(entity);
                }
                item.setStatus(ImportItemStatus.MATERIALIZED);
                item.setTargetId(lesson.getId());
                itemMapper.updateById(item);
            }
        }

        batch.setConfirmKey(confirmKey.trim());
        batch.setMaterializedCourseId(courseId);
        batch.setStatus(ImportBatchStatus.CONFIRMED);
        batch.setConfirmedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);
        return new ConfirmResult(batchId, courseId);
    }

    private long insertItem(
            UUID batchId,
            ImportItemType type,
            String key,
            int startPage,
            int endPage,
            String title,
            Object content,
            int order) {
        ContentImportItemEntity entity = new ContentImportItemEntity();
        entity.setBatchId(batchId);
        entity.setItemType(type);
        entity.setItemKey(key);
        entity.setSourcePageStart(startPage);
        entity.setSourcePageEnd(endPage);
        entity.setTitle(title);
        entity.setContentJson(write(content));
        entity.setStatus(ImportItemStatus.PENDING);
        entity.setSortOrder(order);
        itemMapper.insert(entity);
        return entity.getId();
    }

    private ContentImportBatchEntity requireBatch(UUID batchId) {
        if (batchId == null) throw new IllegalArgumentException("batchId is required");
        ContentImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("import batch not found");
        return batch;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize import item", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to deserialize import item", e);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String effectiveTitle(String parsedTitle, String filename) {
        String value = parsedTitle == null ? "" : parsedTitle.trim();
        boolean unusable = value.isBlank()
                || value.equalsIgnoreCase("Untitled PDF")
                || value.equals("系统架构设计师")
                || value.equals("目录")
                || value.matches("^\\d+\\s+.+");
        if (!unusable) {
            return value;
        }
        String fallback = filename == null ? "PDF 导入内容" : filename.trim();
        return fallback.toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? fallback.substring(0, fallback.length() - 4)
                : fallback;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record DryRunResult(
            UUID batchId,
            ImportDocumentType detectedType,
            ImportBatchStatus status,
            int pageCount,
            int itemCount,
            int issueCount) {}

    public record PageView(int pageNumber, String textContent, String imageObjectKey) {}

    public record ItemView(
            long id, ImportItemType itemType, String itemKey,
            int sourcePageStart, int sourcePageEnd, String title,
            String contentJson, ImportItemStatus status, Long targetId) {}

    public record IssueView(
            long id, Long itemId, ImportIssueSeverity severity, String code,
            String message, Integer sourcePage, ImportIssueStatus status) {}

    public record ImportDetail(
            UUID batchId, long examId, String filename, ImportDocumentType detectedType,
            ImportBatchStatus status, String title, int pageCount, Long courseId,
            List<PageView> pages, List<ItemView> items, List<IssueView> issues) {}

    public record ConfirmResult(UUID batchId, Long courseId) {}
}
