package com.longscoop.ruankao.auth;

public record RuankaoPrincipal(long userId) {

    public RuankaoPrincipal {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
