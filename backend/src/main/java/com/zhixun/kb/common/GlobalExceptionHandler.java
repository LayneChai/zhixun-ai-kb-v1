package com.zhixun.kb.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<String> handleNotLoginException(NotLoginException nle, HttpServletResponse response) {
        log.error("NotLoginException: {}", nle.getMessage());
        response.setStatus(401);
        return ApiResponse.fail("请先登录: " + nle.getMessage());
    }

    @ExceptionHandler({ NotRoleException.class, NotPermissionException.class })
    public ApiResponse<String> handleForbiddenException(SaTokenException e, HttpServletResponse response) {
        log.error("Forbidden: {}", e.getMessage());
        response.setStatus(403);
        return ApiResponse.fail("无权访问: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception e, HttpServletResponse response) {
        log.error("Global Exception: ", e);
        response.setStatus(500);
        return ApiResponse.fail("系统解析错误: " + e.getMessage());
    }
}
