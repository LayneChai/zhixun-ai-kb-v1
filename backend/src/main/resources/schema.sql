CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(128) NOT NULL COMMENT '密码',
  role VARCHAR(32) DEFAULT 'USER' COMMENT '角色',
  status TINYINT DEFAULT 1 COMMENT '状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS kb_dataset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识库ID',
  name VARCHAR(128) NOT NULL COMMENT '名称',
  description VARCHAR(500) COMMENT '描述',
  created_by BIGINT COMMENT '创建人',
  status TINYINT DEFAULT 1 COMMENT '状态',
  is_default TINYINT DEFAULT 0 COMMENT '是否默认',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
  filename VARCHAR(255) NOT NULL COMMENT '文件名',
  file_type VARCHAR(255) COMMENT '文件类型',
  file_path VARCHAR(500) COMMENT '文件路径',
  status TINYINT DEFAULT 0 COMMENT '解析状态',
  dataset_id BIGINT NOT NULL COMMENT '知识库ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '片段ID',
  content TEXT COMMENT '内容',
  vector_id VARCHAR(128) COMMENT '向量ID',
  document_id BIGINT NOT NULL COMMENT '文档ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容片段表';

CREATE TABLE IF NOT EXISTS qa_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  dataset_id BIGINT NOT NULL COMMENT '知识库ID',
  title VARCHAR(255) COMMENT '标题',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

CREATE TABLE IF NOT EXISTS qa_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  question TEXT COMMENT '问题',
  answer LONGTEXT COMMENT '回答',
  references_json LONGTEXT COMMENT '引用来源',
  latency_ms BIGINT COMMENT '耗时',
  token_count INT COMMENT '消耗Token',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息记录表';

CREATE TABLE IF NOT EXISTS sys_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  operator_id BIGINT COMMENT '操作人',
  module VARCHAR(64) COMMENT '模块',
  action VARCHAR(255) COMMENT '行为',
  ip VARCHAR(64) COMMENT 'IP地址',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全系统操作日志';

CREATE TABLE IF NOT EXISTS llm_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  provider VARCHAR(64) NOT NULL COMMENT '供应商',
  display_name VARCHAR(128) NOT NULL COMMENT '显示名称',
  base_url VARCHAR(255) NOT NULL COMMENT 'API地址',
  api_key VARCHAR(255) NOT NULL COMMENT '密钥',
  model VARCHAR(128) NOT NULL COMMENT '模型名',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用',
  is_default TINYINT DEFAULT 0 COMMENT '是否默认',
  extra_json TEXT COMMENT '额外配置',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型配置表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
  role_key VARCHAR(100) NOT NULL COMMENT '角色标识',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
  parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
  menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
  path VARCHAR(200) DEFAULT '' COMMENT '路由',
  component VARCHAR(255) DEFAULT NULL COMMENT '前端组件',
  perms VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  icon VARCHAR(100) DEFAULT '#' COMMENT '图标',
  menu_type CHAR(1) DEFAULT '' COMMENT '类型',
  sort INT DEFAULT 0 COMMENT '排序',
  status CHAR(1) DEFAULT '0' COMMENT '状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 默认初始化数据 (省略部分重复的 INSERT 逻辑以保持文件整洁)
-- 用户信息
INSERT INTO sys_user (id, username, password, role, status)
SELECT 1, 'admin', '123456', 'super-admin', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 1);

-- 默认角色
INSERT INTO sys_role(id, role_name, role_key, status)
SELECT 1, '超级管理员', 'admin', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 1);

INSERT INTO sys_role(id, role_name, role_key, status)
SELECT 2, '普通用户', 'common', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 2);

-- 默认关联
INSERT INTO sys_user_role(user_id, role_id)
SELECT 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1);

-- 默认菜单 (仅列出根节点，详细节点见系统配置页面)
INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 1, 0, '系统管理', '/system', 'Layout', NULL, 'Setting', 'M', 1, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1);

-- RBAC menus & perms (basic system management)
INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 2, 1, 'User Management', '/system/users', 'views/UserManage', 'system:user:list', 'User', 'C', 1, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 3, 1, 'Role Management', '/system/permission', 'views/PermissionManage', 'system:role:list', 'Lock', 'C', 2, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 3);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 4, 1, 'Operation Logs', '/system/logs', 'views/OperationLogs', 'system:log:list', 'List', 'C', 3, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 4);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 11, 1, 'User Add', '', NULL, 'system:user:add', '#', 'F', 11, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 11);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 12, 1, 'User Edit', '', NULL, 'system:user:edit', '#', 'F', 12, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 12);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 13, 1, 'User Delete', '', NULL, 'system:user:delete', '#', 'F', 13, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 13);

INSERT INTO sys_menu(id, parent_id, menu_name, path, component, perms, icon, menu_type, sort, status)
SELECT 14, 1, 'User Grant', '', NULL, 'system:user:grant', '#', 'F', 14, '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 14);

INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, 4
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 4);
