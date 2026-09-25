package com.longscoop.ruankao.ai.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@Repository
public class KnowledgeRepository {
    private static final String AGENT_SELECT = """
            select a.*, array(select ab.base_id from kb_agent_base ab
            where ab.agent_id=a.id order by ab.base_id) as base_ids from kb_agent a
            """;
    private static final Set<String> STOP = Set.of("什么", "么是", "是什", "如何", "为什", "么会", "请问",
            "一下", "解释", "介绍", "这个", "那个", "它们", "哪些", "的", "了", "吗");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public KnowledgeRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Base> bases() {
        return jdbc.query("select * from kb_base order by created_at desc, id", baseMapper());
    }
    public Base requireBase(UUID id, boolean lock) {
        return first(jdbc.query("select * from kb_base where id=?" + (lock ? " for update" : ""), baseMapper(), id));
    }
    public void requireExam(long id) {
        if (id <= 0 || jdbc.queryForObject("select count(*) from exam where id=?", Integer.class, id) == 0) {
            throw new IllegalArgumentException("请选择有效考试");
        }
    }
    public Base insertBase(BaseInput input) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into kb_base(id,exam_id,name,description) values (?,?,?,?)",
                id, input.examId(), input.name(), input.description());
        return requireBase(id, false);
    }
    public Base updateBase(UUID id, BaseInput input) {
        jdbc.update("update kb_base set name=?,description=? where id=?", input.name(), input.description(), id);
        return requireBase(id, false);
    }
    public void deleteBase(UUID id) { jdbc.update("delete from kb_base where id=?", id); }

    public List<Document> documents(UUID baseId, int offset, int limit) {
        return jdbc.query("select * from kb_document where base_id=? order by created_at desc,id limit ? offset ?",
                documentMapper(), baseId, limit, offset);
    }
    public Document requireDocument(UUID id, boolean lock) {
        return first(jdbc.query("select * from kb_document where id=?" + (lock ? " for update" : ""), documentMapper(), id));
    }
    public Optional<Document> documentByHash(UUID baseId, String hash) {
        return jdbc.query("select * from kb_document where base_id=? and sha256=?", documentMapper(), baseId, hash).stream().findFirst();
    }
    public Document insertDocument(UUID id, UUID baseId, String filename, String hash, int pages,
                                   List<String> warnings, List<KnowledgeText.Chunk> chunks, long userId) {
        jdbc.update("""
                insert into kb_document(id,base_id,filename,sha256,page_count,chunk_count,warnings,uploaded_by)
                values (?,?,?,?,?,?,cast(? as jsonb),?)
                """, id, baseId, filename, hash, pages, chunks.size(), encode(warnings), userId);
        List<Object[]> batch = chunks.stream().map(c -> new Object[]{id, c.page(), c.ordinal(), c.text(), c.searchText()}).toList();
        jdbc.batchUpdate("insert into kb_chunk(document_id,page,ordinal,text,search_vector) values (?,?,?,?,to_tsvector('simple',?))", batch);
        return requireDocument(id, false);
    }
    public Document setPublished(UUID id, boolean published) {
        jdbc.update("update kb_document set status=? where id=?", (published ? DocumentStatus.PUBLISHED : DocumentStatus.REVIEW).name(), id);
        return requireDocument(id, false);
    }
    public void deleteDocument(UUID id) { jdbc.update("delete from kb_document where id=?", id); }
    public List<ChunkView> chunks(UUID id, int offset, int limit) {
        return jdbc.query("select * from kb_chunk where document_id=? order by ordinal limit ? offset ?",
                (rs, row) -> new ChunkView(rs.getLong("id"), uuid(rs, "document_id"), rs.getInt("page"),
                        rs.getInt("ordinal"), rs.getString("text")), id, limit, offset);
    }

    public List<Agent> agents() { return jdbc.query(AGENT_SELECT + " order by a.created_at desc,a.id", agentMapper()); }
    public List<PublicAgent> availableAgents() {
        return jdbc.query("""
                select a.id,a.name,a.description from kb_agent a join exam e on e.id=a.exam_id
                where a.enabled and e.status='ACTIVE' order by a.created_at,a.id
                """, (rs, row) -> new PublicAgent(uuid(rs, "id"), rs.getString("name"), rs.getString("description")));
    }
    public Agent requireAgent(UUID id, boolean enabled) {
        return first(jdbc.query(AGENT_SELECT + " where a.id=?" + (enabled
                ? " and a.enabled and exists(select 1 from exam e where e.id=a.exam_id and e.status='ACTIVE')" : ""), agentMapper(), id));
    }
    public void lockAgent(UUID id) {
        first(jdbc.query("select id from kb_agent where id=? for update", (rs, row) -> uuid(rs, "id"), id));
    }
    public Agent insertAgent(long examId, AgentInput input) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into kb_agent(id,exam_id,name,description,instructions,enabled) values (?,?,?,?,?,false)",
                id, examId, input.name(), input.description(), input.instructions());
        replaceBases(id, input.baseIds());
        return requireAgent(id, false);
    }
    public Agent updateAgent(UUID id, long examId, AgentInput input) {
        jdbc.update("update kb_agent set exam_id=?,name=?,description=?,instructions=?,enabled=? where id=?",
                examId, input.name(), input.description(), input.instructions(), input.enabled(), id);
        replaceBases(id, input.baseIds());
        return requireAgent(id, false);
    }
    public void deleteAgent(UUID id) { jdbc.update("delete from kb_agent where id=?", id); }
    private void replaceBases(UUID id, List<UUID> bases) {
        jdbc.update("delete from kb_agent_base where agent_id=?", id);
        jdbc.batchUpdate("insert into kb_agent_base(agent_id,base_id) values (?,?)",
                bases.stream().map(base -> new Object[]{id, base}).toList());
    }

    public List<Citation> search(UUID agentId, String question) {
        List<String> terms = KnowledgeText.terms(question).stream().filter(t -> !STOP.contains(t)).limit(64).toList();
        if (terms.isEmpty()) return List.of();
        String query = String.join(" | ", terms);
        List<Citation> candidates = jdbc.query("""
                select c.id,c.document_id,c.page,c.text,d.filename
                from kb_chunk c join kb_document d on d.id=c.document_id
                join kb_agent_base ab on ab.base_id=d.base_id
                join kb_agent a on a.id=ab.agent_id join exam e on e.id=a.exam_id
                cross join (select to_tsquery('simple',?) as value) q
                where a.id=? and a.enabled and e.status='ACTIVE' and d.status='PUBLISHED'
                  and c.search_vector @@ q.value
                order by ts_rank_cd(c.search_vector,q.value) desc,c.id limit 24
                """, citationMapper(), query, agentId);
        List<Citation> result = new ArrayList<>();
        int required = Math.max(1, (int) Math.ceil(terms.size() * 0.25));
        for (Citation candidate : candidates) {
            Set<String> found = new HashSet<>(KnowledgeText.terms(candidate.text()));
            if (terms.stream().filter(found::contains).count() < required) continue;
            result.add(new Citation(result.size() + 1, candidate.documentId(), candidate.chunkId(),
                    candidate.filename(), candidate.page(), candidate.text()));
            if (result.size() == 6) break;
        }
        return List.copyOf(result);
    }

    public Citation source(UUID agentId, long chunkId) {
        return first(jdbc.query("""
                select c.id,c.document_id,c.page,c.text,d.filename from kb_chunk c
                join kb_document d on d.id=c.document_id join kb_agent_base ab on ab.base_id=d.base_id
                join kb_agent a on a.id=ab.agent_id join exam e on e.id=a.exam_id
                where a.id=? and c.id=? and a.enabled and e.status='ACTIVE' and d.status='PUBLISHED'
                """, citationMapper(), agentId, chunkId));
    }

    /** Called inside the short final transaction; edits wait until the answer commits. */
    public void lockCurrentEvidence(UUID agentId, List<Citation> sources) {
        first(jdbc.query("""
                select a.id from kb_agent a join exam e on e.id=a.exam_id
                where a.id=? and a.enabled and e.status='ACTIVE' for share of a,e
                """, (rs, row) -> uuid(rs, "id"), agentId));
        for (UUID documentId : sources.stream().map(Citation::documentId).distinct().sorted().toList()) {
            var ids = jdbc.query("""
                    select d.id from kb_document d join kb_agent_base ab on ab.base_id=d.base_id
                    where ab.agent_id=? and d.id=? and d.status='PUBLISHED' for share of d
                    """, (rs, row) -> uuid(rs, "id"), agentId, documentId);
            if (ids.isEmpty()) throw KnowledgeException.conflict("资料或智能体配置已变更，请重新提问");
        }
    }

    String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("资料序列化失败", e); }
    }
    private RowMapper<Base> baseMapper() {
        return (rs, row) -> new Base(uuid(rs, "id"), rs.getLong("exam_id"), rs.getString("name"), rs.getString("description"));
    }
    private RowMapper<Document> documentMapper() {
        return (rs, row) -> new Document(uuid(rs, "id"), uuid(rs, "base_id"), rs.getString("filename"),
                rs.getString("sha256"), rs.getInt("page_count"), rs.getInt("chunk_count"),
                DocumentStatus.valueOf(rs.getString("status")), warnings(rs.getString("warnings")),
                rs.getObject("created_at", OffsetDateTime.class));
    }
    private List<String> warnings(String value) {
        try { return json.readValue(value, new TypeReference<List<String>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("资料记录无法读取", e); }
    }
    private RowMapper<Agent> agentMapper() {
        return (rs, row) -> new Agent(uuid(rs, "id"), rs.getString("name"), rs.getString("description"),
                rs.getString("instructions"), rs.getBoolean("enabled"),
                Arrays.stream((Object[]) rs.getArray("base_ids").getArray()).map(v -> UUID.fromString(v.toString())).toList());
    }
    private RowMapper<Citation> citationMapper() {
        return (rs, row) -> new Citation(row + 1, uuid(rs, "document_id"), rs.getLong("id"),
                rs.getString("filename"), rs.getInt("page"), rs.getString("text"));
    }
    private static UUID uuid(ResultSet rs, String key) throws SQLException { return rs.getObject(key, UUID.class); }
    static <T> T first(List<T> rows) { return rows.stream().findFirst().orElseThrow(KnowledgeException::missing); }
}
