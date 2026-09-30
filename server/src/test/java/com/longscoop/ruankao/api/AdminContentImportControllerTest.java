package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.content.ContentImportReviewService;
import com.longscoop.ruankao.content.ContentImportService;
import com.longscoop.ruankao.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminContentImportControllerTest {

    @Test
    void unknownExamIsReportedAsBadRequest() {
        ContentImportService importService = mock(ContentImportService.class);
        when(importService.dryRun(eq(999L), eq("material.pdf"), any(byte[].class), eq(7L)))
                .thenThrow(new IllegalArgumentException("exam must exist"));
        var controller = new AdminContentImportController(
                importService,
                mock(ContentImportReviewService.class),
                mock(StorageProvider.class));
        var file = new MockMultipartFile("file", "material.pdf", "application/pdf", new byte[] {1});

        var response = controller.upload(new RuankaoPrincipal(7L), 999L, file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("message", "exam must exist"), response.getBody());
    }
}
