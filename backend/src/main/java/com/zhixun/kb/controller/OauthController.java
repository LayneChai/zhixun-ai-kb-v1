package com.zhixun.kb.controller;

import cn.dev33.satoken.stp.StpUtil;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDingTalkRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/oauth")
public class OauthController {

    @Value("${justauth.type.wechat_open.client-id}")
    private String wechatClientId;

    @Value("${justauth.type.wechat_open.client-secret}")
    private String wechatClientSecret;

    @Value("${justauth.type.wechat_open.redirect-uri}")
    private String wechatRedirectUri;

    @Value("${justauth.type.dingtalk.client-id}")
    private String dingtalkClientId;

    @Value("${justauth.type.dingtalk.client-secret}")
    private String dingtalkClientSecret;

    @Value("${justauth.type.dingtalk.redirect-uri}")
    private String dingtalkRedirectUri;

    private AuthRequest getAuthRequest(String source) {
        if ("wechat_open".equalsIgnoreCase(source)) {
            return new AuthWeChatOpenRequest(AuthConfig.builder()
                    .clientId(wechatClientId)
                    .clientSecret(wechatClientSecret)
                    .redirectUri(wechatRedirectUri)
                    .build());
        } else if ("dingtalk".equalsIgnoreCase(source)) {
            return new AuthDingTalkRequest(AuthConfig.builder()
                    .clientId(dingtalkClientId)
                    .clientSecret(dingtalkClientSecret)
                    .redirectUri(dingtalkRedirectUri)
                    .build());
        }
        throw new IllegalArgumentException("Unsupported OAuth source: " + source);
    }

    @GetMapping("/render/{source}")
    public void renderAuth(@PathVariable("source") String source, HttpServletResponse response) throws IOException {
        AuthRequest authRequest = getAuthRequest(source);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        response.sendRedirect(authorizeUrl);
    }

    @GetMapping("/callback/{source}")
    public Object login(@PathVariable("source") String source, AuthCallback callback) {
        AuthRequest authRequest = getAuthRequest(source);
        @SuppressWarnings("unchecked")
        AuthResponse<AuthUser> authResponse = authRequest.login(callback);

        if (authResponse.ok()) {
            AuthUser authUser = authResponse.getData();
            // Map third-party authUser.getUuid() to local user
            long mappedUserId = 100L;

            StpUtil.login(mappedUserId);
            return "Third-party login success! Token: " + StpUtil.getTokenValue();
        }

        return "Login failed: " + authResponse.getMsg();
    }
}
