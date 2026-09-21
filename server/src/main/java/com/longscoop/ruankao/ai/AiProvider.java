package com.longscoop.ruankao.ai;

public interface AiProvider {

    AiProviderResponse complete(AiProviderRequest request);

    String providerName();

    String modelName();
}
