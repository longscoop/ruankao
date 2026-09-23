package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.content.pdf.ExtractedPdfPage;
import com.longscoop.ruankao.content.pdf.PdfDocumentExtractor;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.storage.StoredObject;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import com.longscoop.ruankao.user.UserExamProfileService;
import com.longscoop.ruankao.user.model.FoundationLevel;
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

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MixedPdfImportEndToEndIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExamService examService;
    @Autowired UserExamProfileService profileService;

    @MockBean StorageProvider storageProvider;
    @MockBean PdfDocumentExtractor extractor;

    @Test
    void mixedLectureAndTypicalQuestionCanBeReviewedPublishedAndLearned() throws Exception {
        long examId=examService.create("mixed-pdf-exam","System Architect", ExamStatus.ACTIVE);

        when(extractor.extract(any())).thenReturn(List.of(
                new ExtractedPdfPage(1, """
                        大数据处理系统架构分析
                        大数据处理系统架构特征：
                        1.鲁棒性和容错性
                        2.低延迟读取和更新能力
                        3.横向扩容
                        """, new byte[]{1,2,3}),
                new ExtractedPdfPage(2, """
                        典型真题
                        以下不属于大数据处理系统架构特征的是（）。
                        A.鲁棒性
                        B.容错性
                        C.纵向扩容
                        D.即席查询能力
                        参考答案：C
                        """, new byte[]{4,5,6})
        ));
        when(storageProvider.upload(any())).thenAnswer(invocation -> {
            var request=(com.longscoop.ruankao.storage.StorageUploadRequest)invocation.getArgument(0);
            return new StoredObject(request.objectKey(),request.contentType(),request.contentLength());
        });
        when(storageProvider.generateAccessUrl(anyString(),any()))
                .thenAnswer(invocation -> URI.create(
                        "https://cdn.example.test/" + invocation.getArgument(0,String.class)));

        MockMultipartFile file=new MockMultipartFile(
                "file","chapter-big-data.pdf","application/pdf",new byte[]{9,9,9});

        String upload=mockMvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(file)
                        .param("examId",String.valueOf(examId))
                        .with(admin(9101L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detectedType").value("MIXED"))
                .andReturn().getResponse().getContentAsString();
        String batchId=objectMapper.readTree(upload).get("batchId").asText();

        mockMvc.perform(post("/api/v1/admin/imports/{id}/approve-all",batchId)
                        .with(admin(9101L)))
                .andExpect(status().isNoContent());

        JsonNode beforeConfirm=detail(batchId);
        long knowledgeItemId=findItem(beforeConfirm,"KNOWLEDGE").get("id").asLong();
        long questionItemId=findItem(beforeConfirm,"QUESTION").get("id").asLong();

        String confirm=mockMvc.perform(post("/api/v1/admin/imports/{id}/confirm",batchId)
                        .with(admin(9101L))
                        .contentType("application/json")
                        .content("{\"confirmKey\":\"mixed-pdf-confirm\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long courseId=objectMapper.readTree(confirm).get("courseId").asLong();

        JsonNode afterConfirm=detail(batchId);
        JsonNode lessonItem=findItem(afterConfirm,"LESSON");
        long lessonItemId=lessonItem.get("id").asLong();
        long lessonId=lessonItem.get("targetId").asLong();

        String knowledgePublish=mockMvc.perform(post(
                                "/api/v1/admin/imports/{batchId}/items/{itemId}/publish-knowledge",
                                batchId,knowledgeItemId)
                        .with(admin(9101L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code","big-data-architecture",
                                "importance",5,
                                "examFrequency",80,
                                "estimatedMinutes",20,
                                "sortOrder",1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long knowledgeId=objectMapper.readTree(knowledgePublish).get("id").asLong();

        mockMvc.perform(post(
                                "/api/v1/admin/imports/{batchId}/items/{itemId}/publish-lesson",
                                batchId,lessonItemId)
                        .with(admin(9101L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("knowledgeIds",List.of(knowledgeId)))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(
                                "/api/v1/admin/imports/{batchId}/items/{itemId}/publish-question",
                                batchId,questionItemId)
                        .with(admin(9101L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "difficulty","MEDIUM",
                                "source","REAL_EXAM",
                                "knowledgeLinks",List.of(Map.of(
                                        "knowledgeId",knowledgeId,
                                        "weight",1.0,
                                        "primary",true))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/api/v1/admin/imports/{id}",batchId)
                        .with(admin(9101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        long learnerId=9102L;
        profileService.upsert(
                learnerId,examId, LocalDate.now().plusMonths(2),30, FoundationLevel.SOME);

        mockMvc.perform(get("/api/v1/courses/{id}",courseId).with(user(learnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessons[0].id").value(lessonId));

        mockMvc.perform(get("/api/v1/lessons/{id}",lessonId).with(user(learnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].textContent").value(
                        org.hamcrest.Matchers.containsString("鲁棒性和容错性")))
                .andExpect(jsonPath("$.blocks[1].imageUrl").value(
                        org.hamcrest.Matchers.containsString("/pages/1.png")));

        String session=mockMvc.perform(post("/api/v1/question-sessions")
                        .with(user(learnerId))
                        .contentType("application/json")
                        .content("{\"source\":\"REAL_EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId=objectMapper.readTree(session).get("sessionId").asText();

        mockMvc.perform(get("/api/v1/question-sessions/{id}/questions",sessionId)
                        .with(user(learnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value(
                        "以下不属于大数据处理系统架构特征的是（）。"))
                .andExpect(jsonPath("$[0].options[2].key").value("C"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("standardAnswer"))));
    }

    private JsonNode detail(String batchId) throws Exception {
        String body=mockMvc.perform(get("/api/v1/admin/imports/{id}",batchId)
                        .with(admin(9101L)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode findItem(JsonNode detail,String type){
        for(JsonNode item:detail.get("items")){
            if(type.equals(item.get("itemType").asText())) return item;
        }
        throw new AssertionError("missing item type "+type);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin(long id){
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(id),"n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long id){
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(id),"n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
