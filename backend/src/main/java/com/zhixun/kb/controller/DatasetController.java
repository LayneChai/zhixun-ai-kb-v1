package com.zhixun.kb.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.entity.KbChunk;
import com.zhixun.kb.entity.KbDataset;
import com.zhixun.kb.entity.KbDocument;
import com.zhixun.kb.mapper.KbChunkMapper;
import com.zhixun.kb.mapper.KbDatasetMapper;
import com.zhixun.kb.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {
    private final KbDatasetMapper mapper;
    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;

    @PostMapping
    public ApiResponse<KbDataset> create(@RequestBody KbDataset dataset) {
        dataset.setCreatedAt(LocalDateTime.now());
        // New datasets are disabled by default
        dataset.setStatus(0);
        dataset.setIsDefault(0);

        mapper.insert(dataset);
        return ApiResponse.ok(dataset);
    }

    @GetMapping
    public ApiResponse<Page<KbDataset>> page(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        Page<KbDataset> result = mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<>());
        return ApiResponse.ok(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<KbDataset> update(@PathVariable Long id, @RequestBody KbDataset dataset) {
        dataset.setId(id);
        mapper.updateById(dataset);
        return ApiResponse.ok(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<KbDataset> toggle(@PathVariable Long id, @RequestParam Integer status) {
        if (!isAdmin()) {
            return ApiResponse.fail("No permission");
        }
        KbDataset ds = mapper.selectById(id);
        if (ds == null) {
            return ApiResponse.fail("Dataset not found");
        }
        if (status == 1) {
            KbDataset enabled = mapper.selectOne(new LambdaQueryWrapper<KbDataset>()
                    .eq(KbDataset::getStatus, 1)
                    .ne(KbDataset::getId, id)
                    .last("limit 1"));
            if (enabled != null) {
                return ApiResponse.fail("请停用已启用的知识库：" + enabled.getName());
            }
            ds.setStatus(1);
            ds.setIsDefault(1);
        } else {
            ds.setStatus(0);
            if (ds.getIsDefault() != null && ds.getIsDefault() == 1) {
                ds.setIsDefault(0);
            }
        }
        mapper.updateById(ds);
        return ApiResponse.ok(ds);
    }

    @GetMapping("/{id}/content")
    public ApiResponse<Map<String, Object>> content(@PathVariable Long id) {
        KbDataset ds = mapper.selectById(id);
        if (ds == null)
            return ApiResponse.fail("知识库不存在");

        List<KbDocument> docs = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getDatasetId, id)
                .orderByDesc(KbDocument::getCreatedAt));

        List<Map<String, Object>> docItems = docs.stream().map(doc -> {
            List<KbChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KbChunk>()
                    .eq(KbChunk::getDocumentId, doc.getId())
                    .last("limit 20"));
            Map<String, Object> m = new HashMap<>();
            m.put("document", doc);
            m.put("chunks", chunks);
            return m;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("dataset", ds);
        data.put("documents", docItems);
        return ApiResponse.ok(data);
    }

    @PostMapping("/setDefault/{id}")
    public ApiResponse<String> setDefault(@PathVariable Long id) {
        if (!isAdmin()) {
            return ApiResponse.fail("No permission");
        }
        KbDataset ds = mapper.selectById(id);
        if (ds == null) {
            return ApiResponse.fail("Dataset not found");
        }
        // 1. Reset all to 0
        KbDataset reset = new KbDataset();
        reset.setIsDefault(0);
        reset.setStatus(0);
        mapper.update(reset, new LambdaQueryWrapper<KbDataset>());

        // 2. Set target to 1
        KbDataset target = new KbDataset();
        target.setId(id);
        target.setIsDefault(1);
        target.setStatus(1);
        mapper.updateById(target);

        return ApiResponse.ok("ok");
    }

    @GetMapping("/getDefault")
    public ApiResponse<KbDataset> getDefault() {
        KbDataset dataset = mapper.selectOne(new LambdaQueryWrapper<KbDataset>()
                .eq(KbDataset::getIsDefault, 1)
                .last("limit 1"));
        return ApiResponse.ok(dataset);
    }

    private boolean isAdmin() {
        return StpUtil.hasRole("admin") || StpUtil.hasRole("super-admin");
    }
}
