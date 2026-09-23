package com.longscoop.ruankao.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.auth.persistence.AuthIdentityEntity;
import com.longscoop.ruankao.auth.persistence.AuthIdentityMapper;
import com.longscoop.ruankao.auth.persistence.UserAccountEntity;
import com.longscoop.ruankao.auth.persistence.UserAccountMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

@Service
public class AdminAuthService {

    private static final String PROVIDER_ADMIN_PASSWORD = "ADMIN_PASSWORD";
    private static final String ROLE_ADMIN = "ADMIN";

    private final AuthIdentityMapper identityMapper;
    private final UserAccountMapper userMapper;
    private final AuthSessionService sessionService;
    private final String configuredUsername;
    private final String configuredPassword;

    public AdminAuthService(
            AuthIdentityMapper identityMapper,
            UserAccountMapper userMapper,
            AuthSessionService sessionService,
            @Value("${ruankao.admin.username:}") String configuredUsername,
            @Value("${ruankao.admin.password:}") String configuredPassword) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.sessionService = sessionService;
        this.configuredUsername = configuredUsername == null ? "" : configuredUsername.trim();
        this.configuredPassword = configuredPassword == null ? "" : configuredPassword;
    }

    @Transactional
    public LoginResult login(String username, String password) {
        ensureConfigured();

        String normalizedUsername = username == null ? "" : username.trim();
        String candidatePassword = password == null ? "" : password;
        if (!secureEquals(configuredUsername, normalizedUsername)
                || !secureEquals(configuredPassword, candidatePassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        AuthIdentityEntity identity = identityMapper.selectOne(
                Wrappers.<AuthIdentityEntity>lambdaQuery()
                        .eq(AuthIdentityEntity::getProvider, PROVIDER_ADMIN_PASSWORD)
                        .eq(AuthIdentityEntity::getSubject, configuredUsername));

        UserAccountEntity user;
        OffsetDateTime now = OffsetDateTime.now();
        if (identity == null) {
            user = new UserAccountEntity();
            user.setDisplayName(configuredUsername);
            user.setRole(ROLE_ADMIN);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);

            identity = new AuthIdentityEntity();
            identity.setUserId(user.getId());
            identity.setProvider(PROVIDER_ADMIN_PASSWORD);
            identity.setSubject(configuredUsername);
            identity.setCreatedAt(now);
            identity.setUpdatedAt(now);
            identityMapper.insert(identity);
        } else {
            user = userMapper.selectById(identity.getUserId());
            if (user == null) {
                throw new IllegalStateException("Admin identity points to a missing user");
            }
            if (!ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                user.setRole(ROLE_ADMIN);
                user.setUpdatedAt(now);
                userMapper.updateById(user);
            }
            if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
                user.setDisplayName(configuredUsername);
                user.setUpdatedAt(now);
                userMapper.updateById(user);
            }
        }

        return new LoginResult(
                sessionService.create(user.getId()),
                profile(user, configuredUsername));
    }

    public AdminProfile getProfile(long userId) {
        UserAccountEntity user = userMapper.selectById(userId);
        if (user == null || !ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号不是管理员");
        }

        AuthIdentityEntity identity = identityMapper.selectOne(
                Wrappers.<AuthIdentityEntity>lambdaQuery()
                        .eq(AuthIdentityEntity::getUserId, userId)
                        .eq(AuthIdentityEntity::getProvider, PROVIDER_ADMIN_PASSWORD)
                        .last("limit 1"));
        String username = identity == null ? user.getDisplayName() : identity.getSubject();
        return profile(user, username);
    }

    @Transactional
    public void logout(String token) {
        sessionService.revoke(token);
    }

    private AdminProfile profile(UserAccountEntity user, String username) {
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? username
                : user.getDisplayName();
        return new AdminProfile(user.getId(), username, displayName, ROLE_ADMIN);
    }

    private void ensureConfigured() {
        if (configuredUsername.isBlank() || configuredPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "管理员登录尚未配置，请设置 ADMIN_USERNAME 和 ADMIN_PASSWORD");
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public record LoginResult(String token, AdminProfile admin) {
    }

    public record AdminProfile(long userId, String username, String displayName, String role) {
    }
}
