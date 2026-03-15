package com.zhixun.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.entity.LlmConfig;
import com.zhixun.kb.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/llm-config")
@RequiredArgsConstructor
public class LlmConfigController {
    private final LlmConfigMapper mapper;

    @GetMapping
    public ApiResponse<List<LlmConfig>> list() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<LlmConfig>().orderByDesc(LlmConfig::getCreatedAt)));
    }

    @PostMapping
    public ApiResponse<LlmConfig> create(@RequestBody LlmConfig cfg) {
        if (cfg.getEnabled() == null) cfg.setEnabled(1);
        if (cfg.getIsDefault() == null) cfg.setIsDefault(0);
        cfg.setCreatedAt(LocalDateTime.now());
        mapper.insert(cfg);
        return ApiResponse.ok(cfg);
    }

    @PutMapping("/{id}")
    public ApiResponse<LlmConfig> update(@PathVariable Long id, @RequestBody LlmConfig cfg) {
        cfg.setId(id);
        mapper.updateById(cfg);
        return ApiResponse.ok(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> remove(@PathVariable Long id) {
        mapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    @PostMapping("/{id}/default")
    public ApiResponse<String> setDefault(@PathVariable Long id) {
        List<LlmConfig> all = mapper.selectList(new LambdaQueryWrapper<>());
        for (LlmConfig c : all) {
            c.setIsDefault(c.getId().equals(id) ? 1 : 0);
            mapper.updateById(c);
        }
        return ApiResponse.ok("ok");
    }
}
