-- =====================================================================
-- UPGRADE-20260826-full-features.sql
-- AI 模型/用量、配音字幕、预演、剧情图谱、质量流水线、三问体验、OBS 素材列
-- 存量库执行；全新库请直接用更新后的 DDL.sql
-- =====================================================================
USE `bgssai_short`;
SET NAMES utf8mb4;

-- ---------- ALTER 已有表 ----------
ALTER TABLE `short_work`
  ADD COLUMN `narrator_voice_id` VARCHAR(64) DEFAULT NULL AFTER `compose_setting_json`,
  ADD COLUMN `subtitle_style_json` LONGTEXT AFTER `narrator_voice_id`,
  ADD COLUMN `template_id` VARCHAR(64) DEFAULT NULL AFTER `subtitle_style_json`,
  ADD COLUMN `template_override_json` LONGTEXT AFTER `template_id`,
  ADD COLUMN `overall_quality_score` INT DEFAULT NULL AFTER `quality_threshold_score`;

ALTER TABLE `short_shot`
  ADD COLUMN `blocking_json` LONGTEXT AFTER `previz_text`,
  ADD COLUMN `axis_flag` TINYINT NOT NULL DEFAULT 0 AFTER `blocking_json`,
  ADD COLUMN `previz_level` VARCHAR(32) DEFAULT 'NONE' AFTER `axis_flag`,
  ADD COLUMN `stale_flag` TINYINT NOT NULL DEFAULT 0 AFTER `previz_level`,
  ADD COLUMN `prompt_version` INT NOT NULL DEFAULT 1 AFTER `stale_flag`;

ALTER TABLE `short_asset`
  ADD COLUMN `draft_flag` TINYINT NOT NULL DEFAULT 0 AFTER `selected_flag`,
  ADD COLUMN `mime_type` VARCHAR(128) DEFAULT NULL AFTER `asset_type`;

-- ---------- AI 能力与用量 ----------
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_id` VARCHAR(64) NOT NULL,
  `ability_type` VARCHAR(16) NOT NULL,
  `provider_code` VARCHAR(64) NOT NULL,
  `model_code` VARCHAR(128) NOT NULL,
  `model_name` VARCHAR(255) NOT NULL,
  `quality_level` VARCHAR(32) DEFAULT 'STANDARD',
  `price_unit` VARCHAR(32) NOT NULL DEFAULT 'PER_CALL',
  `unit_price_cent` INT NOT NULL DEFAULT 0,
  `default_flag` TINYINT NOT NULL DEFAULT 0,
  `enabled_flag` TINYINT NOT NULL DEFAULT 1,
  `timeout_sec` INT NOT NULL DEFAULT 120,
  `max_retry` INT NOT NULL DEFAULT 1,
  `config_json` LONGTEXT,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_id` (`model_id`),
  UNIQUE KEY `uk_ai_model_code` (`ability_type`, `model_code`, `deleted_flag`),
  KEY `idx_ai_model_ability` (`ability_type`, `enabled_flag`, `default_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置';

CREATE TABLE IF NOT EXISTS `ai_usage_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) DEFAULT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `shot_id` VARCHAR(64) DEFAULT NULL,
  `task_id` VARCHAR(64) DEFAULT NULL,
  `ability_type` VARCHAR(16) NOT NULL,
  `model_code` VARCHAR(128) DEFAULT NULL,
  `quality_level` VARCHAR(32) DEFAULT NULL,
  `price_unit` VARCHAR(32) DEFAULT NULL,
  `usage_amount` INT NOT NULL DEFAULT 1,
  `unit_price_cent` INT NOT NULL DEFAULT 0,
  `cost_cent` INT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_usage_record_id` (`record_id`),
  KEY `idx_ai_usage_user_time` (`user_id`, `created_time`),
  KEY `idx_ai_usage_work` (`work_id`),
  KEY `idx_ai_usage_episode` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用量流水';

CREATE TABLE IF NOT EXISTS `recharge_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `apply_amount_cent` INT NOT NULL,
  `apply_status` VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  `user_remark` VARCHAR(512) DEFAULT NULL,
  `operator_user_id` VARCHAR(64) DEFAULT NULL,
  `operator_remark` VARCHAR(512) DEFAULT NULL,
  `handled_time` DATETIME DEFAULT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_apply_id` (`apply_id`),
  KEY `idx_recharge_user_status` (`user_id`, `apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值申请';

-- ---------- 配音字幕 ----------
CREATE TABLE IF NOT EXISTS `short_dubbing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dubbing_id` VARCHAR(64) NOT NULL,
  `shot_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `line_no` INT NOT NULL DEFAULT 1,
  `character_id` VARCHAR(64) DEFAULT NULL,
  `voice_id` VARCHAR(64) DEFAULT NULL,
  `voice_speed` VARCHAR(32) DEFAULT NULL,
  `emotion` VARCHAR(64) DEFAULT NULL,
  `line_text` VARCHAR(1024) DEFAULT NULL,
  `audio_asset_id` VARCHAR(64) DEFAULT NULL,
  `audio_duration_sec` INT DEFAULT NULL,
  `align_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `align_gap_sec` DECIMAL(8,2) DEFAULT NULL,
  `auto_fixed_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_dubbing_id` (`dubbing_id`),
  KEY `idx_short_dubbing_ep_shot` (`episode_id`, `shot_id`, `line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配音';

CREATE TABLE IF NOT EXISTS `short_subtitle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subtitle_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `subtitle_no` INT NOT NULL,
  `start_ms` INT NOT NULL DEFAULT 0,
  `end_ms` INT NOT NULL DEFAULT 0,
  `subtitle_text` VARCHAR(512) NOT NULL,
  `shot_id` VARCHAR(64) DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_subtitle_id` (`subtitle_id`),
  KEY `idx_short_subtitle_ep` (`episode_id`, `subtitle_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字幕';

-- ---------- 预演 / 连贯性 ----------
CREATE TABLE IF NOT EXISTS `short_shot_previz` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `previz_id` VARCHAR(64) NOT NULL,
  `shot_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `previz_level` VARCHAR(32) NOT NULL,
  `previz_text` LONGTEXT,
  `draft_asset_id` VARCHAR(64) DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_shot_previz_id` (`previz_id`),
  KEY `idx_short_shot_previz_ep` (`episode_id`, `shot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='镜头预演';

CREATE TABLE IF NOT EXISTS `short_prompt_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_id` VARCHAR(64) NOT NULL,
  `shot_id` VARCHAR(64) NOT NULL,
  `prompt_version` INT NOT NULL,
  `image_prompt` LONGTEXT,
  `change_summary` VARCHAR(512) DEFAULT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_prompt_version_id` (`version_id`),
  KEY `idx_short_prompt_shot` (`shot_id`, `prompt_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词版本';

CREATE TABLE IF NOT EXISTS `short_continuity_issue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `issue_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `issue_type` VARCHAR(32) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'WARN',
  `issue_desc` VARCHAR(1024) NOT NULL,
  `shot_id` VARCHAR(64) DEFAULT NULL,
  `related_shot_id` VARCHAR(64) DEFAULT NULL,
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_continuity_issue_id` (`issue_id`),
  KEY `idx_short_continuity_work` (`work_id`, `handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连贯性问题';

-- ---------- 剧情图谱 ----------
CREATE TABLE IF NOT EXISTS `short_story_map` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `map_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `map_scope` VARCHAR(32) NOT NULL,
  `map_json` LONGTEXT,
  `draft_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_story_map_id` (`map_id`),
  KEY `idx_short_story_map_work` (`work_id`, `episode_id`, `map_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧情图谱';

CREATE TABLE IF NOT EXISTS `short_character_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `relation_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `from_character_id` VARCHAR(64) NOT NULL,
  `to_character_id` VARCHAR(64) NOT NULL,
  `relation_type` VARCHAR(32) NOT NULL,
  `as_of_episode_no` INT DEFAULT NULL,
  `relation_desc` VARCHAR(512) DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_character_relation_id` (`relation_id`),
  KEY `idx_short_char_rel_work` (`work_id`, `from_character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物关系';

-- ---------- 质量流水线 ----------
CREATE TABLE IF NOT EXISTS `short_reference_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` VARCHAR(64) NOT NULL,
  `template_name` VARCHAR(255) NOT NULL,
  `genre` VARCHAR(32) NOT NULL,
  `hook_type_json` LONGTEXT,
  `beat_sheet_json` LONGTEXT,
  `turning_point_json` LONGTEXT,
  `taboo_list_json` LONGTEXT,
  `enabled_flag` TINYINT NOT NULL DEFAULT 1,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_reference_template_id` (`template_id`),
  KEY `idx_short_ref_tpl_genre` (`genre`, `enabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构模板';

CREATE TABLE IF NOT EXISTS `short_quality_gate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `gate_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `gate_code` VARCHAR(32) NOT NULL,
  `gate_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `score` INT DEFAULT NULL,
  `threshold_score` INT DEFAULT NULL,
  `force_pass_flag` TINYINT NOT NULL DEFAULT 0,
  `force_pass_reason` VARCHAR(512) DEFAULT NULL,
  `result_json` LONGTEXT,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_quality_gate_id` (`gate_id`),
  KEY `idx_short_quality_gate_ep` (`episode_id`, `gate_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量门';

CREATE TABLE IF NOT EXISTS `short_quality_issue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `issue_id` VARCHAR(64) NOT NULL,
  `gate_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `dimension` VARCHAR(64) DEFAULT NULL,
  `issue_desc` VARCHAR(1024) NOT NULL,
  `fix_hint` VARCHAR(1024) DEFAULT NULL,
  `target_type` VARCHAR(32) DEFAULT NULL,
  `target_id` VARCHAR(64) DEFAULT NULL,
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_quality_issue_id` (`issue_id`),
  KEY `idx_short_quality_issue_gate` (`gate_id`, `handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量问题';

CREATE TABLE IF NOT EXISTS `short_quality_threshold_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `log_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `old_threshold_score` INT DEFAULT NULL,
  `new_threshold_score` INT NOT NULL,
  `adjust_reason` VARCHAR(512) NOT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_quality_threshold_log_id` (`log_id`),
  KEY `idx_short_q_threshold_work` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量阈值调整日志';

CREATE TABLE IF NOT EXISTS `short_variant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `variant_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `variant_node` VARCHAR(64) NOT NULL,
  `target_id` VARCHAR(64) DEFAULT NULL,
  `variant_no` INT NOT NULL DEFAULT 1,
  `content_json` LONGTEXT,
  `score` INT DEFAULT NULL,
  `selected_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_variant_id` (`variant_id`),
  KEY `idx_short_variant_node` (`variant_node`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多方案变体';

-- ---------- 三问：伏笔与集间缺口 ----------
CREATE TABLE IF NOT EXISTS `short_foreshadow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `foreshadow_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `plant_episode_no` INT DEFAULT NULL,
  `payoff_episode_no` INT DEFAULT NULL,
  `foreshadow_desc` VARCHAR(1024) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_foreshadow_id` (`foreshadow_id`),
  KEY `idx_short_foreshadow_work` (`work_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='伏笔';

CREATE TABLE IF NOT EXISTS `short_episode_gap` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `gap_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `from_episode_id` VARCHAR(64) DEFAULT NULL,
  `to_episode_id` VARCHAR(64) DEFAULT NULL,
  `gap_desc` VARCHAR(1024) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'WARN',
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_episode_gap_id` (`gap_id`),
  KEY `idx_short_episode_gap_work` (`work_id`, `handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集间缺口';

-- 密钥占位（enabled=0，需运维写入真实值后启用）
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`)
VALUES
  ('ai.openai_compat.api_key', '', 'OpenAI兼容网关 API Key', 0, 'upgrade-20260826'),
  ('obs.access_key', '', '华为云 OBS AK', 0, 'upgrade-20260826'),
  ('obs.secret_key', '', '华为云 OBS SK', 0, 'upgrade-20260826')
ON DUPLICATE KEY UPDATE `description`=VALUES(`description`);

-- 默认 MOCK 模型（无外部密钥时可跑通计费与路由）
INSERT INTO `ai_model_config` (
  `model_id`, `ability_type`, `provider_code`, `model_code`, `model_name`,
  `quality_level`, `price_unit`, `unit_price_cent`, `default_flag`, `enabled_flag`,
  `timeout_sec`, `max_retry`, `config_json`
) VALUES
 ('m-text-mock', 'TEXT', 'MOCK', 'mock-text', '模拟文本', 'STANDARD', 'PER_CALL', 15, 1, 1, 60, 1, '{}'),
 ('m-image-mock', 'IMAGE', 'MOCK', 'mock-image', '模拟图片', 'STANDARD', 'PER_CALL', 50, 1, 1, 120, 1, '{}'),
 ('m-video-mock', 'VIDEO', 'MOCK', 'mock-video', '模拟视频', 'STANDARD', 'PER_CALL', 150, 1, 1, 600, 1, '{}'),
 ('m-voice-mock', 'VOICE', 'MOCK', 'mock-voice', '模拟配音', 'STANDARD', 'PER_CALL', 20, 1, 1, 120, 1, '{}'),
 ('m-compose-mock', 'COMPOSE', 'MOCK', 'mock-compose', '模拟合成', 'STANDARD', 'PER_CALL', 80, 1, 1, 300, 1, '{}')
ON DUPLICATE KEY UPDATE `updated_time`=CURRENT_TIMESTAMP;
