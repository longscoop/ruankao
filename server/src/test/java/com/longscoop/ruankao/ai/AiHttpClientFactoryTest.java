package com.longscoop.ruankao.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class AiHttpClientFactoryTest {
    @Test void unresponsiveProviderHasABoundedReadTimeout() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/slow", exchange -> {
            entered.countDown();
            try { release.await(3, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var client = AiHttpClientFactory.create(RestClient.builder(), Duration.ofSeconds(1), Duration.ofMillis(500));
            assertThatThrownBy(() -> client.get().uri("http://127.0.0.1:" + server.getAddress().getPort() + "/slow")
                    .retrieve().toBodilessEntity()).isInstanceOf(ResourceAccessException.class);
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        } finally { release.countDown(); server.stop(0); }
    }

    @Test void zeroOrNegativeTimeoutsCannotDisableTheBounds() {
        assertThatThrownBy(() -> AiHttpClientFactory.create(RestClient.builder(), Duration.ZERO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiHttpClientFactory.create(RestClient.builder(), Duration.ofSeconds(1), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void configuredProviderStillSendsTheProtocolAndReadsUsage() throws Exception {
        var received = new AtomicReference<String>();
        var authorization = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"解释[1]\"}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var provider = new OpenAiCompatibleAiProvider(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/", "test-key", "test", "test-model");
            var response = provider.complete(new AiProviderRequest("引用规则", "数据库事务"));
            assertThat(response.content()).isEqualTo("解释[1]");
            assertThat(response.promptTokens()).isEqualTo(12);
            assertThat(response.completionTokens()).isEqualTo(4);
            assertThat(received.get()).contains("test-model", "引用规则", "数据库事务");
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
        } finally { server.stop(0); }
    }
}
