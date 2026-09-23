package com.longscoop.ruankao.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WechatApiIdentityProvider implements WechatIdentityProvider {

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;
    private final String apiBaseUrl;

    public WechatApiIdentityProvider(
            RestClient.Builder restClientBuilder,
            @Value("${WECHAT_APP_ID:}") String appId,
            @Value("${WECHAT_APP_SECRET:}") String appSecret,
            @Value("${WECHAT_API_BASE_URL:https://api.weixin.qq.com}") String apiBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.appId = appId;
        this.appSecret = appSecret;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public WechatIdentity exchangeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("wechat code is required");
        }
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new IllegalStateException("WECHAT_APP_ID and WECHAT_APP_SECRET must be configured");
        }

        WechatCodeResponse response = restClient.get()
                .uri(apiBaseUrl + "/sns/jscode2session"
                                + "?appid={appid}&secret={secret}&js_code={code}"
                                + "&grant_type=authorization_code",
                        appId, appSecret, code.trim())
                .retrieve()
                .body(WechatCodeResponse.class);

        if (response == null) {
            throw new IllegalStateException("empty response from WeChat");
        }
        if (response.errCode() != null && response.errCode() != 0) {
            throw new IllegalArgumentException(
                    "WeChat login failed: " + (response.errMsg() == null ? response.errCode() : response.errMsg()));
        }
        return new WechatIdentity(response.openId(), response.unionId());
    }

    record WechatCodeResponse(
            @JsonProperty("openid") String openId,
            @JsonProperty("unionid") String unionId,
            @JsonProperty("errcode") Integer errCode,
            @JsonProperty("errmsg") String errMsg) {
    }
}
