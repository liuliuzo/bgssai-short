-- =====================================================================
-- sql/dev/DML.sql — 开发环境专属种子
-- =====================================================================
-- 在共享 DDL 与共享 DML 之后执行。清库 init 顺序:
--   mysql ... < sql/DDL.sql
--   mysql ... < sql/DML.sql
--   mysql ... < sql/dev/DML.sql
-- 本文件只写环境相关值。用户端域名每次执行覆盖为 https://dev.user.bgssai-short.com。
-- 管理端域名不作为 OAuth / 支付回调域。
-- =====================================================================

USE `bgssai_short`;

SET NAMES utf8mb4;

INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tencent_tokenhub.api_key', '', '开发环境 TokenHub API Key（由 Admin 写入）', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);

-- 除业务第三方凭证空值行外，当前无环境专属种子。登录 OAuth redirect_uri / 支付 notify-url 写在
-- application-*.properties，不入本表。后续若有随环境变化的库内值，只改
-- sql/dev/DML.sql 与 sql/prod/DML.sql，保持 STEP 名与列清单一致、只改字面量。
