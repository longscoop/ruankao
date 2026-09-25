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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SourcePdfMaterialImportIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ExamService exams;
    @Autowired UserExamProfileService profiles;
    @MockBean PdfDocumentExtractor extractor;
    @MockBean StorageProvider storage;

    @Test
    void nullControlCharacterInExtractedTextDoesNotAbortPagePersistence() throws Exception {
        long examId = exams.create("nul-source-pdf", "System Architect", ExamStatus.ACTIVE);
        when(extractor.extract(any())).thenReturn(List.of(
                new ExtractedPdfPage(1, "Before\u0000After", new byte[]{1, 2, 3})));
        when(storage.upload(any())).thenAnswer(call -> {
            var upload = (com.longscoop.ruankao.storage.StorageUploadRequest) call.getArgument(0);
            return new StoredObject(upload.objectKey(), upload.contentType(), upload.contentLength());
        });

        String upload = mvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(new MockMultipartFile("file", "nul.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("examId", String.valueOf(examId)).with(admin()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String batchId = json.readTree(upload).get("batchId").asText();
        mvc.perform(get("/api/v1/admin/imports/{id}", batchId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pages[0].textContent").value("BeforeAfter"));
    }

    @Test
    void sourcePagesAreChunkedOnceAndRemainReadableWithoutInventedKnowledge() throws Exception {
        long examId = exams.create("source-material-exam", "System Architect", ExamStatus.ACTIVE);
        List<ExtractedPdfPage> pages = new ArrayList<>();
        for (int number = 1; number <= 13; number++) {
            pages.add(new ExtractedPdfPage(number, "Source text on page " + number, new byte[]{(byte) number}));
        }
        when(extractor.extract(any())).thenReturn(pages);
        when(storage.upload(any())).thenAnswer(call -> {
            var upload = (com.longscoop.ruankao.storage.StorageUploadRequest) call.getArgument(0);
            return new StoredObject(upload.objectKey(), upload.contentType(), upload.contentLength());
        });
        when(storage.generateAccessUrl(anyString(), any())).thenAnswer(call ->
                URI.create("https://cdn.example.test/" + call.getArgument(0, String.class)));

        String batchId = json.readTree(mvc.perform(multipart("/api/v1/admin/imports/pdf")
                        .file(new MockMultipartFile("file", "source.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("examId", String.valueOf(examId)).with(admin()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("batchId").asText();

        JsonNode created = json.readTree(mvc.perform(post("/api/v1/admin/imports/{id}/source-lessons", batchId)
                        .with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(2, created.size());
        mvc.perform(post("/api/v1/admin/imports/{id}/source-lessons", batchId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(created.get(0).asLong()))
                .andExpect(jsonPath("$[1]").value(created.get(1).asLong()));

        JsonNode detail = detail(batchId);
        assertEquals(13, detail.get("pages").size());
        for (int chunk = 0; chunk < 2; chunk++) {
            long itemId = created.get(chunk).asLong();
            JsonNode item = findItem(detail, itemId);
            JsonNode payload = json.readTree(item.get("contentJson").asText());
            int expectedPages = chunk == 0 ? 12 : 1;
            assertEquals(expectedPages, payload.get("blocks").size() / 2);
            assertEquals(chunk == 0 ? 1 : 13, payload.get("sourcePageStart").asInt());
            assertEquals(chunk == 0 ? 12 : 13, payload.get("sourcePageEnd").asInt());
        }

        for (JsonNode item : detail.get("items")) {
            long itemId = item.get("id").asLong();
            String action = (itemId == created.get(0).asLong() || itemId == created.get(1).asLong())
                    ? "approve" : "reject";
            mvc.perform(post("/api/v1/admin/imports/{batchId}/items/{itemId}/{action}",
                            batchId, itemId, action).with(admin()))
                    .andExpect(status().isNoContent());
        }
        for (JsonNode issue : detail.get("issues")) {
            if ("UNRECOGNIZED_DOCUMENT".equals(issue.get("code").asText())) {
                mvc.perform(post("/api/v1/admin/imports/{id}/issues/{issueId}/resolve",
                                batchId, issue.get("id").asLong()).with(admin()))
                        .andExpect(status().isNoContent());
            }
        }

        long courseId = json.readTree(mvc.perform(post("/api/v1/admin/imports/{id}/confirm", batchId)
                        .with(admin()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("confirmKey", "source-" + batchId))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("courseId").asLong();
        detail = detail(batchId);
        long firstLessonId = findItem(detail, created.get(0).asLong()).get("targetId").asLong();
        for (JsonNode itemId : created) {
            mvc.perform(post("/api/v1/admin/imports/{batchId}/items/{itemId}/publish-lesson",
                            batchId, itemId.asLong()).with(admin()).contentType("application/json")
                            .content("{\"knowledgeIds\":[]}"))
                    .andExpect(status().isNoContent());
        }

        long learnerId = 9210L;
        profiles.upsert(learnerId, examId, LocalDate.now().plusMonths(2), 30, FoundationLevel.SOME);
        mvc.perform(get("/api/v1/courses/{id}", courseId).with(user(learnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessons.length()").value(2));
        mvc.perform(get("/api/v1/lessons/{id}", firstLessonId).with(user(learnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeIds.length()").value(0))
                .andExpect(jsonPath("$.blocks[0].textContent").value("Source text on page 1"))
                .andExpect(jsonPath("$.blocks[1].imageUrl").value(
                        org.hamcrest.Matchers.containsString("/pages/1.png")))
                .andExpect(jsonPath("$.blocks[23].imageUrl").value(
                        org.hamcrest.Matchers.containsString("/pages/12.png")));
    }

    private JsonNode detail(String batchId) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/admin/imports/{id}", batchId).with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode findItem(JsonNode detail, long id) {
        for (JsonNode item : detail.get("items")) if (item.get("id").asLong() == id) return item;
        throw new AssertionError("missing item " + id);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(9200L), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(long id) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new RuankaoPrincipal(id), "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
