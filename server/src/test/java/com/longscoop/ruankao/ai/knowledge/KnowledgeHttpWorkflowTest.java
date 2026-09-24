package com.longscoop.ruankao.ai.knowledge;

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
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeHttpWorkflowTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockBean AiProvider provider;
    @MockBean KnowledgeFileStore files;

    @Test void administratorUploadToLearnerCitedAnswerWorksThroughTheRealHttpContracts() throws Exception {
        long examId = jdbc.queryForObject("insert into exam(code,name,status) values (?,'HTTP测试','ACTIVE') returning id", Long.class, UUID.randomUUID().toString());
        long adminId = userId("管理员");
        long learnerId = userId("学员");
        long outsiderId = userId("其他学员");
        var admin = auth(adminId, "ADMIN");
        var learner = auth(learnerId, "USER");
        var outsider = auth(outsiderId, "USER");
        when(provider.providerName()).thenReturn("test");
        when(provider.modelName()).thenReturn("fake");
        when(provider.complete(any())).thenReturn(new AiProviderResponse("test", "fake", "数据库事务需要保持一致性。[1]", 20, 10));
        String base = id(mvc.perform(post("/api/v1/admin/knowledge/bases").with(authentication(admin))
                .contentType("application/json").content(json.writeValueAsString(Map.of("examId", examId, "name", "数据库资料", "description", "HTTP全链路")))));
        byte[] bytes = "数据库事务具有原子性、一致性、隔离性、持久性。".getBytes(StandardCharsets.UTF_8);
        String document = id(mvc.perform(multipart("/api/v1/admin/knowledge/bases/" + base + "/documents")
                .file(new MockMultipartFile("file", "事务.md", "text/markdown", bytes)).with(authentication(admin)))
                .andExpect(jsonPath("$.status").value("REVIEW")));
        mvc.perform(get("/api/v1/admin/knowledge/documents/" + document + "/chunks").with(authentication(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].text").value(new String(bytes, StandardCharsets.UTF_8)));
        mvc.perform(put("/api/v1/admin/knowledge/documents/" + document + "/publication").with(authentication(admin))
                .contentType("application/json").content("{\"published\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        Map<String, Object> agentInput = Map.of("name", "数据库助教", "description", "", "instructions", "解释考点", "baseIds", List.of(base), "enabled", true);
        String agent = id(mvc.perform(post("/api/v1/admin/knowledge/agents").with(authentication(admin))
                .contentType("application/json").content(json.writeValueAsString(agentInput)))
                .andExpect(jsonPath("$.enabled").value(false)));
        mvc.perform(put("/api/v1/admin/knowledge/agents/" + agent).with(authentication(admin))
                .contentType("application/json").content(json.writeValueAsString(agentInput))).andExpect(status().isOk());
        String session = id(mvc.perform(post("/api/v1/ai/agents/" + agent + "/sessions").with(authentication(learner))));
        String request = json.writeValueAsString(Map.of("requestId", UUID.randomUUID(), "message", "数据库事务"));
        var result = mvc.perform(post("/api/v1/ai/agents/sessions/" + session + "/turns").with(authentication(learner))
                .contentType("application/json").content(request)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GROUNDED"))
                .andExpect(jsonPath("$.citations[0].filename").value("事务.md"))
                .andExpect(jsonPath("$.citations[0].documentId").value(document)).andReturn();
        long chunk = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("citations").get(0).path("chunkId").asLong();
        mvc.perform(post("/api/v1/ai/agents/sessions/" + session + "/turns").with(authentication(learner))
                .contentType("application/json").content(request)).andExpect(status().isOk());
        verify(provider, times(1)).complete(any());
        mvc.perform(get("/api/v1/ai/agents/sessions/" + session + "/turns").with(authentication(outsider)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ai/agents/" + agent + "/sources/" + chunk).with(authentication(learner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value(new String(bytes, StandardCharsets.UTF_8)));
        when(files.read(UUID.fromString(document))).thenReturn(bytes);
        mvc.perform(get("/api/v1/admin/knowledge/documents/" + document + "/original").with(authentication(admin)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(bytes));
        mvc.perform(get("/api/v1/admin/knowledge/documents/" + document + "/original").with(authentication(learner)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/knowledge/documents/" + document + "/publication").with(authentication(admin))
                .contentType("application/json").content("{\"published\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ai/agents/" + agent + "/sources/" + chunk).with(authentication(learner)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ai/agents/sessions/" + session + "/turns").with(authentication(learner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    private String id(ResultActions action) throws Exception {
        return json.readTree(action.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id").asText();
    }
    private long userId(String name) { return jdbc.queryForObject("insert into app_user(display_name) values (?) returning id", Long.class, name); }
    private UsernamePasswordAuthenticationToken auth(long id, String role) {
        return new UsernamePasswordAuthenticationToken(new RuankaoPrincipal(id), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
