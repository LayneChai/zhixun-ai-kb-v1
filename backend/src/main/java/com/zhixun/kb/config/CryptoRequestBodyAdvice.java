package com.zhixun.kb.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import com.zhixun.kb.common.annotation.CryptoContext;
import com.zhixun.kb.common.annotation.Encrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@RestControllerAdvice
@RequiredArgsConstructor
public class CryptoRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final CryptoProperties cryptoProperties;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        Encrypt encrypt = methodParameter.getMethodAnnotation(Encrypt.class);
        if (encrypt == null) {
            encrypt = methodParameter.getDeclaringClass().getAnnotation(Encrypt.class);
        }
        return encrypt != null && encrypt.in();
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String encryptedAesKey = inputMessage.getHeaders().getFirst("X-Encrypt-Key");
        if (encryptedAesKey == null || encryptedAesKey.isEmpty()) {
            throw new RuntimeException("X-Encrypt-Key Header is missing");
        }

        try {
            // RSA Decrypt the AES key
            RSA rsa = new RSA(cryptoProperties.getRsaPrivateKey(), null);
            String aesKey = new String(rsa.decrypt(encryptedAesKey, KeyType.PrivateKey), StandardCharsets.UTF_8);

            // Save AES Key to ThreadLocal context for Response Encryption later
            CryptoContext.setAesKey(aesKey);

            // Read the encrypted request body
            String encryptedBodyString = IoUtil.read(inputMessage.getBody(), StandardCharsets.UTF_8);

            // Return empty input message if body is missing
            if (encryptedBodyString == null || encryptedBodyString.trim().isEmpty()) {
                return inputMessage;
            }

            // AES Decrypt the body
            AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
            String decryptedBodyString = new String(aes.decrypt(encryptedBodyString), StandardCharsets.UTF_8);

            return new HttpInputMessage() {
                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }

                @Override
                public InputStream getBody() {
                    return new ByteArrayInputStream(decryptedBodyString.getBytes(StandardCharsets.UTF_8));
                }
            };
        } catch (Exception e) {
            CryptoContext.clear();
            throw new RuntimeException("Body Decryption Failed", e);
        }
    }
}
