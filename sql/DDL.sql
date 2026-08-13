-- =====================================================================
-- DDL.sql — AI 短剧制作平台建库脚本（单一真值，仓库根 sql/DDL.sql）
-- =====================================================================
-- 管理端与用户端共用同一个库 bgssai_short_drama。当前仅含两张基础表：
--   sys_user   用户表（登录鉴权）
--   login_log  登录记录表（示例分页接口的数据源）
--
-- 约定：
--   - 应用启动期不自动执行任何 DDL；schema 由部署方手动执行本文件建立（Standards §9）。
--   - 列命名保持现网风格（del_flag / create_by / create_time / update_by / update_time）。
--   - 新增 / 变更表结构后，用 MyBatis Generator 按新库表重新生成 model/mapper/XML。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `bgssai_short_drama` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `bgssai_short_drama`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键id',
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '用户id',
  `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '登录账号',
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '密码（BCrypt）',
  `salt` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '历史盐字段（BCrypt 后已废弃）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '头像',
  `birthday` datetime NULL DEFAULT NULL COMMENT '生日',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别(0-未知,1-男,2-女)',
  `email` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '电话',
  `org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '角色/机构编码（如 PLATFORM_ADMIN）',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态(1-正常,2-冻结)',
  `company_id` int(11) NULL DEFAULT NULL COMMENT '公司ID',
  `online` tinyint(1) NULL DEFAULT NULL COMMENT '在线状态(0-在线,1-不在线)',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_user_name`(`user_name`) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_user_id`(`user_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_del_flag`(`del_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for login_log
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '用户id',
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '动作(LOGIN_SUCCESS / LOGIN_FAIL_PASSWORD / LOGIN_FAIL_ROLE)',
  `ip_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '登录IP',
  `user_agent` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT 'user-agent',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'system' COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'system' COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_del_flag`(`del_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
