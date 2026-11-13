-- =====================================================================
-- sql/dev/DML.sql — 开发环境专属种子
-- =====================================================================
-- 在共享 DDL 与共享 DML 之后执行。清库 init 顺序:
--   mysql ... < sql/DDL.sql
--   mysql ... < sql/DML.sql
--   mysql ... < sql/dev/DML.sql
-- 本文件只写环境相关值。用户端域名每次执行覆盖为 https://dev.user.bgssai-short.com。
-- 管理端域名不作为 OAuth / 支付回调域。
-- 末尾 QA 账号只属于开发库，禁止抄进 sql/prod/DML.sql。
-- =====================================================================

USE `bgssai_short`;

SET NAMES utf8mb4;

INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tencent_tokenhub.api_key', 'sk-bgssai-short-cn-temp', 'TokenHub-CN gateway key (bgssai-short)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;

-- 腾讯云短信 SdkAppId（BGSSAI 共享控制台 · 开发应用）；SignName / TemplateId 见共享 DML STEP SMS-1
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.sms.sdk-app-id', '1401156379', '腾讯云短信 SdkAppId（开发 · 共享控制台）', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);

-- 支付 notify_url 随环境写入库内商户行（业务凭证分层）。OAuth redirect_uri 仍在 profile。

-- 微信开放平台网站应用（授权登录，channel/provider=wechat，账号 B，已通过）。
-- 开发库每次执行都覆盖 AppID / AppSecret，用来换掉误写入的官网或 blog AppID。
-- 只动 wechat 这两行，不改支付宝、抖音、百度及其它渠道。
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-id', 'wx1b5b8a21ba130fb8', '登录页微信开放平台网站应用 AppID(snsapi_login) · DEV', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-secret', '5100147ce88a93b21dc4a2c0e186e826', '登录页微信开放平台网站应用 AppSecret · DEV', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;

-- 支付回调 URL（开发环境用户端域名）
INSERT INTO `wechat_pay_config` (`merchant_name`, `mch_id`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short WeChat', '1112047784', 'wx496f923c7d4ac64e', 'https://dev.user.bgssai-short.com/bgssai/user/payment/wechat/notify', 'PRODUCTION', 1, 1, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);
INSERT INTO `alipay_config` (`merchant_name`, `app_id`, `notify_url`, `env_mode`, `is_enabled`, `is_collection_active`, `del_flag`, `creator`) VALUES ('BGSSAI Short Alipay', '2021006196627810', 'https://dev.user.bgssai-short.com/bgssai/user/payment/alipay/notify', 'PRODUCTION', 1, 1, 0, 'seed') ON DUPLICATE KEY UPDATE `notify_url` = IF(`notify_url` IS NULL OR `notify_url` = '', VALUES(`notify_url`), `notify_url`);

-- 
-- Keep TokenHub-CN routing normalized without overriding company defaults
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`)
VALUES ('ai.tencent_tokenhub.api_key', 'sk-bgssai-short-cn-temp', 'TokenHub-CN gateway key (bgssai-short)', 1, 'system')
ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
UPDATE ai_model_config
SET model_code = 'deepseek-chat', model_name = 'DeepSeek Chat', default_flag = 0,
    config_json = JSON_REMOVE(JSON_SET(COALESCE(config_json, '{}'), '$.hub', 'cn', '$.chat_path', '/v1/chat/completions', '$.secret_ref', 'ai.tokenhub_cn.api_key'), '$.base_url')
WHERE model_id = 'm-tokenhub-text-deepseek';
UPDATE ai_model_config
SET config_json = JSON_REMOVE(COALESCE(config_json, '{}'), '$.base_url')
WHERE provider_code = 'TENCENT_TOKENHUB';

-- CN gateway key only. Overseas key is disabled and is not a fallback.
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tokenhub_cn.api_key', 'sk-bgssai-short-cn-temp', 'BGSSAI TokenHub-CN API Key (bgssai-short)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = VALUES(`secret_value`), `description` = VALUES(`description`), `enabled` = 1;
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.tokenhub_global.api_key', '', '短剧不使用境外中枢密钥', 0, 'system') ON DUPLICATE KEY UPDATE `enabled` = 0, `description` = VALUES(`description`);
UPDATE `ai_model_config`
SET `config_json` = JSON_REMOVE(JSON_SET(COALESCE(`config_json`, '{}'), '$.hub', 'cn', '$.secret_ref', 'ai.tokenhub_cn.api_key'), '$.base_url')
WHERE `provider_code` = 'TENCENT_TOKENHUB' AND `model_id` NOT LIKE 'm-tokenhub-global-%';
UPDATE `ai_model_config`
SET `enabled_flag` = 0, `default_flag` = 0
WHERE `model_id` LIKE 'm-tokenhub-global-%'
   OR (`provider_code` = 'TENCENT_TOKENHUB' AND JSON_UNQUOTE(JSON_EXTRACT(`config_json`, '$.hub')) = 'global');

-- 可灵文生图保留为可选备选模型，不覆盖公司默认 bgssai-image。
INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-tokenhub-image-kling', 'IMAGE', 'TENCENT_TOKENHUB', 'kling-v3', '可灵文生图', 'STANDARD', 'PER_CALL', 80, 0, 1, 180, 1, '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key"}') ON DUPLICATE KEY UPDATE `model_code` = VALUES(`model_code`), `model_name` = VALUES(`model_name`), `provider_code` = VALUES(`provider_code`), `default_flag` = 0, `enabled_flag` = 1, `config_json` = VALUES(`config_json`), `deleted_flag` = 0;

-- 本库账本默认套餐目录（开发环境幂等回填）
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('FREE', '免费版', 0, 0, 1000, 10000, 1, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('PRO', '专业版', 9900, 99900, 20000, 100000, 2, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;
INSERT INTO `ledger_subscription_plan` (`plan_code`, `plan_name`, `monthly_price_cent`, `yearly_price_cent`, `five_hour_limit_tokens`, `weekly_limit_tokens`, `sort_no`, `enabled`, `del_flag`, `creator`, `modifier`) VALUES ('MAX', '旗舰版', 29900, 299900, 100000, 500000, 3, 1, 0, 'system', 'system') ON DUPLICATE KEY UPDATE `plan_name` = VALUES(`plan_name`), `five_hour_limit_tokens` = VALUES(`five_hour_limit_tokens`), `weekly_limit_tokens` = VALUES(`weekly_limit_tokens`), `enabled` = 1, `del_flag` = 0;

-- =====================================================================
-- QA 登录账号（仅开发库，对齐 2026-10-15 QA 密钥环境）
-- qa_short_a 管理端 PLATFORM_ADMIN；qa_short_u 用户端 USER。
-- 用户名与邮箱都能作为密码登录标识。重跑本段会把口令、邮箱、角色盖回种子，
-- 避免开发库口令漂移后浏览器 QA 再报 4001。禁止写入生产种子。
-- =====================================================================
INSERT INTO `sys_user` (`user_id`, `user_name`, `real_name`, `password`, `email`, `org_code`, `status`, `del_flag`, `create_by`) VALUES ('qa-e1e72ce67088450f822c', 'qa_short_a', 'qa_short_a', 'PM3nrTt9PIaRT3p8L07g', 'qa+short+a@bgssai-test.local', 'PLATFORM_ADMIN', 1, 0, 'qa-seed-20261015') ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `email` = VALUES(`email`), `real_name` = VALUES(`real_name`), `org_code` = VALUES(`org_code`), `status` = 1, `del_flag` = 0;
INSERT INTO `sys_user` (`user_id`, `user_name`, `real_name`, `password`, `email`, `org_code`, `status`, `del_flag`, `create_by`) VALUES ('qa-e47544decfbd4c74bfd2', 'qa_short_u', 'qa_short_u', 'Ckeh8Pqt3eSjN5BWaOh9', 'qa+short+u@bgssai-test.local', 'USER', 1, 0, 'qa-seed-20261015') ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `email` = VALUES(`email`), `real_name` = VALUES(`real_name`), `org_code` = VALUES(`org_code`), `status` = 1, `del_flag` = 0;

-- 公司自托管多模态模型目录对齐（2026-11-12 / 2026-11-13）：
-- 文本（bgssai-chat）、分镜图（bgssai-image）、配音（bgssai-tts）、文生视频（bgssai-video）统一设为默认。
-- 图生视频（https://chat.bgssai.com/i2v）与公司直连语音转写（https://chat.bgssai.com/v1/audio/transcriptions）仅写空占位密钥，绝不写入真实密钥。
INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-tokenhub-text-bgssai-chat', 'TEXT', 'TENCENT_TOKENHUB', 'bgssai-chat', 'BGSSAI Chat', 'STANDARD', 'PER_KILO_TOKEN', 1, 1, 1, 120, 1, '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","chat_path":"/v1/chat/completions","public_alias":"bgssai-chat","public_name":"BGSSAI Chat"}') ON DUPLICATE KEY UPDATE `model_code` = VALUES(`model_code`), `model_name` = VALUES(`model_name`), `provider_code` = VALUES(`provider_code`), `default_flag` = 1, `enabled_flag` = 1, `config_json` = VALUES(`config_json`), `deleted_flag` = 0;
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `ability_type` = 'TEXT' AND `model_id` <> 'm-tokenhub-text-bgssai-chat' AND `deleted_flag` = 0;

INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-tokenhub-image-bgssai', 'IMAGE', 'TENCENT_TOKENHUB', 'bgssai-image', 'BGSSAI Image', 'STANDARD', 'PER_CALL', 40, 1, 1, 180, 1, '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","public_alias":"bgssai-image","public_name":"BGSSAI Image"}') ON DUPLICATE KEY UPDATE `model_code` = VALUES(`model_code`), `model_name` = VALUES(`model_name`), `provider_code` = VALUES(`provider_code`), `default_flag` = 1, `enabled_flag` = 1, `config_json` = VALUES(`config_json`), `deleted_flag` = 0;
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `ability_type` = 'IMAGE' AND `model_id` <> 'm-tokenhub-image-bgssai' AND `deleted_flag` = 0;

INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-tokenhub-voice-bgssai-tts', 'VOICE', 'TENCENT_TOKENHUB', 'bgssai-tts', 'BGSSAI TTS', 'STANDARD', 'PER_CALL', 2, 1, 1, 180, 1, '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","default_voice_id":"alloy","public_alias":"bgssai-tts","public_name":"BGSSAI TTS"}') ON DUPLICATE KEY UPDATE `model_code` = VALUES(`model_code`), `model_name` = VALUES(`model_name`), `provider_code` = VALUES(`provider_code`), `default_flag` = 1, `enabled_flag` = 1, `config_json` = VALUES(`config_json`), `deleted_flag` = 0;
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `ability_type` = 'VOICE' AND `model_id` <> 'm-tokenhub-voice-bgssai-tts' AND `deleted_flag` = 0;

INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-tokenhub-video-bgssai', 'VIDEO', 'TENCENT_TOKENHUB', 'bgssai-video', 'BGSSAI Video', 'STANDARD', 'PER_SECOND', 20, 1, 1, 900, 1, '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","poll_interval_ms":3000,"public_alias":"bgssai-video","public_name":"BGSSAI Video"}') ON DUPLICATE KEY UPDATE `model_code` = 'bgssai-video', `provider_code` = 'TENCENT_TOKENHUB', `model_name` = 'BGSSAI Video', `enabled_flag` = 1, `deleted_flag` = 0, `timeout_sec` = 900, `default_flag` = 1, `config_json` = '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","poll_interval_ms":3000,"public_alias":"bgssai-video","public_name":"BGSSAI Video"}';
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `model_id` = 'm-tokenhub-video-minimax';
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `ability_type` = 'VIDEO' AND `deleted_flag` = 0 AND `model_code` = 'cogvideox';
UPDATE `ai_model_config` SET `default_flag` = 0 WHERE `ability_type` = 'VIDEO' AND `deleted_flag` = 0 AND NOT (`provider_code` = 'TENCENT_TOKENHUB' AND `model_code` = 'bgssai-video');
UPDATE `ai_model_config` SET `provider_code` = 'TENCENT_TOKENHUB', `model_name` = 'BGSSAI Video', `enabled_flag` = 1, `deleted_flag` = 0, `timeout_sec` = 900, `default_flag` = 1, `config_json` = '{"hub":"cn","secret_ref":"ai.tokenhub_cn.api_key","poll_interval_ms":3000,"public_alias":"bgssai-video","public_name":"BGSSAI Video"}' WHERE `ability_type` = 'VIDEO' AND `provider_code` = 'TENCENT_TOKENHUB' AND `model_code` = 'bgssai-video' AND `deleted_flag` = 0;

INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.openai_compat.api_key', '', '公司自建多模态网关 Bearer（https://chat.bgssai.com/v1）。不写真实密钥。', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('ai.image_to_video.api_key', '', '图生视频 Bearer。空值时改用公司键 ai.openai_compat.api_key 调用 https://chat.bgssai.com/i2v 。不写真实密钥。', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`), `description` = VALUES(`description`);
INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-compat-asr-whisper', 'ASR', 'OPENAI_COMPAT', 'whisper-1', '台词转写', 'STANDARD', 'PER_MINUTE', 2, 0, 1, 300, 1, '{"base_url":"https://chat.bgssai.com/v1","secret_ref":"ai.openai_compat.api_key","transcription_path":"/audio/transcriptions","language":"zh","public_alias":"bgssai-asr","public_name":"BGSSAI ASR"}') ON DUPLICATE KEY UPDATE `model_code` = VALUES(`model_code`), `model_name` = VALUES(`model_name`), `provider_code` = VALUES(`provider_code`), `enabled_flag` = 1, `timeout_sec` = VALUES(`timeout_sec`), `config_json` = VALUES(`config_json`), `deleted_flag` = 0;
INSERT INTO `ai_model_config` (`model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`, `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`, `timeout_sec`, `max_retry`, `config_json`) VALUES ('m-bgssai-image-to-video', 'VIDEO', 'IMAGE_CONDITIONED_VIDEO', 'bgssai-video-i2v', 'BGSSAI Video', 'STANDARD', 'PER_SECOND', 20, 0, 1, 14400, 0, '{"base_url":"https://chat.bgssai.com/i2v","secret_ref":"ai.image_to_video.api_key","public_alias":"bgssai-video","public_name":"BGSSAI Video","width":384,"height":256,"num_frames":25,"fps":8}') ON DUPLICATE KEY UPDATE `model_name` = 'BGSSAI Video', `price_unit` = 'PER_SECOND', `unit_price_cent` = 20, `timeout_sec` = 14400, `enabled_flag` = 1, `default_flag` = 0, `deleted_flag` = 0, `config_json` = '{"base_url":"https://chat.bgssai.com/i2v","secret_ref":"ai.image_to_video.api_key","public_alias":"bgssai-video","public_name":"BGSSAI Video","width":384,"height":256,"num_frames":25,"fps":8}';
