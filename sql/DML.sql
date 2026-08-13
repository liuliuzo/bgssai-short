-- =====================================================================
-- DML.sql — AI 短剧制作平台种子数据（仓库根 sql/DML.sql）
-- =====================================================================
-- 执行前先跑 sql/DDL.sql 建库建表。
-- 两个示例账号，密码为 BCrypt 存储（$2a$）：
--   admin / admin123   org_code=PLATFORM_ADMIN，可登录管理端（8080）
--   demo  / demo123    普通用户，可登录用户端（8081）
-- 生产环境请删除或改密这两个示例账号；请勿在联网环境沿用示例密码。
-- =====================================================================

USE `bgssai_short_drama`;

DELETE FROM `sys_user` WHERE `user_name` IN ('admin', 'demo');

-- 一行数据一条 INSERT（Standards §2「DML 种子 / 数据维护脚本写法」）。
INSERT INTO `sys_user` (`user_id`, `user_name`, `real_name`, `password`, `org_code`, `status`, `del_flag`, `create_by`) VALUES ('u-admin-0001', 'admin', '系统管理员', '$2a$10$0eTrXkHZyyyevt7SLUuKiOgg4fy/5KFAQeGtW5MfWrfopp3xoqAy2', 'PLATFORM_ADMIN', 1, 0, 'system');
INSERT INTO `sys_user` (`user_id`, `user_name`, `real_name`, `password`, `org_code`, `status`, `del_flag`, `create_by`) VALUES ('u-demo-0001', 'demo', '演示用户', '$2a$10$ijjA2bzajVCFg4gaDhmT7eW3vx6.luk9pUB5kR9w/TWpHi/l1HK8S', 'USER', 1, 0, 'system');
