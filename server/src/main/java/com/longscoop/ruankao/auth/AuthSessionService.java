package com.longscoop.ruankao.auth;

import com.longscoop.ruankao.auth.persistence.AuthSessionEntity;
import com.longscoop.ruankao.auth.persistence.AuthSessionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthSessionMapper sessionMapper;
    private final int sessionDays;

    public AuthSessionService(
            AuthSessionMapper sessionMapper,
            @Value("${AUTH_SESSION_DAYS:30}") int sessionDays) {
        this.sessionMapper = sessionMapper;
        this.sessionDays = Math.max(1, sessionDays);
    }

    @Transactional
    public String create(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OffsetDateTime now = OffsetDateTime.now();

        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(now.plusDays(sessionDays));
        entity.setLastUsedAt(now);
        entity.setCreatedAt(now);
        sessionMapper.insert(entity);
        return token;
    }

    @Transactional
    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hash(token.trim());
        OffsetDateTime now = OffsetDateTime.now();
        Long userId = sessionMapper.selectActiveUserId(tokenHash, now);
        if (userId == null) {
            return Optional.empty();
        }
        sessionMapper.touch(tokenHash, now);
        return Optional.of(userId);
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessionMapper.deleteByTokenHash(hash(token.trim()));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
