-- =====================================================================
-- sql/prod/DML.sql — 生产环境专属种子
-- =====================================================================
-- 在共享 DDL 与共享 DML 之后执行。清库 init 顺序:
--   mysql ... < sql/DDL.sql
--   mysql ... < sql/DML.sql
--   mysql ... < sql/prod/DML.sql
-- 本文件只写环境相关值。用户端域名每次执行覆盖为 https://www.bgssai-short.com。
-- 管理端域名不作为 OAuth / 支付回调域。
-- =====================================================================

USE `bgssai_short`;

SET NAMES utf8mb4;

INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tencent_tokenhub.api_key', '', '生产环境 TokenHub API Key（由 Admin 写入）', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);

-- 支付 notify_url 随环境写入库内商户行（业务凭证分层）；OAuth redirect_uri 仍在 profile。

-- 支付回调 URL（生产环境用户端域名）
INSERT INTO `wechat_pay_config` (`merchant_name`, `mch_id`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short WeChat', '1112047784', 'wx496f923c7d4ac64e', 'https://www.bgssai-short.com/bgssai/user/payment/wechat/notify', 'PRODUCTION', 1, 1, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);
INSERT INTO `alipay_config` (`merchant_name`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short Alipay (PENDING app)', 'PENDING_SHORT_DRAMA', 'https://www.bgssai-short.com/bgssai/user/payment/alipay/notify', 'PRODUCTION', 0, 0, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);
