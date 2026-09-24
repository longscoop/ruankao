package com.longscoop.ruankao.ai.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@Repository
public class KnowledgeConversationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final KnowledgeRepository knowledge;
    private final TransactionTemplate transaction;

    public KnowledgeConversationRepository(JdbcTemplate jdbc, ObjectMapper json,
                                           KnowledgeRepository knowledge, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.json = json;
        this.knowledge = knowledge;
        this.transaction = new TransactionTemplate(manager);
    }
    public Session start(long userId, UUID agentId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into kb_session(id,user_id,agent_id) values (?,?,?)", id, userId, agentId);
        return owned(userId, id);
    }
    public Session owned(long userId, UUID id) {
        return KnowledgeRepository.first(jdbc.query("select * from kb_session where id=? and user_id=?",
                sessionMapper(), id, userId));
    }
    public List<Session> sessions(long userId, UUID agentId, int offset, int limit) {
        return jdbc.query("select * from kb_session where user_id=? and agent_id=? order by updated_at desc,id limit ? offset ?",
                sessionMapper(), userId, agentId, limit, offset);
    }
    public List<Turn> history(UUID sessionId, int offset, int limit) {
        return jdbc.query("select * from kb_turn where session_id=? order by id limit ? offset ?", turnMapper(), sessionId, limit, offset);
    }
    public List<Turn> recent(UUID sessionId) {
        var turns = new ArrayList<>(jdbc.query("select * from kb_turn where session_id=? order by id desc limit 6", turnMapper(), sessionId));
        Collections.reverse(turns);
        return List.copyOf(turns);
    }
    public Optional<Turn> cached(UUID sessionId, UUID requestId) {
        return jdbc.query("select * from kb_turn where session_id=? and request_id=?", turnMapper(), sessionId, requestId).stream().findFirst();
    }
    public int turnCount(UUID sessionId) {
        return jdbc.queryForObject("select count(*) from kb_turn where session_id=?", Integer.class, sessionId);
    }
    public boolean acquire(long userId, UUID sessionId, UUID token) {
        return jdbc.update("""
                update kb_session set busy_token=?,busy_until=now()+interval '2 minutes'
                where id=? and user_id=? and (busy_until is null or busy_until < now())
                """, token, sessionId, userId) == 1;
    }
    public void release(UUID sessionId, UUID token) {
        jdbc.update("update kb_session set busy_token=null,busy_until=null where id=? and busy_token=?", sessionId, token);
    }
    public Turn finish(long userId, Session session, UUID token, UUID requestId, String question,
                       GroundedAnswer.Result answer, List<Citation> suppliedSources) {
        return transaction.execute(status -> {
            knowledge.lockCurrentEvidence(session.agentId(), suppliedSources);
            int changed = jdbc.update("""
                    update kb_session set busy_token=null,busy_until=null,updated_at=now(),
                    title=case when title='新会话' then ? else title end
                    where id=? and user_id=? and busy_token=? and busy_until > now()
                    """, question.substring(0, Math.min(60, question.length())), session.id(), userId, token);
            if (changed != 1) throw KnowledgeException.conflict("请求已超时或会话已变化，请重试");
            return KnowledgeRepository.first(jdbc.query("""
                    insert into kb_turn(session_id,request_id,question,content,status,citations)
                    values (?,?,?,?,?,cast(? as jsonb)) returning *
                    """, turnMapper(), session.id(), requestId, question, answer.content(),
                    answer.status().name(), knowledge.encode(answer.citations())));
        });
    }
    private RowMapper<Session> sessionMapper() {
        return (rs, row) -> new Session(rs.getObject("id", UUID.class), rs.getObject("agent_id", UUID.class),
                rs.getString("title"), rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
    }
    private RowMapper<Turn> turnMapper() {
        return (rs, row) -> new Turn(rs.getLong("id"), rs.getObject("request_id", UUID.class), rs.getString("question"),
                rs.getString("content"), AnswerStatus.valueOf(rs.getString("status")),
                citations(rs.getString("citations")), rs.getObject("created_at", OffsetDateTime.class));
    }
    private List<Citation> citations(String value) {
        try { return json.readValue(value, new TypeReference<List<Citation>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("会话来源记录无法读取", e); }
    }
}
