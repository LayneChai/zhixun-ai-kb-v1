package com.zhixun.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.config.AppProperties;
import com.zhixun.kb.entity.KbChunk;
import com.zhixun.kb.entity.KbDocument;
import com.zhixun.kb.mapper.KbChunkMapper;
import com.zhixun.kb.mapper.KbDocumentMapper;
import com.zhixun.kb.service.ChunkSearchService;
import com.zhixun.kb.service.DocumentParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final DocumentParseService documentParseService;
    private final ChunkSearchService chunkSearchService;
    private final AppProperties appProperties;

    @PostMapping("/upload")
    public ApiResponse<KbDocument> upload(@RequestParam Long datasetId, @RequestPart MultipartFile file) {
        try {
            log.info("Starting upload: datasetId={}, filename={}, size={}", datasetId, file.getOriginalFilename(),
                    file.getSize());
            KbDocument doc = new KbDocument();
            doc.setDatasetId(datasetId);
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.length() > 255) {
                originalFilename = originalFilename.substring(0, 255);
            }
            doc.setFilename(originalFilename);

            String contentType = file.getContentType();
            if (contentType != null && contentType.length() > 255) {
                contentType = contentType.substring(0, 255);
            }
            doc.setFileType(contentType);
            doc.setStatus(0); // 0:已上传
            doc.setCreatedAt(LocalDateTime.now());
            documentMapper.insert(doc);
            log.info("Document inserted: id={}", doc.getId());

            // 使用可配置路径（兼容 Windows/Linux）
            String safeName = (file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                    .replaceAll("[^a-zA-Z0-9._\\-\u4e00-\u9fa5]", "_");
            Path dir = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(doc.getId() + "_" + safeName);
            log.info("Saving file to: {}", target);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            doc.setFilePath(target.toString());
            documentMapper.updateById(doc);
            log.info("Upload completed: id={}", doc.getId());
            return ApiResponse.ok(doc);
        } catch (Exception e) {
            log.error("Upload failed: ", e);
            return ApiResponse.fail("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/parse")
    public ApiResponse<String> parse(@PathVariable Long id) {
        try {
            log.info("Starting parse: id={}", id);
            KbDocument doc = documentMapper.selectById(id);
            if (doc == null)
                return ApiResponse.fail("文档不存在");
            if (doc.getFilePath() == null || doc.getFilePath().isBlank())
                return ApiResponse.fail("文件路径不存在，请重新上传");

            String text = documentParseService.extractText(Path.of(doc.getFilePath()), doc.getFilename());
            List<String> chunks = documentParseService.chunk(text, 500, 100);

            chunkMapper.delete(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, id));
            int idx = 1;
            for (String c : chunks) {
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(id);
                chunk.setVectorId("bm25-" + id + "-" + (idx++));
                chunk.setContent(c);
                chunkMapper.insert(chunk);
            }

            doc.setStatus(1); // 1:已解析
            documentMapper.updateById(doc);

            // 清除该知识库的 BM25 索引缓存，下次查询时重建
            chunkSearchService.invalidateCache(doc.getDatasetId());
            log.info("Parse completed: id={}, chunks={}", id, chunks.size());

            return ApiResponse.ok("parsed:" + chunks.size());
        } catch (Exception e) {
            log.error("Parse failed: ", e);
            return ApiResponse.fail("解析失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/reindex")
    public ApiResponse<String> reindex(@PathVariable Long id) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc == null)
            return ApiResponse.fail("文档不存在");
        doc.setStatus(2); // 2:已重建索引
        documentMapper.updateById(doc);
        // 清除 BM25 索引缓存
        chunkSearchService.invalidateCache(doc.getDatasetId());
        return ApiResponse.ok("reindexed");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc != null) {
            if (doc.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Path.of(doc.getFilePath()));
                } catch (Exception ignored) {
                }
            }
            chunkSearchService.invalidateCache(doc.getDatasetId());
        }
        chunkMapper.delete(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, id));
        documentMapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    @GetMapping
    public ApiResponse<List<KbDocument>> list(@RequestParam Long datasetId) {
        return ApiResponse.ok(documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getDatasetId, datasetId)
                .orderByDesc(KbDocument::getCreatedAt)));
    }
}
