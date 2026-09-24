package com.longscoop.ruankao.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Component
public class OpenAiCompatibleAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String providerName;
    private final String model;

    public OpenAiCompatibleAiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${AI_BASE_URL:}") String baseUrl,
            @Value("${AI_API_KEY:}") String apiKey,
            @Value("${AI_PROVIDER_NAME:openai-compatible}") String providerName,
            @Value("${AI_MODEL:qwen-plus}") String model) {
        this.restClient = AiHttpClientFactory.create(restClientBuilder, Duration.ofSeconds(10), Duration.ofSeconds(45));
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.providerName = providerName == null || providerName.isBlank()
                ? "openai-compatible"
                : providerName.trim();
        this.model = model == null || model.isBlank() ? "qwen-plus" : model.trim();
    }

    @Override
    public AiProviderResponse complete(AiProviderRequest request) {
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI_BASE_URL and AI_API_KEY must be configured");
        }

        ChatResponse response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(new ChatRequest(
                        model,
                        List.of(
                                new Message("system", request.systemPrompt()),
                                new Message("user", request.userPrompt())),
                        0.2))
                .retrieve()
                .body(ChatResponse.class);

        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new IllegalStateException("AI provider returned no completion");
        }
        Usage usage = response.usage();
        return new AiProviderResponse(
                providerName,
                model,
                response.choices().get(0).message().content(),
                usage == null || usage.promptTokens() == null ? 0 : usage.promptTokens(),
                usage == null || usage.completionTokens() == null ? 0 : usage.completionTokens());
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public String modelName() {
        return model;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    record ChatRequest(String model, List<Message> messages, double temperature) {
    }

    record Message(String role, String content) {
    }

    record ChatResponse(List<Choice> choices, Usage usage) {
    }

    record Choice(Message message) {
    }

    record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens) {
    }
}
