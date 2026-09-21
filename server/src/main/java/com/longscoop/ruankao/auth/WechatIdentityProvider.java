package com.longscoop.ruankao.auth;

public interface WechatIdentityProvider {

    WechatIdentity exchangeCode(String code);
}
