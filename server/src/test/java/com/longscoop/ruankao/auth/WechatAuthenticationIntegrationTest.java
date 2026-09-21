package com.longscoop.ruankao.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.model.ExamStatus;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WechatAuthenticationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ExamService examService;

    @MockitoBean
    WechatIdentityProvider wechatIdentityProvider;

    @Test
    void loginCreatesStableUserAndBearerSession() throws Exception {
        long examId = examService.create("auth-exam", "System Architect", ExamStatus.ACTIVE);
        when(wechatIdentityProvider.exchangeCode("wx-code"))
                .thenReturn(new WechatIdentity("openid-001", "union-001"));

        JsonNode first = login("wx-code");
        long userId = first.get("userId").asLong();
        String token = first.get("token").asText();

        assertEquals(false, first.get("profileCompleted").asBoolean());

        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(examId))
                .andExpect(jsonPath("$[0].name").value("System Architect"));

        mockMvc.perform(put("/api/v1/users/me/exam-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "examId", examId,
                                "examDate", LocalDate.now().plusMonths(2).toString(),
                                "dailyTargetMinutes", 30,
                                "foundationLevel", "SOME"))))
                .andExpect(status().isNoContent());

        JsonNode second = login("wx-code");
        assertEquals(userId, second.get("userId").asLong());
        assertEquals(true, second.get("profileCompleted").asBoolean());
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/learning/today")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login(String code) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
