package com.zhixun.kb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用级配置属性
 * 对应 application.yml 中的 app.* 节点
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文档上传存储目录，支持相对路径（相对于工作目录），兼容 Windows/Linux */
    private String uploadDir = "./data/uploads";

    /** BM25 检索参数 */
    private Bm25 bm25 = new Bm25();

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public Bm25 getBm25() {
        return bm25;
    }

    public void setBm25(Bm25 bm25) {
        this.bm25 = bm25;
    }

    public static class Bm25 {
        /** 词频饱和参数，典型值 1.2~2.0 */
        private double k1 = 1.5;
        /** 文档长度归一化参数，0=不归一化，1=完全归一化 */
        private double b = 0.75;
        /** 返回最相关的 TopK 片段数 */
        private int topK = 5;

        public double getK1() {
            return k1;
        }

        public void setK1(double k1) {
            this.k1 = k1;
        }

        public double getB() {
            return b;
        }

        public void setB(double b) {
            this.b = b;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}
