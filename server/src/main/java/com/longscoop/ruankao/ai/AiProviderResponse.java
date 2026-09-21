package com.longscoop.ruankao.ai;

public record AiProviderResponse(
        String provider,
        String model,
        String content,
        int promptTokens,
        int completionTokens) {

    public AiProviderResponse {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("AI response content is required");
        }
        provider = provider == null || provider.isBlank() ? "unknown" : provider.trim();
        model = model == null || model.isBlank() ? "unknown" : model.trim();
        content = content.trim();
        promptTokens = Math.max(0, promptTokens);
        completionTokens = Math.max(0, completionTokens);
    }
}
