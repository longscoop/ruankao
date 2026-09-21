package com.longscoop.ruankao.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.auth.persistence.AuthIdentityEntity;
import com.longscoop.ruankao.auth.persistence.AuthIdentityMapper;
import com.longscoop.ruankao.auth.persistence.UserAccountEntity;
import com.longscoop.ruankao.auth.persistence.UserAccountMapper;
import com.longscoop.ruankao.user.UserExamProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String PROVIDER_WECHAT = "WECHAT";

    private final WechatIdentityProvider wechatIdentityProvider;
    private final AuthIdentityMapper identityMapper;
    private final UserAccountMapper userMapper;
    private final AuthSessionService sessionService;
    private final UserExamProfileService profileService;

    public AuthService(
            WechatIdentityProvider wechatIdentityProvider,
            AuthIdentityMapper identityMapper,
            UserAccountMapper userMapper,
            AuthSessionService sessionService,
            UserExamProfileService profileService) {
        this.wechatIdentityProvider = wechatIdentityProvider;
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.sessionService = sessionService;
        this.profileService = profileService;
    }

    @Transactional
    public LoginResult loginWechat(String code) {
        WechatIdentity wechat = wechatIdentityProvider.exchangeCode(code);
        AuthIdentityEntity identity = identityMapper.selectOne(
                Wrappers.<AuthIdentityEntity>lambdaQuery()
                        .eq(AuthIdentityEntity::getProvider, PROVIDER_WECHAT)
                        .eq(AuthIdentityEntity::getSubject, wechat.openId()));

        long userId;
        if (identity == null) {
            UserAccountEntity user = new UserAccountEntity();
            userMapper.insert(user);
            userId = user.getId();

            identity = new AuthIdentityEntity();
            identity.setUserId(userId);
            identity.setProvider(PROVIDER_WECHAT);
            identity.setSubject(wechat.openId());
            identity.setUnionId(wechat.unionId());
            identityMapper.insert(identity);
        } else {
            userId = identity.getUserId();
            if (wechat.unionId() != null && !wechat.unionId().equals(identity.getUnionId())) {
                identity.setUnionId(wechat.unionId());
                identityMapper.updateById(identity);
            }
        }

        return new LoginResult(
                sessionService.create(userId),
                userId,
                profileService.countForUser(userId) > 0);
    }

    public record LoginResult(String token, long userId, boolean profileCompleted) {
    }
}
