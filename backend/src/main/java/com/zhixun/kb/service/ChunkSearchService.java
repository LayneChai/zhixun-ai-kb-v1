package com.zhixun.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.zhixun.kb.config.AppProperties;
import com.zhixun.kb.entity.KbChunk;
import com.zhixun.kb.entity.KbDocument;
import com.zhixun.kb.mapper.KbChunkMapper;
import com.zhixun.kb.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BM25（Okapi BM25）全文检索服务
 *
 * <p>
 * 算法说明：
 * 
 * <pre>
 *   score(q,d) = Σ IDF(t) × [ tf(t,d)×(k1+1) ] / [ tf(t,d) + k1×(1 - b + b×|d|/avgdl) ]
 *   IDF(t)     = ln((N - df(t) + 0.5) / (df(t) + 0.5) + 1)   // Robertson IDF
 * </pre>
 * 
 * 参数：k1=1.5（词频饱和），b=0.75（长度归一化），由 application.yml 配置。
 *
 * <p>
 * 分词：中文使用 HanLP（hanlp-portable），英文按空格切分，
 * 统一转小写、过滤标点与停用词。
 *
 * <p>
 * 索引：按 datasetId 在内存中缓存，文档解析/重建索引后自动失效，
 * 下次查询时懒加载重建（Lazy Build）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkSearchService {

    private final KbChunkMapper chunkMapper;
    private final KbDocumentMapper documentMapper;
    private final AppProperties appProperties;

    /** 停用词表（高频但无信息量的词） */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这", "那",
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "shall", "can",
            "of", "in", "to", "for", "with", "on", "at", "from", "by",
            "about", "as", "into", "through", "during", "before", "after");

    // ── 内存索引结构 ──────────────────────────────────────────────────────────

    /** 每个 datasetId 对应一个 BM25 索引（懒加载，失效后重建） */
    private final Map<Long, Bm25Index> indexCache = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 对指定知识库执行 BM25 检索，返回按相关度降序排列的 Top-K 结果。
     *
     * @param query     用户问题（原文）
     * @param datasetId 知识库 ID
     * @param topK      最多返回结果数
     * @return 按 BM25 分数排序的结果列表，每项包含 chunk、docName、score
     */
    public List<Map<String, Object>> search(String query, Long datasetId, int topK) {
        if (query == null || query.isBlank() || datasetId == null) {
            return Collections.emptyList();
        }

        // 懒加载：若索引不存在则构建
        Bm25Index index = indexCache.computeIfAbsent(datasetId, this::buildIndex);
        if (index.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        AppProperties.Bm25 cfg = appProperties.getBm25();
        double k1 = cfg.getK1();
        double b = cfg.getB();
        double avgdl = index.avgDocLength();
        int N = index.docCount();

        // 对每个 chunk 计算 BM25 分数
        Map<Long, Double> scores = new HashMap<>();
        for (String term : queryTerms) {
            int df = index.docFreq(term);
            if (df == 0)
                continue;

            // Robertson IDF（防止负数）
            double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1.0);

            for (Map.Entry<Long, Map<String, Integer>> entry : index.chunkTermFreq().entrySet()) {
                long chunkId = entry.getKey();
                int tf = entry.getValue().getOrDefault(term, 0);
                if (tf == 0)
                    continue;

                int docLen = index.docLength(chunkId);
                double numerator = tf * (k1 + 1.0);
                double denominator = tf + k1 * (1.0 - b + b * docLen / avgdl);
                double termScore = idf * (numerator / denominator);
                scores.merge(chunkId, termScore, Double::sum);
            }
        }

        if (scores.isEmpty()) {
            return Collections.emptyList();
        }

        // 归一化分数到 [0, 1]
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        // 按分数排序，取 Top-K
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    long chunkId = e.getKey();
                    double normalizedScore = maxScore > 0 ? e.getValue() / maxScore : 0.0;
                    KbChunk chunk = index.chunk(chunkId);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("docName", index.docName(chunkId));
                    item.put("score", Math.round(normalizedScore * 10000.0) / 10000.0);
                    item.put("snippet", chunk != null ? chunk.getContent() : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 使缓存的 BM25 索引失效。
     * 应在文档 parse / reindex 成功后调用，下次查询将自动重建索引。
     */
    public void invalidateCache(Long datasetId) {
        if (datasetId != null) {
            indexCache.remove(datasetId);
            log.info("BM25 索引缓存已清除 [datasetId={}]", datasetId);
        }
    }

    // ── 索引构建 ──────────────────────────────────────────────────────────────

    private Bm25Index buildIndex(Long datasetId) {
        log.info("正在构建 BM25 索引 [datasetId={}]", datasetId);

        // 查询该知识库下所有文档
        List<KbDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getDatasetId, datasetId));

        if (docs.isEmpty()) {
            return Bm25Index.empty();
        }

        List<Long> docIds = docs.stream().map(KbDocument::getId).collect(Collectors.toList());
        Map<Long, String> docNameMap = docs.stream()
                .collect(Collectors.toMap(KbDocument::getId, d -> d.getFilename() == null ? "" : d.getFilename()));

        // 查询所有 chunk
        List<KbChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<KbChunk>().in(KbChunk::getDocumentId, docIds));

        if (chunks.isEmpty()) {
            return Bm25Index.empty();
        }

        // 构建：chunkId → tf表，chunkId → 文档长度，chunkId → docName，df表
        Map<Long, Map<String, Integer>> chunkTermFreq = new HashMap<>();
        Map<Long, Integer> chunkDocLen = new HashMap<>();
        Map<Long, String> chunkDocName = new HashMap<>();
        Map<Long, KbChunk> chunkMap = new HashMap<>();
        Map<String, Integer> df = new HashMap<>();

        long totalLen = 0;
        for (KbChunk chunk : chunks) {
            List<String> terms = tokenize(chunk.getContent());
            Map<String, Integer> tf = new HashMap<>();
            for (String t : terms) {
                tf.merge(t, 1, Integer::sum);
            }
            chunkTermFreq.put(chunk.getId(), tf);
            chunkDocLen.put(chunk.getId(), terms.size());
            chunkDocName.put(chunk.getId(), docNameMap.getOrDefault(chunk.getDocumentId(), ""));
            chunkMap.put(chunk.getId(), chunk);
            totalLen += terms.size();

            // 统计文档频率（每个 term 在多少个 chunk 中出现）
            for (String t : tf.keySet()) {
                df.merge(t, 1, Integer::sum);
            }
        }

        double avgdl = chunks.isEmpty() ? 1.0 : (double) totalLen / chunks.size();
        log.info("BM25 索引构建完成 [datasetId={}, chunks={}, avgDocLen={}]",
                datasetId, chunks.size(), String.format("%.1f", avgdl));

        return new Bm25Index(chunkTermFreq, chunkDocLen, chunkDocName, chunkMap, df, avgdl, chunks.size());
    }

    // ── 分词 ──────────────────────────────────────────────────────────────────

    /**
     * 对文本进行分词：HanLP 处理中文，空格切分处理英文，统一小写，过滤停用词与标点。
     */
    static List<String> tokenize(String text) {
        if (text == null || text.isBlank())
            return Collections.emptyList();

        List<Term> terms = HanLP.segment(text);
        List<String> result = new ArrayList<>();
        for (Term term : terms) {
            String w = term.word.toLowerCase().trim();
            // 过滤：长度<2、纯数字、停用词、非字母数字中文
            if (w.length() < 2)
                continue;
            if (w.matches("\\d+"))
                continue;
            if (STOP_WORDS.contains(w))
                continue;
            if (!w.matches("[\\u4e00-\\u9fa5a-z0-9]+"))
                continue;
            result.add(w);
        }
        return result;
    }

    // ── 索引数据结构 ──────────────────────────────────────────────────────────

    private static final class Bm25Index {
        private final Map<Long, Map<String, Integer>> chunkTermFreq;
        private final Map<Long, Integer> chunkLen;
        private final Map<Long, String> chunkDocName;
        private final Map<Long, KbChunk> chunkMap;
        private final Map<String, Integer> dfMap;
        private final double avgDocLength;
        private final int docCount;

        Bm25Index(Map<Long, Map<String, Integer>> chunkTermFreq,
                Map<Long, Integer> chunkLen,
                Map<Long, String> chunkDocName,
                Map<Long, KbChunk> chunkMap,
                Map<String, Integer> dfMap,
                double avgDocLength,
                int docCount) {
            this.chunkTermFreq = chunkTermFreq;
            this.chunkLen = chunkLen;
            this.chunkDocName = chunkDocName;
            this.chunkMap = chunkMap;
            this.dfMap = dfMap;
            this.avgDocLength = avgDocLength;
            this.docCount = docCount;
        }

        static Bm25Index empty() {
            return new Bm25Index(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), 1.0, 0);
        }

        boolean isEmpty() {
            return docCount == 0;
        }

        double avgDocLength() {
            return avgDocLength;
        }

        int docCount() {
            return docCount;
        }

        Map<Long, Map<String, Integer>> chunkTermFreq() {
            return chunkTermFreq;
        }

        int docFreq(String term) {
            return dfMap.getOrDefault(term, 0);
        }

        int docLength(long cid) {
            return chunkLen.getOrDefault(cid, 0);
        }

        String docName(long cid) {
            return chunkDocName.getOrDefault(cid, "");
        }

        KbChunk chunk(long cid) {
            return chunkMap.get(cid);
        }
    }
}
