package com.longscoop.ruankao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.longscoop.ruankao.content.pdf.ExtractedPdfPage;
import com.longscoop.ruankao.content.pdf.PdfDocumentExtractor;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.storage.StorageProvider;
import com.longscoop.ruankao.storage.StoredObject;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "ruankao.admin.username=upload-test-admin", "ruankao.admin.password=upload-test-password"})
class LargePdfUploadHttpIntegrationTest extends PostgresIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired MultipartProperties multipartProperties;
    @Autowired ExamService exams;
    @MockBean PdfDocumentExtractor extractor;
    @MockBean StorageProvider storage;

    @Test
    void uploadLimitCoversLargestSourcePdfAndRequestReachesController() {
        assertTrue(multipartProperties.getMaxFileSize().toBytes() >= 57_000_000);
        assertTrue(multipartProperties.getMaxRequestSize().toBytes() >= 57_000_000);
        long examId = exams.create("large-pdf-http", "System Architect", ExamStatus.INACTIVE);
        when(extractor.extract(any())).thenReturn(List.of(
                new ExtractedPdfPage(1, "Large source PDF", new byte[]{1, 2, 3})));
        when(storage.upload(any())).thenAnswer(call -> {
            var upload = (com.longscoop.ruankao.storage.StorageUploadRequest) call.getArgument(0);
            return new StoredObject(upload.objectKey(), upload.contentType(), upload.contentLength());
        });

        JsonNode login = http.postForObject("/api/v1/auth/admin/login",
                Map.of("username", "upload-test-admin", "password", "upload-test-password"), JsonNode.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.get("token").asText());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("examId", String.valueOf(examId));
        form.add("file", new ByteArrayResource(new byte[2_000_000]) {
            @Override public String getFilename() { return "large.pdf"; }
        });

        ResponseEntity<JsonNode> response = http.postForEntity(
                "/api/v1/admin/imports/pdf", new HttpEntity<>(form, headers), JsonNode.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().get("pageCount").asInt());
    }
}
