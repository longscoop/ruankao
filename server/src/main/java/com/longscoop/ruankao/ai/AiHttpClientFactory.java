package com.longscoop.ruankao.ai;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/** Builds a bounded transport so an unavailable AI provider cannot wait indefinitely. */
public final class AiHttpClientFactory {
    private AiHttpClientFactory() {}

    public static RestClient create(RestClient.Builder builder, Duration connectTimeout, Duration readTimeout) {
        Objects.requireNonNull(builder, "builder");
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("AI connection and read timeouts must be positive");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return builder.requestFactory(factory).build();
    }
}
