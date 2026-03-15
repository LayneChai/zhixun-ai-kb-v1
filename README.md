# 智询AI知识库问答系统 V1.0（软著版）

## 1. 系统定位
该系统定位为企业级知识库管理与智能问答服务平台，核心流程包括：
上传文档 → 自动切片与解析 → 建立向量知识库 → AI智能问答 → 答案溯源 → 会话留痕。

## 2. 技术栈
- **后端**：Spring Boot 3 + MyBatis-Plus + MySQL + Vector Store (假设后端封装)
- **前端**：Vue 3 + Vite + Element Plus + Vue Router + Pinia

## 3. 数据库设计
系统采用关系型数据库进行元数据和配置存储，建表脚本见 `backend/src/main/resources/schema.sql`，核心表包括：
- **基础核心表**
  - `sys_user`: 用户表
  - `sys_role`: 系统角色表
  - `sys_menu`: 系统菜单表
  - `sys_user_role`: 用户角色关联表
  - `sys_role_menu`: 角色菜单关联表
  - `sys_log`: 全系统操作日志表
- **大模型与配置表**
  - `llm_config`: 大模型配置表（支持多供应商配置）
- **知识库与文档表**
  - `kb_dataset`: 知识库表
  - `kb_document`: 文档表
  - `kb_chunk`: 内容片段/向量关联表
- **智能问答记录表**
  - `qa_session`: 会话记录表
  - `qa_message`: 消息明细记录表

## 4. 核心功能与API模块
基于 `Controller` 分离架构，系统主要提供以下业务接口与功能：

### 4.1 认证与授权 (`AuthController` / `OauthController`)
- 基于 Token 的登录退出机制
- 第三方 OAuth2.0 登录（如果已实现）
- 用户信息获取

### 4.2 RBAC与系统管理 (`RbacController` / `SystemController`)
- **角色与权限管理**：分配角色、操作菜单挂载与按角色验权设置
- **用户管理**：系统账户增删改查
- **系统日志**：所有核心操作留痕追踪

### 4.3 知识库与文档管理 (`DatasetController` / `DocumentController`)
- 知识库的创建、编辑与切换
- 文档的上传、状态更新以及文件管理
- 文档的精准切块归档及重新索引配置

### 4.4 问答与大模型配置 (`QaController` / `LlmConfigController`)
- LLM模型管理：切换不同厂商的大模型或调整默认API Key
- 新建问答会话（流式响应）
- 获取历史问答详情及针对每一条回答的「来源溯源」

## 5. 前端路由结构
前端管理平台主要包含以下页面路由模块：
- `/login`: 登录页
- `/dashboard`: 系统主控面板 (仪表盘)
- `/dataset`: 知识库管理
- `/document`: 文档列表与上传管理
- `/qa`: 智能问答界面
- `/llm-config`: 大语言模型配置面板
- `/system/*`: 系统设置核心页（用户管理 `/users`、角色与权限管理 `/roles` & `/permission`、菜单管理 `/menus`、日志审计 `/logs`）

## 6. 运行方式
### 后端
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 前端
```bash
cd frontend
npm install
npm run dev
```

## 7. 软著材料建议
见 `docs/softcopyright-material.md`
