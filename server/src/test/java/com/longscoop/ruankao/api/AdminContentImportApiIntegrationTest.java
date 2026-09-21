package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.ai.AiProvider;
import com.longscoop.ruankao.ai.AiProviderResponse;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.content.persistence.LessonMapper;
import com.longscoop.ruankao.course.model.CourseStatus;
import com.longscoop.ruankao.course.persistence.CourseMapper;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.knowledge.KnowledgePointService;
import com.longscoop.ruankao.knowledge.model.KnowledgeStatus;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.storage.StoredObject;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminContentImportApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExamService examService;
    @Autowired KnowledgePointService knowledgePointService;
    @Autowired LessonMapper lessonMapper;
    @Autowired CourseMapper courseMapper;

    @MockBean StorageProvider storageProvider;
    @MockBean AiProvider aiProvider;

    @Test
    void adminCanUploadReviewConfirmAndPublishLecture() throws Exception {
        long examId = examService.create("admin-import-exam", "System Architect", ExamStatus.ACTIVE);
        long knowledgeId = knowledgePointService.create(
                examId, null, 1, "lambda-architecture", "Lambda Architecture",
                null, 5, 80, 20, 1, KnowledgeStatus.ACTIVE);

        when(storageProvider.upload(any())).thenAnswer(invocation -> {
            var request=(com.longscoop.ruankao.storage.StorageUploadRequest)invocation.getArgument(0);
            return new StoredObject(request.objectKey(), request.contentType(), request.contentLength());
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "lambda.pdf", "application/pdf", lecturePdf());

        String uploadBody = mockMvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(file)
                        .param("examId", String.valueOf(examId))
                        .with(admin(7001L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").isString())
                .andExpect(jsonPath("$.detectedType").value("LECTURE"))
                .andReturn().getResponse().getContentAsString();

        String batchId = objectMapper.readTree(uploadBody).get("batchId").asText();

        mockMvc.perform(post("/api/v1/admin/imports/{id}/approve-all", batchId)
                        .with(admin(7001L)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/imports/{id}/confirm", batchId)
                        .with(admin(7001L))
                        .contentType("application/json")
                        .content("{\"confirmKey\":\"admin-lambda-confirm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").isNumber());

        JsonNode detail = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/admin/imports/{id}", batchId)
                                .with(admin(7001L)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        JsonNode lessonItem = null;
        for (JsonNode item : detail.get("items")) {
            if ("LESSON".equals(item.get("itemType").asText())) {
                lessonItem = item;
                break;
            }
        }
        long lessonItemId = lessonItem.get("id").asLong();
        long lessonId = lessonItem.get("targetId").asLong();

        mockMvc.perform(post("/api/v1/admin/imports/{batchId}/items/{itemId}/publish-lesson",
                                batchId, lessonItemId)
                        .with(admin(7001L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "knowledgeIds", List.of(knowledgeId)))))
                .andExpect(status().isNoContent());

        assertEquals("PUBLISHED", lessonMapper.selectById(lessonId).getStatus().name());
        assertEquals(CourseStatus.PUBLISHED,
                courseMapper.selectById(detail.get("courseId").asLong()).getStatus());
    }


    @Test
    void adminCanRequestAiStructureSuggestionWithoutChangingImportItem() throws Exception {
        long examId = examService.create("admin-ai-import-exam", "System Architect AI", ExamStatus.ACTIVE);
        when(storageProvider.upload(any())).thenAnswer(invocation -> {
            var request = (com.longscoop.ruankao.storage.StorageUploadRequest) invocation.getArgument(0);
            return new StoredObject(request.objectKey(), request.contentType(), request.contentLength());
        });
        when(aiProvider.complete(any())).thenReturn(
                new AiProviderResponse("Qwen", "qwen-test",
                        "{\"warnings\":[\"review heading\"]}", 12, 6));

        MockMultipartFile file = new MockMultipartFile(
                "file", "ai-lecture.pdf", "application/pdf", lecturePdf());

        String uploadBody = mockMvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(file)
                        .param("examId", String.valueOf(examId))
                        .with(admin(7003L)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String batchId = objectMapper.readTree(uploadBody).get("batchId").asText();

        JsonNode detail = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/admin/imports/{id}", batchId)
                                .with(admin(7003L)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        long itemId = detail.get("items").get(0).get("id").asLong();
        String originalJson = detail.get("items").get(0).get("contentJson").asText();

        mockMvc.perform(post("/api/v1/admin/imports/{batchId}/items/{itemId}/ai-suggest",
                                batchId, itemId)
                        .with(admin(7003L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{\"warnings\":[\"review heading\"]}"));

        JsonNode after = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/admin/imports/{id}", batchId)
                                .with(admin(7003L)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        assertEquals(originalJson, after.get("items").get(0).get("contentJson").asText());
    }

    @Test
    void learnerRoleCannotUseUploadEndpoint() throws Exception {
        long examId = examService.create("admin-denied-exam", "Denied", ExamStatus.ACTIVE);
        MockMultipartFile file = new MockMultipartFile(
                "file", "denied.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(file)
                        .param("examId", String.valueOf(examId))
                        .with(user(7002L)))
                .andExpect(status().isForbidden());
    }

    private byte[] lecturePdf() throws Exception {
        try(PDDocument document=new PDDocument()){
            PDPage page=new PDPage(); document.addPage(page);
            try(PDPageContentStream stream=new PDPageContentStream(document,page)){
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(40,700);
                stream.showText("Lambda Architecture");
                stream.newLineAtOffset(0,-20);
                stream.showText("Batch and speed layers provide robust data processing.");
                stream.endText();
            }
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin(long userId){
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),"n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long userId){
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(userId),"n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
