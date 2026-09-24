package com.longscoop.ruankao.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatApiIdentityProviderTest {

    @Test
    void acceptsWechatJsonReturnedAsTextPlain() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(wechatUrl("wx-code")))
                .andRespond(withSuccess("{\"openid\":\"openid-001\",\"unionid\":\"union-001\"}", MediaType.TEXT_PLAIN));

        WechatIdentity identity = provider(builder).exchangeCode("wx-code");

        assertEquals("openid-001", identity.openId());
        assertEquals("union-001", identity.unionId());
        server.verify();
    }

    @Test
    void reportsWechatErrorFromTextPlainJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(wechatUrl("invalid-code")))
                .andRespond(withSuccess("{\"errcode\":40029,\"errmsg\":\"invalid code\"}", MediaType.TEXT_PLAIN));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> provider(builder).exchangeCode("invalid-code"));

        assertEquals("WeChat login failed: invalid code", error.getMessage());
        server.verify();
    }

    private WechatApiIdentityProvider provider(RestClient.Builder builder) {
        return new WechatApiIdentityProvider(builder, new ObjectMapper(), "app-id", "secret-value", "https://api.weixin.qq.com");
    }

    private String wechatUrl(String code) {
        return "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=app-id&secret=secret-value&js_code=" + code
                + "&grant_type=authorization_code";
    }
}
