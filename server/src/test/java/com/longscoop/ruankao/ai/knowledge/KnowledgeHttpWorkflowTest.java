package com.longscoop.ruankao.ai.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longscoop.ruankao.ai.AiProvider;
import com.longscoop.ruankao.ai.AiProviderResponse;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeHttpWorkflowTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockBean AiProvider provider;
    @MockBean KnowledgeFileStore files;

    @Test
    void administratorUploadToLearnerCitedAnswerWorksThroughTheRealHttpContracts() throws Exception {
        long examId = jdbc.queryForObject(
                "insert into exam(code,name,status) values (?,'HTTP测试','ACTIVE') returning id",
                Long.class, UUID.randomUUID().toString());
        var admin = authenticatedUser(createUser("管理员"), "ADMIN");
        var learner = authenticatedUser(createUser("学员"), "USER");
        var outsider = authenticatedUser(createUser("其他学员"), "USER");
        when(provider.providerName()).thenReturn("test");
        when(provider.modelName()).thenReturn("fake");
        when(provider.complete(any())).thenReturn(new AiProviderResponse(
                "test", "fake", "数据库事务需要保持一致性。[1]", 20, 10));

        ResultActions createBase = mvc.perform(post("/api/v1/admin/knowledge/bases")
                .with(authentication(admin))
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of(
                        "examId", examId, "name", "数据库资料", "description", "HTTP全链路"))));
        String baseId = successfulJson(createBase).path("id").asText();
        assertThat(baseId).isNotBlank();

        String originalText = "数据库事务具有原子性、一致性、隔离性、持久性。";
        byte[] bytes = originalText.getBytes(StandardCharsets.UTF_8);
        ResultActions upload = mvc.perform(multipart(
                        "/api/v1/admin/knowledge/bases/" + baseId + "/documents")
                .file(new MockMultipartFile("file", "事务.md", "text/markdown", bytes))
                .with(authentication(admin)));
        JsonNode uploaded = successfulJson(upload);
        assertThat(uploaded.path("status").asText()).isEqualTo("REVIEW");
        String documentId = uploaded.path("id").asText();
        assertThat(documentId).isNotBlank();

        JsonNode chunks = successfulJson(mvc.perform(get(
                        "/api/v1/admin/knowledge/documents/" + documentId + "/chunks")
                .with(authentication(admin))));
        assertThat(chunks.isArray()).isTrue();
        assertThat(chunks.size()).isEqualTo(1);
        assertThat(chunks.get(0).path("text").asText()).isEqualTo(originalText);

        JsonNode published = successfulJson(mvc.perform(put(
                        "/api/v1/admin/knowledge/documents/" + documentId + "/publication")
                .with(authentication(admin)).contentType("application/json")
                .content("{\"published\":true}")));
        assertThat(published.path("status").asText()).isEqualTo("PUBLISHED");

        Map<String, Object> agentInput = Map.of(
                "name", "数据库助教", "description", "", "instructions", "解释考点",
                "baseIds", List.of(baseId), "enabled", true);
        ResultActions createAgent = mvc.perform(post("/api/v1/admin/knowledge/agents")
                .with(authentication(admin)).contentType("application/json")
                .content(json.writeValueAsString(agentInput)));
        JsonNode createdAgent = successfulJson(createAgent);
        assertThat(createdAgent.path("enabled").asBoolean()).isFalse();
        String agentId = createdAgent.path("id").asText();
        assertThat(agentId).isNotBlank();
        successfulJson(mvc.perform(put("/api/v1/admin/knowledge/agents/" + agentId)
                .with(authentication(admin)).contentType("application/json")
                .content(json.writeValueAsString(agentInput))));

        JsonNode session = successfulJson(mvc.perform(post(
                        "/api/v1/ai/agents/" + agentId + "/sessions")
                .with(authentication(learner))));
        String sessionId = session.path("id").asText();
        assertThat(sessionId).isNotBlank();
        String turnsPath = "/api/v1/ai/agents/sessions/" + sessionId + "/turns";
        String request = json.writeValueAsString(Map.of(
                "requestId", UUID.randomUUID(), "message", "数据库事务"));
        JsonNode answer = successfulJson(mvc.perform(post(turnsPath)
                .with(authentication(learner)).contentType("application/json").content(request)));
        assertThat(answer.path("status").asText()).isEqualTo("GROUNDED");
        JsonNode citation = answer.path("citations").get(0);
        assertThat(citation.path("filename").asText()).isEqualTo("事务.md");
        assertThat(citation.path("documentId").asText()).isEqualTo(documentId);
        long chunkId = citation.path("chunkId").asLong();

        JsonNode retried = successfulJson(mvc.perform(post(turnsPath)
                .with(authentication(learner)).contentType("application/json").content(request)));
        assertThat(retried.path("id").asLong()).isEqualTo(answer.path("id").asLong());
        verify(provider, times(1)).complete(any());
        mvc.perform(get(turnsPath).with(authentication(outsider)))
                .andExpect(status().isNotFound());

        String sourcePath = "/api/v1/ai/agents/" + agentId + "/sources/" + chunkId;
        JsonNode source = successfulJson(mvc.perform(get(sourcePath).with(authentication(learner))));
        assertThat(source.path("text").asText()).isEqualTo(originalText);
        when(files.read(UUID.fromString(documentId))).thenReturn(bytes);
        String originalPath = "/api/v1/admin/knowledge/documents/" + documentId + "/original";
        MvcResult original = mvc.perform(get(originalPath).with(authentication(admin)))
                .andExpect(status().isOk()).andReturn();
        assertThat(original.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(original.getResponse().getContentAsByteArray()).isEqualTo(bytes);
        mvc.perform(get(originalPath).with(authentication(learner)))
                .andExpect(status().isForbidden());

        successfulJson(mvc.perform(put(
                        "/api/v1/admin/knowledge/documents/" + documentId + "/publication")
                .with(authentication(admin)).contentType("application/json")
                .content("{\"published\":false}")));
        mvc.perform(get(sourcePath).with(authentication(learner)))
                .andExpect(status().isNotFound());
        JsonNode retainedHistory = successfulJson(mvc.perform(get(turnsPath).with(authentication(learner))));
        assertThat(retainedHistory.size()).isEqualTo(1);
    }

    private JsonNode successfulJson(ResultActions actions) throws Exception {
        MvcResult result = actions.andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long createUser(String name) {
        return jdbc.queryForObject("insert into app_user(display_name) values (?) returning id", Long.class, name);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(long id, String role) {
        return new UsernamePasswordAuthenticationToken(new RuankaoPrincipal(id), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
