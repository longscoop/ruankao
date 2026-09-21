package com.longscoop.ruankao.auth;

public record WechatIdentity(String openId, String unionId) {

    public WechatIdentity {
        if (openId == null || openId.trim().isEmpty()) {
            throw new IllegalArgumentException("openId is required");
        }
        openId = openId.trim();
        unionId = unionId == null || unionId.trim().isEmpty() ? null : unionId.trim();
    }
}
