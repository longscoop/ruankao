package com.longscoop.ruankao.ai.knowledge;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final MaterialExtractor extractor;
    private final KnowledgeFileStore files;
    private final TransactionTemplate transaction;

    public KnowledgeService(KnowledgeRepository repository, MaterialExtractor extractor,
                            KnowledgeFileStore files, PlatformTransactionManager manager) {
        this.repository = repository;
        this.extractor = extractor;
        this.files = files;
        this.transaction = new TransactionTemplate(manager);
    }

    public List<Base> bases() { return repository.bases(); }
    public Base createBase(BaseInput input) {
        BaseInput checked = baseInput(input);
        return transaction.execute(status -> repository.insertBase(checked));
    }
    public Base updateBase(UUID id, BaseInput input) {
        BaseInput checked = baseInput(input);
        return transaction.execute(status -> {
            Base existing = repository.requireBase(id, true);
            if (existing.examId() != checked.examId()) throw new IllegalArgumentException("知识库所属考试不能修改");
            return repository.updateBase(id, checked);
        });
    }
    public void deleteBase(UUID id) {
        try {
            transaction.executeWithoutResult(status -> {
                repository.requireBase(id, true);
                repository.deleteBase(id);
            });
        } catch (DataIntegrityViolationException e) {
            throw KnowledgeException.conflict("请先移除知识库中的资料及智能体关联");
        }
    }
    private BaseInput baseInput(BaseInput input) {
        if (input == null) throw new IllegalArgumentException("知识库参数不能为空");
        repository.requireExam(input.examId());
        return new BaseInput(input.examId(), text(input.name(), 120, true), text(input.description(), 1000, false));
    }

    public Document upload(UUID baseId, String filename, byte[] data, long userId) {
        repository.requireBase(baseId, false);
        if (filename != null) {
            filename = filename.replace('\\', '/');
            filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        }
        var extracted = extractor.extract(filename, data);
        var chunks = KnowledgeText.split(extracted.pages());
        String hash = sha256(data);
        String safeName = filename;
        AtomicReference<UUID> written = new AtomicReference<>();
        try {
            return transaction.execute(status -> {
                repository.requireBase(baseId, true);
                var duplicate = repository.documentByHash(baseId, hash);
                if (duplicate.isPresent()) return duplicate.get();
                UUID id = UUID.randomUUID();
                files.put(id, data);
                written.set(id);
                return repository.insertDocument(id, baseId, safeName, hash, extracted.pageCount(),
                        extracted.warnings(), chunks, userId);
            });
        } catch (RuntimeException e) {
            if (written.get() != null) {
                try { files.delete(written.get()); } catch (RuntimeException cleanup) { e.addSuppressed(cleanup); }
            }
            throw e;
        }
    }
    public List<Document> documents(UUID baseId, int offset, int limit) {
        page(offset, limit);
        repository.requireBase(baseId, false);
        return repository.documents(baseId, offset, limit);
    }
    public Document document(UUID id) { return repository.requireDocument(id, false); }
    public List<ChunkView> chunks(UUID id, int offset, int limit) {
        page(offset, limit);
        repository.requireDocument(id, false);
        return repository.chunks(id, offset, limit);
    }
    public Document setPublished(UUID id, boolean published) {
        return transaction.execute(status -> {
            repository.requireDocument(id, true);
            return repository.setPublished(id, published);
        });
    }
    public byte[] original(UUID id) {
        repository.requireDocument(id, false);
        return files.read(id);
    }
    public void deleteDocument(UUID id) {
        transaction.executeWithoutResult(status -> {
            repository.requireDocument(id, true);
            repository.deleteDocument(id);
        });
        // Retrieval is revoked before physical deletion. A filesystem failure is surfaced, not hidden.
        files.delete(id);
    }

    public List<Agent> agents() { return repository.agents(); }
    public List<PublicAgent> availableAgents() { return repository.availableAgents(); }
    public Agent createAgent(AgentInput input) {
        AgentInput checked = agentInput(input);
        return transaction.execute(status -> repository.insertAgent(lockAndCheckBases(checked.baseIds()), checked));
    }
    public Agent updateAgent(UUID id, AgentInput input) {
        AgentInput checked = agentInput(input);
        return transaction.execute(status -> {
            repository.lockAgent(id);
            return repository.updateAgent(id, lockAndCheckBases(checked.baseIds()), checked);
        });
    }
    public void deleteAgent(UUID id) {
        try {
            transaction.executeWithoutResult(status -> {
                repository.lockAgent(id);
                repository.deleteAgent(id);
            });
        } catch (DataIntegrityViolationException e) {
            throw KnowledgeException.conflict("智能体已有学习会话，请停用而不是删除，以保留学习历史");
        }
    }
    public Citation source(UUID agentId, long chunkId) { return repository.source(agentId, chunkId); }
    private AgentInput agentInput(AgentInput input) {
        if (input == null || input.baseIds() == null || input.baseIds().isEmpty()
                || input.baseIds().size() > 10 || input.baseIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("智能体必须关联 1 至 10 个知识库");
        }
        return new AgentInput(text(input.name(), 120, true), text(input.description(), 1000, false),
                text(input.instructions(), 2000, false), input.baseIds().stream().distinct().sorted().toList(), input.enabled());
    }
    private long lockAndCheckBases(List<UUID> ids) {
        Long examId = null;
        for (UUID id : ids) {
            Base base = repository.requireBase(id, true);
            if (examId != null && examId.longValue() != base.examId()) {
                throw new IllegalArgumentException("一个智能体只能关联同一考试的知识库");
            }
            examId = base.examId();
        }
        return examId;
    }
    static String text(String value, int max, boolean required) {
        String result = value == null ? "" : value.trim();
        if ((required && result.isEmpty()) || result.length() > max || result.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("文本不能为空（必填项）且长度不能超过 " + max + " 字符");
        }
        return result;
    }
    static void page(int offset, int limit) {
        if (offset < 0 || offset > 100000 || limit < 1 || limit > 100) throw new IllegalArgumentException("分页参数无效");
    }
    private static String sha256(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
