package com.longscoop.ruankao.ai;

public record AiProviderRequest(String systemPrompt, String userPrompt) {

    public AiProviderRequest {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("userPrompt is required");
        }
        systemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        userPrompt = userPrompt.trim();
    }
}
