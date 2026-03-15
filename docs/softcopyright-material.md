# 智询AI知识库问答系统 V1.0 — 软著提交材料

---

## A. 软件说明书框架

### 一、软件概述

| 项目 | 内容 |
|---|---|
| **软件名称** | 智询AI知识库问答系统 |
| **版本号** | V1.0 |
| **著作权人** | （填写单位/个人名称） |
| **开发完成日期** | 2025 年 |
| **开发语言** | Java 17、JavaScript (Vue 3) |
| **运行环境** | JDK 17+、MySQL 8.0+、Node.js 18+、现代浏览器 |

**软件用途**：面向企业/团队的 AI 知识库问答平台。用户可上传 PDF、Word、TXT、Markdown 等格式文档构建知识库，系统通过 **BM25 全文检索算法**结合大语言模型，实现基于企业私有知识的智能问答，并提供答案溯源功能。

---

### 二、功能说明

#### 2.1 用户认证模块
- 账号密码登录，Token 鉴权（Bearer），退出登录清除凭证。
- 路由守卫保障未登录用户无法访问受保护页面。

#### 2.2 知识库管理模块
- 创建、编辑、启用/停用、删除知识库。
- 查看知识库内所有文档及其解析片段（折叠展示）。

#### 2.3 文档管理模块
- 支持上传 PDF、DOCX、TXT、Markdown、CSV 格式。
- 文档解析（文本提取 + 分块切片，chunk_size=500，overlap=100）。
- 解析后自动清除 BM25 内存索引，触发懒加载重建。

#### 2.4 智能问答模块
- 多会话管理（创建/重命名/切换会话）。
- 支持流式（SSE）与非流式两种应答模式。
- **BM25 检索**：根据用户问题从知识库中召回最相关 Top-5 文本片段，作为上下文送入大模型。
- 展示引用来源（文档名、BM25 相关度分数、原始片段）。

#### 2.5 答案溯源模块
- 每条问答消息存储对应引用片段 JSON。
- 前端折叠展示，支持追溯回答依据。

#### 2.6 大模型配置模块
- 支持多套 OpenAI-Compatible 接口配置（BaseURL/APIKey/模型名）。
- 可设置默认模型，动态切换无需重启。

#### 2.7 系统管理模块
- 用户列表查看。
- 操作日志记录（模块、动作、IP、时间）。
- 控制台统计数据（知识库数、文档数、问答次数）。

---

### 三、系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端（Vue 3 + Element Plus）            │
│   LoginView / DashboardView / DatasetView / DocumentView │
│   QaView / LlmConfigView / UserLogView                   │
│                  Axios + EventSource（SSE）               │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP / SSE
┌───────────────────────▼─────────────────────────────────┐
│              后端（Spring Boot 3 + MyBatis-Plus）          │
│                                                          │
│  AuthController   DatasetController   DocumentController  │
│  QaController     LlmConfigController SystemController   │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ChunkSearchService（BM25 全文检索核心）             │  │
│  │  ·分词：HanLP Portable（中文）/ 空格切分（英文）     │  │
│  │  ·倒排索引：内存 Map，按 datasetId 缓存             │  │
│  │  ·打分：Okapi BM25（k₁=1.5，b=0.75）               │  │
│  │  ·结果：Top-K 归一化相关度分数                       │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  LlmService（OpenAI-Compatible，流式/非流式）             │
│  DocumentParseService（PDFBox / POI / 纯文本）            │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│                        MySQL 8.0                         │
│  sys_user  kb_dataset  kb_document  kb_chunk             │
│  qa_session  qa_message  sys_log  llm_config             │
└─────────────────────────────────────────────────────────┘
```

---

### 四、检索算法说明（核心创新点）

#### BM25（Best Match 25 / Okapi BM25）

BM25 是信息检索领域最经典的概率检索模型，被 Elasticsearch 等主流搜索引擎采用。

**打分公式：**

$$score(q,d) = \sum_{t \in q} IDF(t) \cdot \frac{tf(t,d) \cdot (k_1 + 1)}{tf(t,d) + k_1 \cdot \left(1 - b + b \cdot \frac{|d|}{avgdl}\right)}$$

$$IDF(t) = \ln\left(\frac{N - df(t) + 0.5}{df(t) + 0.5} + 1\right)$$

| 符号 | 含义 |
|---|---|
| tf(t,d) | 词项 t 在文档片段 d 中的词频 |
| df(t) | 包含词项 t 的片段总数 |
| N | 知识库片段总数 |
| \|d\| | 片段词数 |
| avgdl | 语料库平均片段词数 |
| k₁=1.5 | 词频饱和参数 |
| b=0.75 | 文档长度归一化参数 |

**中文分词：**  使用 HanLP（`hanlp-portable-1.8.4`）对中文文本分词，英文按空格切分，统一过滤停用词与标点。

**索引策略：**  内存倒排索引，按 datasetId 懒加载构建；文档解析成功后自动失效并重建。

---

### 五、数据库设计（核心表）

| 表名 | 说明 |
|---|---|
| sys_user | 系统用户（id, username, password, role, status） |
| kb_dataset | 知识库（id, name, description, created_by, status） |
| kb_document | 文档（id, filename, file_type, file_path, status, dataset_id） |
| kb_chunk | 文本片段（id, content, vector_id, document_id） |
| qa_session | 问答会话（id, user_id, dataset_id, title） |
| qa_message | 问答消息（id, session_id, question, answer, references_json, latency_ms, token_count） |
| sys_log | 操作日志（id, operator_id, module, action, ip） |
| llm_config | 大模型配置（id, provider, display_name, base_url, api_key, model, enabled, is_default） |

---

### 六、核心接口清单

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | /api/auth/login | 账号密码登录 |
| 认证 | POST | /api/auth/logout | 退出登录 |
| 认证 | GET | /api/auth/me | 获取当前用户信息 |
| 知识库 | POST | /api/datasets | 创建知识库 |
| 知识库 | GET | /api/datasets | 分页查询知识库列表 |
| 知识库 | PUT | /api/datasets/{id} | 编辑知识库 |
| 知识库 | DELETE | /api/datasets/{id} | 删除知识库 |
| 知识库 | POST | /api/datasets/{id}/toggle | 启用/停用 |
| 知识库 | GET | /api/datasets/{id}/content | 查看知识库内容（含片段） |
| 文档 | POST | /api/documents/upload | 上传文档 |
| 文档 | POST | /api/documents/{id}/parse | 解析切片 + 重建BM25索引 |
| 文档 | POST | /api/documents/{id}/reindex | 重建索引标记 |
| 文档 | DELETE | /api/documents/{id} | 删除文档 |
| 文档 | GET | /api/documents | 按知识库查询文档列表 |
| 问答 | POST | /api/qa/session | 创建会话 |
| 问答 | GET | /api/qa/sessions | 查询会话列表 |
| 问答 | POST | /api/qa/ask | 非流式问答（BM25召回+LLM）|
| 问答 | GET | /api/qa/ask/stream | 流式问答（SSE） |
| 问答 | GET | /api/qa/history | 查询会话历史消息 |
| 问答 | POST | /api/qa/session/rename | 重命名会话 |
| 溯源 | GET | /api/qa/source/{messageId} | 查询引用来源 |
| 大模型 | POST | /api/llm-config | 新增模型配置 |
| 大模型 | GET | /api/llm-config | 查询所有配置 |
| 大模型 | PUT | /api/llm-config/{id} | 编辑配置 |
| 大模型 | POST | /api/llm-config/{id}/default | 设为默认模型 |
| 大模型 | DELETE | /api/llm-config/{id} | 删除配置 |
| 系统 | GET | /api/system/stats | 统计数据 |
| 系统 | GET | /api/system/logs | 操作日志 |
| 系统 | GET | /api/system/users | 用户列表 |

---

### 七、部署说明

#### 环境要求
- JDK 17+
- MySQL 8.0+
- Node.js 18+

#### 数据库初始化
```sql
CREATE DATABASE zhixun_kb CHARACTER SET utf8mb4;
CREATE USER 'zhixun'@'localhost' IDENTIFIED BY 'Zhixun@123';
GRANT ALL ON zhixun_kb.* TO 'zhixun'@'localhost';
-- 执行 backend/src/main/resources/schema.sql
```

#### 后端启动
```bash
cd backend
mvn spring-boot:run
# 服务启动于 http://localhost:8080
```

#### 前端启动
```bash
cd frontend
npm install
npm run dev
# 开发服务器启动于 http://localhost:5173
```

---

## B. 源代码打印建议

建议打印以下关键文件（前后端各不少于 60 页，连续页码）：

**后端（优先级从高到低）：**
1. `ChunkSearchService.java`（BM25 检索核心算法，约 200 行）
2. `QaController.java`（问答接口）
3. `DocumentController.java`（文档管理）
4. `LlmService.java`（大模型调用）
5. `DocumentParseService.java`（文档解析切片）
6. `DatasetController.java`、`AuthController.java`、`SystemController.java`
7. 所有 Entity / Mapper 类
8. `KbApplication.java`、`AppProperties.java`、`SecurityConfig.java`
9. `schema.sql`、`application.yml`

**前端（优先级从高到低）：**
1. `QaView.vue`（智能问答页，含BM25引用展示）
2. `App.vue`（全局布局）
3. `DatasetView.vue`、`DocumentView.vue`
4. `DashboardView.vue`、`LlmConfigView.vue`
5. `LoginView.vue`、`UserLogView.vue`
6. `http.js`（axios 拦截器）、`router/index.js`（路由守卫）

---

## C. 版本说明

| 版本 | 状态 | 说明 |
|---|---|---|
| **V1.0（当前）** | ✅ 软著申报版 | 完整功能模块、BM25检索算法、流式问答、答案溯源 |
| V1.1（规划） | 🔲 | 接入向量数据库（pgvector/Milvus），Embedding 语义检索 |
| V1.2（规划） | 🔲 | RAG 流程进阶优化，多轮对话，权限细化 |
