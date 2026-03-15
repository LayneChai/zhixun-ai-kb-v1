package com.zhixun.kb.config;

import cn.hutool.crypto.symmetric.AES;
import cn.hutool.json.JSONUtil;
import com.zhixun.kb.common.annotation.CryptoContext;
import com.zhixun.kb.common.annotation.Encrypt;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;

@RestControllerAdvice
public class CryptoResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Encrypt encrypt = returnType.getMethodAnnotation(Encrypt.class);
        if (encrypt == null) {
            encrypt = returnType.getDeclaringClass().getAnnotation(Encrypt.class);
        }
        return encrypt != null && encrypt.out();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        String aesKey = CryptoContext.getAesKey();
        try {
            if (aesKey != null && body != null) {
                String plainText = body instanceof String ? (String) body : JSONUtil.toJsonStr(body);
                AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
                return aes.encryptHex(plainText);
            }
            return body;
        } catch (Exception e) {
            throw new RuntimeException("Response Encryption Failed", e);
        } finally {
            CryptoContext.clear();
        }
    }
}
