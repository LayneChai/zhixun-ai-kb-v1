package com.zhixun.kb.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.entity.KbDataset;
import com.zhixun.kb.entity.KbDocument;
import com.zhixun.kb.entity.QaMessage;
import com.zhixun.kb.entity.SysLog;
import com.zhixun.kb.entity.SysUser;
import com.zhixun.kb.mapper.KbDatasetMapper;
import com.zhixun.kb.mapper.KbDocumentMapper;
import com.zhixun.kb.mapper.QaMessageMapper;
import com.zhixun.kb.mapper.SysLogMapper;
import com.zhixun.kb.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SysLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final KbDatasetMapper datasetMapper;
    private final KbDocumentMapper documentMapper;
    private final QaMessageMapper qaMessageMapper;

    @GetMapping("/logs")
    @SaCheckPermission("system:log:list")
    public ApiResponse<List<SysLog>> logs(@RequestParam(required = false) Long operatorId) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (operatorId != null) qw.eq(SysLog::getOperatorId, operatorId);
        return ApiResponse.ok(logMapper.selectList(qw.orderByDesc(SysLog::getCreatedAt)));
    }

    @GetMapping("/users/raw")
    @SaCheckPermission("system:user:list")
    public ApiResponse<List<SysUser>> users() {
        return ApiResponse.ok(userMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("datasetCount", datasetMapper.selectCount(new LambdaQueryWrapper<KbDataset>()));
        stats.put("docCount", documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>()));
        stats.put("qaCount", qaMessageMapper.selectCount(new LambdaQueryWrapper<QaMessage>()));
        return ApiResponse.ok(stats);
    }
}
