package com.longscoop.ruankao.ai.knowledge;

import com.longscoop.ruankao.ai.AiProvider;
import com.longscoop.ruankao.ai.AiProviderResponse;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeIntegrationTest extends PostgresIntegrationTest {
    @Autowired KnowledgeService knowledge;
    @Autowired KnowledgeRepository repository;
    @Autowired KnowledgeChatService chat;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockBean AiProvider provider;
    @MockBean KnowledgeFileStore files;
    long userId;
    long otherUserId;
    long examId;
    Base base;
    Agent agent;

    @BeforeEach void setup() {
        reset(provider, files);
        examId = jdbc.queryForObject("insert into exam(code,name,status) values (?,?,'ACTIVE') returning id",
                Long.class, "KB-" + UUID.randomUUID(), "知识库测试");
        userId = jdbc.queryForObject("insert into app_user(display_name) values ('学员') returning id", Long.class);
        otherUserId = jdbc.queryForObject("insert into app_user(display_name) values ('其他学员') returning id", Long.class);
        base = knowledge.createBase(new BaseInput(examId, "架构资料", "测试知识库"));
        agent = knowledge.createAgent(new AgentInput("架构助教", "有来源的讲解", "举例说明", List.of(base.id()), false));
        assertThat(agent.enabled()).isFalse();
        agent = knowledge.updateAgent(agent.id(), new AgentInput(agent.name(), agent.description(),
                agent.instructions(), agent.baseIds(), true));
        when(provider.providerName()).thenReturn("test");
        when(provider.modelName()).thenReturn("fake");
        when(provider.complete(any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response("分层架构按职责划分层次。[1]");
        });
    }

    @Test void uploadDeduplicatesAndOnlyPublishedBoundDocumentsAreSearchable() {
        var document = upload("分层架构按职责划分层次。每层提供清晰接口。", false);
        var duplicate = knowledge.upload(base.id(), "重复.txt", "分层架构按职责划分层次。每层提供清晰接口。".getBytes(StandardCharsets.UTF_8), userId);
        assertThat(duplicate.id()).isEqualTo(document.id());
        verify(files, times(1)).put(any(), any());
        assertThat(repository.search(agent.id(), "分层架构")).isEmpty();
        knowledge.setPublished(document.id(), true);
        var hits = repository.search(agent.id(), "分层架构");
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).page()).isEqualTo(1);
        assertThat(hits.get(0).filename()).isEqualTo("资料.txt");
        var separate = knowledge.createBase(new BaseInput(examId, "隔离库", ""));
        var hidden = knowledge.upload(separate.id(), "隐藏.txt", "分层架构专有隐藏证据".getBytes(StandardCharsets.UTF_8), userId);
        knowledge.setPublished(hidden.id(), true);
        assertThat(repository.search(agent.id(), "分层架构")).noneMatch(c -> c.documentId().equals(hidden.id()));
        knowledge.setPublished(document.id(), false);
        assertThat(repository.search(agent.id(), "分层架构")).isEmpty();
        assertThatThrownBy(() -> knowledge.source(agent.id(), hits.get(0).chunkId())).isInstanceOf(KnowledgeException.class);
    }

    @Test void invalidInputAndStorageFailureDoNotCreateDocuments() {
        assertThatThrownBy(() -> knowledge.upload(base.id(), "空.txt", new byte[0], userId)).isInstanceOf(IllegalArgumentException.class);
        doThrow(new IllegalStateException("disk unavailable")).when(files).put(any(), any());
        assertThatThrownBy(() -> upload("事务一致性", false)).isInstanceOf(IllegalStateException.class);
        assertThat(knowledge.documents(base.id(), 0, 50)).isEmpty();
    }

    @Test void noEvidenceSkipsModelAndSessionOwnershipIsEnforced() {
        var session = chat.start(userId, agent.id());
        var answer = chat.ask(userId, session.id(), UUID.randomUUID(), "量子纠缠");
        assertThat(answer.status()).isEqualTo(AnswerStatus.NO_EVIDENCE);
        verify(provider, never()).complete(any());
        assertThat(chat.history(userId, session.id(), 0, 50)).hasSize(1);
        assertThat(chat.sessions(otherUserId, agent.id(), 0, 50)).isEmpty();
        assertThatThrownBy(() -> chat.history(otherUserId, session.id(), 0, 50)).isInstanceOf(KnowledgeException.class);
        assertThatThrownBy(() -> chat.ask(otherUserId, session.id(), UUID.randomUUID(), "分层架构")).isInstanceOf(KnowledgeException.class);
    }

    @Test void successfulRetryIsIdempotentAndChangedPayloadConflicts() {
        upload("分层架构按职责划分层次。", true);
        var session = chat.start(userId, agent.id());
        var requestId = UUID.randomUUID();
        var answer = chat.ask(userId, session.id(), requestId, "分层架构是什么？");
        assertThat(answer.status()).isEqualTo(AnswerStatus.GROUNDED);
        assertThat(answer.citations()).isNotEmpty();
        assertThat(chat.ask(userId, session.id(), requestId, "分层架构是什么？").id()).isEqualTo(answer.id());
        assertThatThrownBy(() -> chat.ask(userId, session.id(), requestId, "另一问题"))
                .isInstanceOfSatisfying(KnowledgeException.class, e -> assertThat(e.status()).isEqualTo(409));
        verify(provider, times(1)).complete(any());
        assertThat(jdbc.queryForObject("select count(*) from ai_usage_log where user_id=? and function='KNOWLEDGE_CHAT'", Integer.class, userId)).isEqualTo(1);
    }

    @Test void failuresReleaseLeaseAndDoNotSaveFakeSuccess() {
        upload("分层架构按职责划分层次。", true);
        var session = chat.start(userId, agent.id());
        var id = UUID.randomUUID();
        when(provider.complete(any())).thenThrow(new IllegalStateException("provider unavailable"));
        assertThatThrownBy(() -> chat.ask(userId, session.id(), id, "分层架构")).isInstanceOf(IllegalStateException.class);
        assertThat(chat.history(userId, session.id(), 0, 50)).isEmpty();
        when(provider.complete(any())).thenReturn(response("没有依据的结论[999]"));
        assertThat(chat.ask(userId, session.id(), id, "分层架构").status()).isEqualTo(AnswerStatus.EXTRACT_ONLY);
    }

    @Test void rejectsConcurrentTurnsInTheSameSession() throws Exception {
        upload("分层架构按职责划分层次。", true);
        var session = chat.start(userId, agent.id());
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.complete(any())).thenAnswer(call -> {
            entered.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return response("分层架构按职责划分。[1]");
        });
        var first = CompletableFuture.supplyAsync(() -> chat.ask(userId, session.id(), UUID.randomUUID(), "分层架构"));
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> chat.ask(userId, session.id(), UUID.randomUUID(), "分层架构"))
                    .isInstanceOfSatisfying(KnowledgeException.class, e -> assertThat(e.status()).isEqualTo(409));
        } finally { release.countDown(); }
        assertThat(first.get(5, TimeUnit.SECONDS).status()).isEqualTo(AnswerStatus.GROUNDED);
        assertThat(chat.history(userId, session.id(), 0, 50)).hasSize(1);
    }

    @Test void changedLeaseCannotCommitOrReleaseAnotherRequestsLease() {
        upload("分层架构按职责划分层次。", true);
        var session = chat.start(userId, agent.id());
        var replacement = UUID.randomUUID();
        when(provider.complete(any())).thenAnswer(call -> {
            jdbc.update("update kb_session set busy_token=?, busy_until=now()+interval '2 minutes' where id=?", replacement, session.id());
            return response("分层架构。[1]");
        });
        assertThatThrownBy(() -> chat.ask(userId, session.id(), UUID.randomUUID(), "分层架构"))
                .isInstanceOf(KnowledgeException.class);
        assertThat(chat.history(userId, session.id(), 0, 50)).isEmpty();
        assertThat(jdbc.queryForObject("select busy_token from kb_session where id=?", UUID.class, session.id())).isEqualTo(replacement);
    }

    @Test void withdrawalDuringGenerationIsNotSavedAsCurrentEvidence() {
        var document = upload("分层架构按职责划分层次。", true);
        var session = chat.start(userId, agent.id());
        when(provider.complete(any())).thenAnswer(call -> {
            knowledge.setPublished(document.id(), false);
            return response("分层架构。[1]");
        });
        assertThatThrownBy(() -> chat.ask(userId, session.id(), UUID.randomUUID(), "分层架构"))
                .isInstanceOf(KnowledgeException.class);
        assertThat(chat.history(userId, session.id(), 0, 50)).isEmpty();
    }

    @Test void disabledAgentsCannotStartOrAnswerNewTurns() {
        var session = chat.start(userId, agent.id());
        knowledge.updateAgent(agent.id(), new AgentInput(agent.name(), "", "", agent.baseIds(), false));
        assertThat(knowledge.availableAgents()).noneMatch(a -> a.id().equals(agent.id()));
        assertThatThrownBy(() -> chat.start(userId, agent.id())).isInstanceOf(KnowledgeException.class);
        assertThatThrownBy(() -> chat.ask(userId, session.id(), UUID.randomUUID(), "分层架构")).isInstanceOf(KnowledgeException.class);
    }

    @Test void crudAndExamBoundariesAreValidated() {
        var spare = knowledge.createBase(new BaseInput(examId, "备用", ""));
        assertThat(knowledge.updateBase(spare.id(), new BaseInput(examId, "改名", "说明")).name()).isEqualTo("改名");
        knowledge.deleteBase(spare.id());
        assertThat(knowledge.bases()).noneMatch(b -> b.id().equals(spare.id()));
        var otherExam = jdbc.queryForObject("insert into exam(code,name,status) values (?,'其他考试','ACTIVE') returning id", Long.class, UUID.randomUUID().toString());
        var otherBase = knowledge.createBase(new BaseInput(otherExam, "其他库", ""));
        assertThatThrownBy(() -> knowledge.createAgent(new AgentInput("跨考试", "", "", List.of(base.id(), otherBase.id()), false))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> knowledge.deleteBase(base.id())).isInstanceOf(KnowledgeException.class);
        var document = upload("可删除资料", false);
        knowledge.deleteDocument(document.id());
        verify(files).delete(document.id());
    }

    @Test void restRequiresAdminAndDoesNotExposeOtherUsersSessions() throws Exception {
        mvc.perform(get("/api/v1/admin/knowledge/bases")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/knowledge/bases").with(user("learner").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/knowledge/bases").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        var session = chat.start(userId, agent.id());
        var auth = new UsernamePasswordAuthenticationToken(new RuankaoPrincipal(otherUserId), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get("/api/v1/ai/agents/sessions/" + session.id() + "/turns").with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    private Document upload(String text, boolean published) {
        var doc = knowledge.upload(base.id(), "资料.txt", text.getBytes(StandardCharsets.UTF_8), userId);
        return published ? knowledge.setPublished(doc.id(), true) : doc;
    }

    private AiProviderResponse response(String content) {
        return new AiProviderResponse("test", "fake", content, 50, 20);
    }
}
