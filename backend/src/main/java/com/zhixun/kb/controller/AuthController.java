package com.zhixun.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.dto.LoginRequest;
import com.zhixun.kb.entity.SysUser;
import com.zhixun.kb.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaIgnore;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;

    @PostMapping("/login")
    @SaIgnore
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getPassword, request.getPassword())
                .last("limit 1"));
        if (user == null)
            return ApiResponse.fail("用户名或密码错误");

        // 登录并生成 token
        StpUtil.login(user.getId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", StpUtil.getTokenValue());
        resp.put("user", user);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        StpUtil.logout();
        return ApiResponse.ok("ok");
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> me() {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(sysUserMapper.selectById(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<String> updateProfile(@RequestBody Map<String, String> payload) {
        long userId = StpUtil.getLoginIdAsLong();
        String newUsername = payload.get("username");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ApiResponse.fail("用户名不能为空");
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(newUsername);

        try {
            sysUserMapper.updateById(user);
            return ApiResponse.ok("ok");
        } catch (Exception e) {
            return ApiResponse.fail("修改失败，用户名可能已存在");
        }
    }

    @PutMapping("/password")
    public ApiResponse<String> updatePassword(@RequestBody Map<String, String> payload) {
        long userId = StpUtil.getLoginIdAsLong();
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.trim().isEmpty()) {
            return ApiResponse.fail("密码不能为空");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !user.getPassword().equals(oldPassword)) {
            return ApiResponse.fail("原密码错误");
        }

        user.setPassword(newPassword);
        sysUserMapper.updateById(user);

        StpUtil.logout(); // 密码修改后强制下线当前会话
        return ApiResponse.ok("ok");
    }
}
