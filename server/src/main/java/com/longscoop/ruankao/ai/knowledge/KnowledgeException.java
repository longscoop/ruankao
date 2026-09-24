package com.longscoop.ruankao.ai.knowledge;

public class KnowledgeException extends RuntimeException {
    private final int status;
    public KnowledgeException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int status() { return status; }
    public static KnowledgeException missing() { return new KnowledgeException(404, "资源不存在或当前不可用"); }
    public static KnowledgeException conflict(String message) { return new KnowledgeException(409, message); }
}
