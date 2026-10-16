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

INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tencent_tokenhub.api_key', 'sk-bgssai-short-cn-temp', 'TokenHub-CN gateway key (bgssai-short)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;

-- 腾讯云短信 SdkAppId（BGSSAI 共享控制台 · 生产应用）；SignName / TemplateId 见共享 DML STEP SMS-1
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.sms.sdk-app-id', '1401191076', '腾讯云短信 SdkAppId（生产 · 共享控制台）', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);

-- 支付 notify_url 随环境写入库内商户行（业务凭证分层）；OAuth redirect_uri 仍在 profile。

-- 支付回调 URL（生产环境用户端域名）
INSERT INTO `wechat_pay_config` (`merchant_name`, `mch_id`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short WeChat', '1112047784', 'wx496f923c7d4ac64e', 'https://www.bgssai-short.com/bgssai/user/payment/wechat/notify', 'PRODUCTION', 1, 1, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);
INSERT INTO `alipay_config` (`merchant_name`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short Alipay', '2021006196627810', 'https://www.bgssai-short.com/bgssai/user/payment/alipay/notify', 'PRODUCTION', 1, 1, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);

-- 
-- Force TokenHub-CN API key + DeepSeek TEXT default (2026-09-17)
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`)
VALUES ('ai.tencent_tokenhub.api_key', 'sk-bgssai-short-cn-temp', 'TokenHub-CN gateway key (bgssai-short)', 1, 'system')
ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
UPDATE ai_model_config
SET model_code = 'deepseek-chat', model_name = 'DeepSeek Chat', default_flag = 1,
    config_json = JSON_SET(COALESCE(config_json, '{}'), '$.base_url', 'https://www.bgssai-tokenhub.cn', '$.chat_path', '/v1/chat/completions', '$.secret_ref', 'ai.tencent_tokenhub.api_key')
WHERE model_id = 'm-tokenhub-text-deepseek';
UPDATE ai_model_config SET default_flag = 0
WHERE provider_code = 'TENCENT_TOKENHUB' AND ability_type = 'TEXT' AND model_id <> 'm-tokenhub-text-deepseek';
UPDATE ai_model_config
SET config_json = JSON_SET(COALESCE(config_json, '{}'), '$.base_url', 'https://www.bgssai-tokenhub.cn')
WHERE provider_code = 'TENCENT_TOKENHUB' AND ability_type = 'TEXT';

-- Dual TokenHub secrets (prod seeds; replace via Admin after deploy)
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tokenhub_cn.api_key', 'sk-bgssai-short-cn-temp', 'BGSSAI TokenHub-CN API Key (bgssai-short)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tokenhub_global.api_key', 'sk-bgssai-short-global-temp', 'BGSSAI TokenHub-Global API Key (bgssai-short)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
UPDATE `ai_model_config`
SET `config_json` = JSON_SET(COALESCE(`config_json`, '{}'), '$.hub', 'cn', '$.base_url', 'https://www.bgssai-tokenhub.cn', '$.secret_ref', 'ai.tokenhub_cn.api_key')
WHERE `provider_code` = 'TENCENT_TOKENHUB' AND `model_id` NOT LIKE 'm-tokenhub-global-%';

-- 本库账本默认套餐目录（生产环境幂等回填）
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('FREE', '免费版', 0, 0, 1000, 10000, 1, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('PRO', '专业版', 9900, 99900, 20000, 100000, 2, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('MAX', '旗舰版', 29900, 299900, 100000, 500000, 3, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;

