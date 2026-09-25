package com.longscoop.ruankao.api;

import com.longscoop.ruankao.ai.AiQuotaExceededException;
import com.longscoop.ruankao.ai.knowledge.KnowledgeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = {AdminKnowledgeController.class, KnowledgeAgentController.class})
public class KnowledgeApiAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeApiAdvice.class);
    @ExceptionHandler(KnowledgeException.class) public ResponseEntity<Map<String, String>> knowledge(KnowledgeException e) {
        return error(e.status(), e.getMessage());
    }
    @ExceptionHandler(AiQuotaExceededException.class) public ResponseEntity<Map<String, String>> quota() {
        return error(429, "今日 AI 请求额度已用完，请明天再试");
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return error(400, e.getMessage());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> malformed() { return error(400, "请求格式或参数无效"); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<Map<String, String>> size() {
        return error(413, "文件不能超过 20 MiB");
    }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<Map<String, String>> conflict() {
        return error(409, "数据已变更或仍被其他记录使用，请刷新后重试");
    }
    @ExceptionHandler(RuntimeException.class) public ResponseEntity<Map<String, String>> unavailable(RuntimeException e) {
        String reference = UUID.randomUUID().toString();
        LOG.warn("Knowledge operation failed reference={} type={}", reference, e.getClass().getSimpleName());
        return ResponseEntity.status(503).body(Map.of("message", "资料或 AI 服务暂不可用，请检查服务配置后重试", "reference", reference));
    }
    private ResponseEntity<Map<String, String>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
