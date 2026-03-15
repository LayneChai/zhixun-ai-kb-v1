package com.zhixun.kb.config;

import cn.dev33.satoken.stp.StpUtil;
import com.zhixun.kb.entity.SysLog;
import com.zhixun.kb.mapper.SysLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {

    private final SysLogMapper sysLogMapper;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!shouldLog(request, response, ex)) {
            return;
        }

        SysLog log = new SysLog();
        log.setOperatorId(StpUtil.getLoginIdAsLong());
        log.setModule(resolveModule(request.getRequestURI()));
        log.setAction(request.getMethod() + " " + request.getRequestURI());
        log.setIp(resolveIp(request));
        log.setCreatedAt(LocalDateTime.now());
        sysLogMapper.insert(log);
    }

    private boolean shouldLog(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        String uri = request.getRequestURI();
        if (ex != null || response.getStatus() >= 400 || !uri.startsWith("/api/")) {
            return false;
        }
        if ("/api/system/logs".equals(uri) || uri.startsWith("/api/qa/ask/stream")) {
            return false;
        }
        return StpUtil.isLogin();
    }

    private String resolveModule(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 ? parts[2] : "system";
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
