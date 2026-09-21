package com.longscoop.ruankao.ai;

public class FakeAiProvider implements AiProvider {

    private final String content;

    public FakeAiProvider(String content) {
        this.content = content;
    }

    @Override
    public AiProviderResponse complete(AiProviderRequest request) {
        return new AiProviderResponse("fake", "fake-model", content, 0, 0);
    }

    @Override
    public String providerName() {
        return "fake";
    }

    @Override
    public String modelName() {
        return "fake-model";
    }
}
