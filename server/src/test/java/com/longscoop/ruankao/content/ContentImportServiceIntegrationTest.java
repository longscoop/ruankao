package com.longscoop.ruankao.content;

import com.longscoop.ruankao.content.model.ImportBatchStatus;
import com.longscoop.ruankao.content.model.ImportDocumentType;
import com.longscoop.ruankao.content.persistence.LessonMapper;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.storage.StoredObject;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ContentImportServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired ContentImportService contentImportService;
    @Autowired ExamService examService;
    @Autowired CourseMapper courseMapper;
    @Autowired LessonMapper lessonMapper;

    @MockBean StorageProvider storageProvider;

    @Test
    void dryRunPersistsSourcePagesThenConfirmMaterializesApprovedLectureIdempotently() throws Exception {
        long examId = examService.create("pdf-import-exam", "System Architect", ExamStatus.ACTIVE);
        byte[] pdf = lecturePdf();

        when(storageProvider.upload(any())).thenAnswer(invocation -> {
            var request = (com.longscoop.ruankao.storage.StorageUploadRequest) invocation.getArgument(0);
            return new StoredObject(request.objectKey(), request.contentType(), request.contentLength());
        });

        var dryRun = contentImportService.dryRun(
                examId,
                "chapter-19.pdf",
                pdf,
                9001L);

        assertEquals(ImportDocumentType.LECTURE, dryRun.detectedType());
        assertEquals(1, dryRun.pageCount());
        assertTrue(dryRun.itemCount() >= 1);
        assertEquals(ImportBatchStatus.REVIEWING, dryRun.status());

        var detail = contentImportService.get(dryRun.batchId()).orElseThrow();
        assertEquals(1, detail.pages().size());
        assertTrue(detail.pages().get(0).imageObjectKey().endsWith("/pages/1.png"));
        assertTrue(detail.items().stream().anyMatch(item -> item.itemType().name().equals("LESSON")));

        contentImportService.approveAll(dryRun.batchId());

        var first = contentImportService.confirm(
                dryRun.batchId(),
                "confirm-chapter-19");
        var second = contentImportService.confirm(
                dryRun.batchId(),
                "confirm-chapter-19");

        assertEquals(first.courseId(), second.courseId());
        assertEquals(1L, courseMapper.selectCount(null));
        assertEquals(1L, lessonMapper.selectCount(null));
        assertEquals(ImportBatchStatus.CONFIRMED,
                contentImportService.get(dryRun.batchId()).orElseThrow().status());
    }

    private byte[] lecturePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText("Lambda Architecture Design");
                stream.newLineAtOffset(0, -20);
                stream.showText("Batch layer stores immutable master data.");
                stream.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
