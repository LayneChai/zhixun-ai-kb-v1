package com.zhixun.kb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.config.LlmProperties;
import com.zhixun.kb.entity.LlmConfig;
import com.zhixun.kb.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmProperties llmProperties;
    private final LlmConfigMapper llmConfigMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String question, String context) {
        try {
            ResolvedConfig cfg = resolveConfig();
            String endpoint = cfg.baseUrl.replaceAll("/$", "") + "/chat/completions";

            Map<String, Object> body = buildBody(question, context, false, cfg.model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return "模型返回为空，请稍后重试";
            }
            return contentNode.asText();
        } catch (Exception e) {
            return "模型调用失败：" + e.getMessage();
        }
    }

    public String chatStream(String question, String context, Consumer<String> onToken) {
        StringBuilder acc = new StringBuilder();
        try {
            ResolvedConfig cfg = resolveConfig();
            String endpoint = cfg.baseUrl.replaceAll("/$", "") + "/chat/completions";
            Map<String, Object> body = buildBody(question, context, true, cfg.model);

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + cfg.apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.io.InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:"))
                        continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data))
                        break;
                    if (data.isEmpty())
                        continue;

                    JsonNode node = objectMapper.readTree(data);
                    JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                    if (!delta.isMissingNode() && !delta.isNull()) {
                        String token = delta.asText();
                        acc.append(token);
                        onToken.accept(token);
                    }
                }
            }
            return acc.toString();
        } catch (Exception e) {
            String msg = "模型流式调用失败：" + e.getMessage();
            onToken.accept(msg);
            return acc.isEmpty() ? msg : acc.toString();
        }
    }

    private Map<String, Object> buildBody(String question, String context, boolean stream, String model) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("temperature", 0.7); // 提高一点随机性，适合日常对话

        String systemPrompt = "你是企业知识库问答助手。";
        if ("暂无知识片段".equals(context)) {
            systemPrompt += "目前知识库中没有找到相关内容。请作为通用的AI助手，以友好、专业的语气回答用户的问题。";
        } else {
            systemPrompt += "请优先基于给定知识片段进行回答。如果知识片段中没有涵盖问题的全部信息，你可以结合通用知识进行补充，但请明确说明哪些部分是基于知识库的。";
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "【知识片段】\n" + context + "\n\n【问题】\n" + question));
        body.put("messages", messages);
        return body;
    }

    private ResolvedConfig resolveConfig() {
        LlmConfig cfg = llmConfigMapper.selectOne(new LambdaQueryWrapper<LlmConfig>()
                .eq(LlmConfig::getEnabled, 1)
                .eq(LlmConfig::getIsDefault, 1)
                .last("limit 1"));
        if (cfg != null && cfg.getBaseUrl() != null && cfg.getApiKey() != null && cfg.getModel() != null) {
            return new ResolvedConfig(cfg.getBaseUrl(), cfg.getApiKey(), cfg.getModel());
        }
        return new ResolvedConfig(llmProperties.getBaseUrl(), llmProperties.getApiKey(), llmProperties.getModel());
    }

    private record ResolvedConfig(String baseUrl, String apiKey, String model) {
    }
}
