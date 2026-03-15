package com.zhixun.kb.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.config.AppProperties;
import com.zhixun.kb.dto.AskRequest;
import com.zhixun.kb.dto.RenameSessionRequest;
import com.zhixun.kb.entity.KbDataset;
import com.zhixun.kb.entity.QaMessage;
import com.zhixun.kb.entity.QaSession;
import com.zhixun.kb.mapper.KbDatasetMapper;
import com.zhixun.kb.mapper.QaMessageMapper;
import com.zhixun.kb.mapper.QaSessionMapper;
import com.zhixun.kb.service.ChunkSearchService;
import com.zhixun.kb.service.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {
    private final QaSessionMapper sessionMapper;
    private final QaMessageMapper messageMapper;
    private final KbDatasetMapper datasetMapper;
    private final LlmService llmService;
    private final ChunkSearchService chunkSearchService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/session")
    public ApiResponse<QaSession> createSession(@RequestBody QaSession session) {
        session.setUserId(currentUserId());
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return ApiResponse.ok(session);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<QaSession>> sessions(@RequestParam(required = false) Long datasetId) {
        LambdaQueryWrapper<QaSession> query = new LambdaQueryWrapper<QaSession>()
                .eq(QaSession::getUserId, currentUserId())
                .orderByDesc(QaSession::getCreatedAt);
        if (datasetId != null) {
            query.eq(QaSession::getDatasetId, datasetId);
        }
        return ApiResponse.ok(sessionMapper.selectList(query));
    }

    @PostMapping("/ask")
    public ApiResponse<Map<String, Object>> ask(@RequestBody @Valid AskRequest request) {
        long start = System.currentTimeMillis();
        QaSession session = requireOwnedSession(request.getSessionId());
        if (session == null) {
            return ApiResponse.fail("会话不存在");
        }

        int topK = appProperties.getBm25().getTopK();
        List<Map<String, Object>> refsList = chunkSearchService.search(
                request.getQuestion(), session.getDatasetId(), topK);

        String context = refsList.stream()
                .map(x -> String.valueOf(x.get("snippet")))
                .collect(Collectors.joining("\n\n"));
        if (context.isBlank()) {
            context = "暂无知识片段";
        }

        String answer = llmService.chat(request.getQuestion(), context);
        String refsJson = toJson(refsList);

        QaMessage msg = saveMessage(request.getSessionId(), request.getQuestion(), answer, refsJson, start);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", msg.getId());
        payload.put("answer", answer);
        payload.put("references", refsList);
        return ApiResponse.ok(payload);
    }

    @GetMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestParam Long sessionId, @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(60_000L);
        long start = System.currentTimeMillis();
        QaSession session = requireOwnedSession(sessionId);

        new Thread(() -> {
            try {
                if (session == null) {
                    emitter.send(SseEmitter.event().name("error").data("会话不存在"));
                    emitter.complete();
                    return;
                }

                int topK = appProperties.getBm25().getTopK();
                List<Map<String, Object>> refsList = chunkSearchService.search(question, session.getDatasetId(), topK);
                String context = refsList.stream()
                        .map(x -> String.valueOf(x.get("snippet")))
                        .collect(Collectors.joining("\n\n"));
                if (context.isBlank()) {
                    context = "暂无知识片段";
                }

                String refsJson = toJson(refsList);
                String answer = llmService.chatStream(question, context, token -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(token));
                    } catch (IOException ioException) {
                        throw new RuntimeException(ioException);
                    }
                });

                QaMessage msg = saveMessage(sessionId, question, answer, refsJson, start);
                emitter.send(SseEmitter.event().name("refs").data(refsJson));
                emitter.send(SseEmitter.event().name("done").data(msg.getId()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @GetMapping("/history")
    public ApiResponse<List<QaMessage>> history(@RequestParam Long sessionId) {
        if (requireOwnedSession(sessionId) == null) {
            return ApiResponse.fail("会话不存在");
        }
        return ApiResponse.ok(messageMapper.selectList(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getSessionId, sessionId)
                .orderByAsc(QaMessage::getCreatedAt)));
    }

    @PostMapping("/session/rename")
    public ApiResponse<String> rename(@RequestBody @Valid RenameSessionRequest req) {
        QaSession session = requireOwnedSession(req.getSessionId());
        if (session == null) {
            return ApiResponse.fail("会话不存在");
        }
        session.setTitle(req.getTitle());
        sessionMapper.updateById(session);
        return ApiResponse.ok("renamed");
    }

    @PostMapping("/session/{sessionId}/delete")
    public ApiResponse<String> deleteSession(@PathVariable Long sessionId) {
        QaSession session = requireOwnedSession(sessionId);
        if (session == null) {
            return ApiResponse.fail("会话不存在或无权限删除");
        }
        // 删除会话及其关联的所有消息
        messageMapper.delete(new LambdaQueryWrapper<QaMessage>().eq(QaMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
        return ApiResponse.ok("deleted");
    }

    @GetMapping("/source/{messageId}")
    public ApiResponse<String> source(@PathVariable Long messageId) {
        QaMessage msg = messageMapper.selectById(messageId);
        if (msg == null || requireOwnedSession(msg.getSessionId()) == null) {
            return ApiResponse.fail("消息不存在");
        }
        return ApiResponse.ok(msg.getReferencesJson());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private QaSession requireOwnedSession(Long sessionId) {
        QaSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }

        // Default dataset fallback
        if (session.getDatasetId() == null || session.getDatasetId() == 0) {
            session.setDatasetId(getDefaultDatasetId());
        }

        return Objects.equals(session.getUserId(), currentUserId()) ? session : null;
    }

    private Long getDefaultDatasetId() {
        KbDataset ds = datasetMapper.selectOne(new LambdaQueryWrapper<KbDataset>()
                .eq(KbDataset::getIsDefault, 1)
                .last("limit 1"));
        return ds != null ? ds.getId() : null;
    }

    private QaMessage saveMessage(Long sessionId, String question, String answer, String refs, long start) {
        QaMessage msg = new QaMessage();
        msg.setSessionId(sessionId);
        msg.setQuestion(question);
        msg.setAnswer(answer);
        msg.setReferencesJson(refs);
        msg.setLatencyMs(System.currentTimeMillis() - start);
        msg.setTokenCount(answer.length());
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }
}
