package com.longscoop.ruankao.ai.knowledge;

import com.longscoop.ruankao.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@Service
public class KnowledgeChatService {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeChatService.class);
    private final KnowledgeRepository knowledge;
    private final KnowledgeConversationRepository conversations;
    private final AiService ai;

    public KnowledgeChatService(KnowledgeRepository knowledge, KnowledgeConversationRepository conversations, AiService ai) {
        this.knowledge = knowledge;
        this.conversations = conversations;
        this.ai = ai;
    }
    public Session start(long userId, UUID agentId) {
        knowledge.requireAgent(agentId, true);
        return conversations.start(userId, agentId);
    }
    public List<Session> sessions(long userId, UUID agentId, int offset, int limit) {
        KnowledgeService.page(offset, limit);
        return conversations.sessions(userId, agentId, offset, limit);
    }
    public List<Turn> history(long userId, UUID sessionId, int offset, int limit) {
        KnowledgeService.page(offset, limit);
        conversations.owned(userId, sessionId);
        return conversations.history(sessionId, offset, limit);
    }

    /** Network calls deliberately happen outside database transactions. */
    public Turn ask(long userId, UUID sessionId, UUID requestId, String message) {
        if (requestId == null) throw new IllegalArgumentException("requestId 不能为空");
        String question = KnowledgeService.text(message, 2000, true);
        Session session = conversations.owned(userId, sessionId);
        var cached = conversations.cached(sessionId, requestId);
        if (cached.isPresent()) return replay(cached.get(), question);
        knowledge.requireAgent(session.agentId(), true);
        UUID token = UUID.randomUUID();
        if (!conversations.acquire(userId, sessionId, token)) {
            throw KnowledgeException.conflict("该会话正在回答，请稍后重试同一请求");
        }
        try {
            cached = conversations.cached(sessionId, requestId);
            if (cached.isPresent()) return replay(cached.get(), question);
            if (conversations.turnCount(sessionId) >= 200) {
                throw KnowledgeException.conflict("该会话已达到 200 轮，请新建会话");
            }
            Agent agent = knowledge.requireAgent(session.agentId(), true);
            List<Turn> history = conversations.recent(sessionId);
            String query = question;
            if (!history.isEmpty() && question.length() < 120
                    && question.matches("(?s).*(它|这个|上述|继续|前面|两者|刚才).*$")) {
                query = history.get(history.size() - 1).question() + " " + question;
            }
            List<Citation> sources = knowledge.search(agent.id(), query);
            GroundedAnswer.Result answer;
            if (sources.isEmpty()) {
                answer = GroundedAnswer.compose(null, sources);
            } else {
                String context = knowledge.encode(Map.of("question", question,
                        "history", safeHistory(agent.id(), history), "sources", sources));
                String content = ai.groundedChat(userId, agent.instructions(), context).content();
                answer = GroundedAnswer.compose(content, sources);
            }
            return conversations.finish(userId, session, token, requestId, question, answer, sources);
        } finally {
            try { conversations.release(sessionId, token); }
            catch (RuntimeException e) {
                // Expiring/fenced leases recover after a database outage. Do not log prompts or provider secrets.
                LOG.warn("Knowledge lease release failed for session {}, type {}", sessionId, e.getClass().getSimpleName());
            }
        }
    }
    private Turn replay(Turn turn, String question) {
        if (!turn.question().equals(question)) throw KnowledgeException.conflict("requestId 已用于不同的问题，请生成新的请求标识");
        return turn;
    }
    private List<Map<String, String>> safeHistory(UUID agentId, List<Turn> turns) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Turn turn : turns) {
            boolean current = !turn.citations().isEmpty();
            for (Citation citation : turn.citations()) {
                try { knowledge.source(agentId, citation.chunkId()); }
                catch (KnowledgeException e) { current = false; break; }
            }
            String answer = current ? turn.content().substring(0, Math.min(1200, turn.content().length()))
                    : "历史回答不作为本轮证据，请仅使用当前 sources。";
            result.add(Map.of("question", turn.question(), "answer", answer));
        }
        return result;
    }
}
