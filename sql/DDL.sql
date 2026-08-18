-- =====================================================================
-- DDL.sql — AI 短剧制作平台建库脚本（单一真值，仓库根 sql/DDL.sql）
-- =====================================================================
-- 管理端与用户端共用同一个库 bgssai_short。含登录基础表、订阅支付表与短剧制作闭环表。
--
-- 约定：
--   - 应用启动期不自动执行任何 DDL；schema 由部署方手动执行本文件建立（Standards §9）。
--   - 列命名保持现网风格（del_flag / create_by / create_time / update_by / update_time）。
--   - 新增 / 变更表结构后，用 MyBatis Generator 按新库表重新生成 model/mapper/XML。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `bgssai_short` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `bgssai_short`;

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

DROP TABLE IF EXISTS `sys_secret`;
CREATE TABLE `sys_secret` (
  `secret_key`   VARCHAR(128) NOT NULL                           COMMENT '密钥标识,对应代码中原配置键',
  `secret_value` TEXT         NOT NULL                           COMMENT '密钥值(明文存储)',
  `description`  VARCHAR(255) DEFAULT NULL                       COMMENT '说明',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1                 COMMENT '0=停用,1=启用',
  `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`   VARCHAR(64)  DEFAULT NULL                       COMMENT '最后修改人',
  PRIMARY KEY (`secret_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方密钥配置';

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE `oauth_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键id',
  `binding_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '业务ID',
  `platform` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'wechat/alipay/douyin/weibo',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '平台内用户唯一标识',
  `union_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '跨应用统一标识',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '绑定的本平台账号',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '第三方昵称快照',
  `avatar_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '第三方头像快照',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  `creator` varchar(64) NULL DEFAULT NULL COMMENT '创建者标识',
  `modifier` varchar(64) NULL DEFAULT NULL COMMENT '最后修改者标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_ob_binding_id`(`binding_id`) USING BTREE,
  UNIQUE INDEX `uniq_ob_platform_openid`(`platform`, `open_id`) USING BTREE,
  INDEX `idx_ob_user`(`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '三方平台账号绑定' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- mcp_access_token MCP 个人访问令牌（AI 客户端接入 /api/mcp）
-- 临时开发期明文落 token_hash 列；上生产前按 interim-plaintext 回滚闸门改回 SHA-256。
-- ----------------------------
DROP TABLE IF EXISTS `mcp_access_token`;
CREATE TABLE `mcp_access_token` (
  `id`                 BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`            VARCHAR(64)  NOT NULL                       COMMENT '归属用户 sys_user.user_id',
  `token_name`         VARCHAR(64)  NOT NULL                       COMMENT '令牌备注名',
  `token_hash`         VARCHAR(80)  NOT NULL                       COMMENT '令牌明文（临时开发期全量明文策略；列名沿用）',
  `last_used_time`     DATETIME     DEFAULT NULL                   COMMENT '最近使用时间',
  `del_flag`           TINYINT(1)   NOT NULL DEFAULT 0             COMMENT '软删 0/1',
  `create_by`          VARCHAR(64)  NOT NULL DEFAULT 'system'      COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          VARCHAR(64)  NOT NULL DEFAULT 'system'      COMMENT '更新人',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mcp_token_hash` (`token_hash`),
  KEY `idx_mcp_token_user` (`user_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='MCP 个人访问令牌';




-- ----------------------------
-- 订阅套餐 / 加量 credit / 支付订单 / 商户配置
-- ----------------------------
-- ----------------------------
-- subscription_plan
-- ----------------------------
DROP TABLE IF EXISTS subscription_plan;
CREATE TABLE subscription_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  plan_code VARCHAR(32) NOT NULL COMMENT '套餐编码',
  plan_name VARCHAR(64) NOT NULL COMMENT '套餐名称',
  price_cent INT NOT NULL DEFAULT 0 COMMENT '售价(分)',
  monthly_balance_cent INT NOT NULL DEFAULT 0 COMMENT '开通发放的周期额度(分)',
  monthly_limit_cent INT NOT NULL DEFAULT 0 COMMENT '本期用量上限(分)',

  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_subscription_plan_code (plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅套餐目录';
-- ----------------------------
-- user_subscription
-- ----------------------------
DROP TABLE IF EXISTS user_subscription;
CREATE TABLE user_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id VARCHAR(64) NOT NULL COMMENT '用户',
  plan_code VARCHAR(32) NOT NULL COMMENT '当前套餐',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active / expired',
  current_period_start DATETIME DEFAULT NULL COMMENT '本期开始',
  current_period_end DATETIME DEFAULT NULL COMMENT '本期结束',
  source VARCHAR(32) DEFAULT NULL COMMENT 'order / grant / free',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_subscription_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅';
-- ----------------------------
-- user_quota
-- ----------------------------
DROP TABLE IF EXISTS user_quota;
CREATE TABLE user_quota (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id VARCHAR(64) NOT NULL COMMENT '用户',
  balance_cent INT NOT NULL DEFAULT 0 COMMENT '本期套餐剩余(分)',
  monthly_limit_cent INT NOT NULL DEFAULT 0 COMMENT '本期用量上限(分)',
  extra_balance_cent INT NOT NULL DEFAULT 0 COMMENT '加量钱包(分)',
  month_spent_cent INT NOT NULL DEFAULT 0 COMMENT '本期已消耗(分)',
  month_reset_date DATE DEFAULT NULL COMMENT '月度重置日',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_quota_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户额度钱包';
-- ----------------------------
-- credit_pack
-- ----------------------------
DROP TABLE IF EXISTS credit_pack;
CREATE TABLE credit_pack (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  pack_code VARCHAR(32) NOT NULL COMMENT '加量包编码',
  name VARCHAR(64) NOT NULL COMMENT '名称',
  credit_cent INT NOT NULL COMMENT '入账额度(分)',
  price_cent INT NOT NULL COMMENT '售价(分)',
  
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_pack_code (pack_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加量包目录';
-- ----------------------------
-- payment_order
-- ----------------------------
DROP TABLE IF EXISTS payment_order;
CREATE TABLE payment_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
  user_id VARCHAR(64) NOT NULL COMMENT '下单用户',
  order_type VARCHAR(32) NOT NULL COMMENT 'subscription / credit_pack',
  biz_code VARCHAR(64) DEFAULT NULL COMMENT 'plan_code / pack_code',
  biz_name VARCHAR(128) DEFAULT NULL COMMENT '展示名',
  biz_ref_id VARCHAR(64) DEFAULT NULL COMMENT '业务关联 id',
  amount_cent INT NOT NULL COMMENT '实付金额(分)，服务端目录价',
  grant_cent INT NOT NULL DEFAULT 0 COMMENT '支付成功发放额度(分)',
  payment_channel VARCHAR(16) DEFAULT NULL COMMENT 'WECHAT / ALIPAY',
  pay_merchant_no VARCHAR(64) DEFAULT NULL COMMENT '收款商户号',
  status VARCHAR(16) NOT NULL DEFAULT 'created' COMMENT 'created / paid / closed',
  code_url VARCHAR(512) DEFAULT NULL COMMENT '扫码 URL',
  pay_form MEDIUMTEXT DEFAULT NULL COMMENT '渠道表单/扩展',
  channel_trade_no VARCHAR(64) DEFAULT NULL COMMENT '渠道交易号',
  paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
  expires_at DATETIME DEFAULT NULL COMMENT '过期时间',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_no (order_no),
  KEY idx_payment_order_user (user_id, status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';
-- ----------------------------
-- quota_change_log
-- ----------------------------
DROP TABLE IF EXISTS quota_change_log;
CREATE TABLE quota_change_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id VARCHAR(64) NOT NULL COMMENT '被调整用户',
  operator_user_id VARCHAR(64) DEFAULT NULL COMMENT '操作者',
  change_type VARCHAR(32) NOT NULL COMMENT 'adjust / consume / grant_plan / grant_pack',
  balance_delta_cent INT NOT NULL DEFAULT 0 COMMENT '周期余额变化',
  extra_delta_cent INT NOT NULL DEFAULT 0 COMMENT '加量余额变化',
  balance_after_cent INT DEFAULT NULL COMMENT '变化后周期余额',
  extra_after_cent INT DEFAULT NULL COMMENT '变化后加量余额',
  monthly_limit_cent INT DEFAULT NULL COMMENT '调整后月度上限',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_quota_change_user (user_id, gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='额度变更流水';
-- ----------------------------
-- wechat_pay_config
-- ----------------------------
DROP TABLE IF EXISTS wechat_pay_config;
CREATE TABLE wechat_pay_config (
  id                      BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键',
  merchant_name           VARCHAR(128) NOT NULL DEFAULT ''                    COMMENT '商户名称',
  mch_id                  VARCHAR(64)  NOT NULL                               COMMENT '微信支付商户号',
  app_id                  VARCHAR(64)  DEFAULT NULL                           COMMENT 'AppID',
  key_mode                VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC_KEY'          COMMENT 'PUBLIC_KEY / PLATFORM_CERT',
  merchant_serial_no      VARCHAR(128) DEFAULT NULL                           COMMENT '商户 API 证书序列号',
  mch_private_key         MEDIUMTEXT   DEFAULT NULL                           COMMENT '商户 API 私钥',
  apiclient_cert          MEDIUMTEXT   DEFAULT NULL                           COMMENT '商户 API 证书',
  mch_cert_p12            MEDIUMTEXT   DEFAULT NULL                           COMMENT '商户证书 p12 base64',
  wechat_public_key_id    VARCHAR(128) DEFAULT NULL                           COMMENT '微信支付公钥 ID',
  wechat_public_key       MEDIUMTEXT   DEFAULT NULL                           COMMENT '微信支付公钥 PEM',
  platform_cert_serial_no VARCHAR(128) DEFAULT NULL                           COMMENT '平台证书序列号',
  platform_cert           MEDIUMTEXT   DEFAULT NULL                           COMMENT '平台证书 PEM',
  api_v3_key              VARCHAR(128) DEFAULT NULL                           COMMENT 'APIv3 密钥',
  api_v2_key              VARCHAR(128) DEFAULT NULL                           COMMENT 'APIv2 密钥',
  jsapi_auth_dir          VARCHAR(255) DEFAULT NULL                           COMMENT 'JSAPI 授权目录',
  notify_url              VARCHAR(512) DEFAULT NULL                           COMMENT 'Native 异步通知地址',
  env_mode                VARCHAR(16)  NOT NULL DEFAULT 'PRODUCTION'          COMMENT 'SANDBOX / PRODUCTION',
  last_test_status        VARCHAR(16)  NOT NULL DEFAULT 'UNTESTED'            COMMENT 'UNTESTED / TESTING / OK / FAILED',
  last_test_at            DATETIME     DEFAULT NULL                           COMMENT '最近连通测试时间',
  last_test_error         VARCHAR(512) DEFAULT NULL                           COMMENT '最近连通测试失败摘要',
  is_enabled              TINYINT(1)   NOT NULL DEFAULT 1                     COMMENT '是否启用',
  is_collection_active    TINYINT(1)   NOT NULL DEFAULT 0                     COMMENT '是否当前收款商户',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wechat_pay_config_mch_id (mch_id),
  KEY idx_wechat_pay_config_collect (is_collection_active, is_enabled, is_delete),
  KEY idx_wechat_pay_config_routing (mch_id, env_mode, is_enabled, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付商户配置';
-- ----------------------------
-- alipay_config
-- ----------------------------
DROP TABLE IF EXISTS alipay_config;
CREATE TABLE alipay_config (
  id                     BIGINT       NOT NULL AUTO_INCREMENT                 COMMENT '主键',
  merchant_name          VARCHAR(128) NOT NULL DEFAULT ''                     COMMENT '商户名称',
  app_id                 VARCHAR(64)  NOT NULL                                COMMENT '支付宝 AppID',
  key_mode               VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC_KEY'           COMMENT 'PUBLIC_KEY / CERT',
  app_private_key        MEDIUMTEXT   DEFAULT NULL                            COMMENT '应用私钥',
  alipay_public_key      MEDIUMTEXT   DEFAULT NULL                            COMMENT '支付宝公钥',
  app_cert_public_key    MEDIUMTEXT   DEFAULT NULL                            COMMENT '应用公钥证书',
  alipay_cert_public_key MEDIUMTEXT   DEFAULT NULL                            COMMENT '支付宝公钥证书',
  alipay_root_cert       MEDIUMTEXT   DEFAULT NULL                            COMMENT '支付宝根证书',
  app_cert_sn            VARCHAR(128) DEFAULT NULL                            COMMENT '应用证书 SN',
  alipay_root_cert_sn    VARCHAR(255) DEFAULT NULL                            COMMENT '根证书 SN',
  sign_type              VARCHAR(16)  NOT NULL DEFAULT 'RSA2'                 COMMENT '签名算法',
  charset                VARCHAR(16)  NOT NULL DEFAULT 'utf-8'                COMMENT '字符集',
  gateway_url            VARCHAR(255) NOT NULL DEFAULT 'https://openapi.alipay.com/gateway.do' COMMENT '网关',
  notify_url             VARCHAR(512) DEFAULT NULL                            COMMENT '异步通知地址',
  return_url             VARCHAR(512) DEFAULT NULL                            COMMENT '同步跳转',
  env_mode               VARCHAR(16)  NOT NULL DEFAULT 'PRODUCTION'           COMMENT 'SANDBOX / PRODUCTION',
  last_test_status       VARCHAR(16)  NOT NULL DEFAULT 'UNTESTED'             COMMENT '连通测试状态',
  last_test_at           DATETIME     DEFAULT NULL                            COMMENT '最近连通测试时间',
  last_test_error        VARCHAR(512) DEFAULT NULL                            COMMENT '最近连通测试失败摘要',
  is_enabled             TINYINT(1)   NOT NULL DEFAULT 1                      COMMENT '是否启用',
  is_collection_active   TINYINT(1)   NOT NULL DEFAULT 0                      COMMENT '是否当前收款商户',
  is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_alipay_config_app_id (app_id),
  KEY idx_alipay_config_collect (is_collection_active, is_enabled, is_delete),
  KEY idx_alipay_config_routing (app_id, env_mode, is_enabled, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝商户配置';

-- ----------------------------
-- 短剧制作闭环 MVP
-- ----------------------------
DROP TABLE IF EXISTS `short_work`;
CREATE TABLE `short_work` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `work_id` VARCHAR(64) NOT NULL COMMENT '作品ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '创作者',
  `work_title` VARCHAR(255) NOT NULL COMMENT '剧名',
  `one_line_pitch` VARCHAR(512) NOT NULL COMMENT '一句话梗概',
  `genre` VARCHAR(32) NOT NULL DEFAULT 'REVENGE',
  `work_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `episode_count` INT NOT NULL DEFAULT 24,
  `episode_duration_sec` INT NOT NULL DEFAULT 90,
  `visual_style` VARCHAR(32) NOT NULL DEFAULT 'MODERN_URBAN',
  `aspect_ratio` VARCHAR(32) NOT NULL DEFAULT 'VERTICAL_9_16',
  `cover_asset_id` VARCHAR(64) DEFAULT NULL,
  `cost_budget_cent` INT DEFAULT NULL,
  `cost_spent_cent` INT NOT NULL DEFAULT 0,
  `skip_level_check_flag` TINYINT NOT NULL DEFAULT 0,
  `quality_threshold_score` INT DEFAULT NULL,
  `outline_json` LONGTEXT,
  `compose_setting_json` LONGTEXT,
  `archived_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_work_work_id` (`work_id`),
  KEY `idx_short_work_user_status` (`user_id`, `work_status`, `updated_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧作品';

DROP TABLE IF EXISTS `short_episode`;
CREATE TABLE `short_episode` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `episode_no` INT NOT NULL,
  `episode_title` VARCHAR(255) DEFAULT NULL,
  `episode_synopsis` VARCHAR(1024) DEFAULT NULL,
  `ending_hook` VARCHAR(512) DEFAULT NULL,
  `script_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `storyboard_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `dubbing_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `compose_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
  `film_asset_id` VARCHAR(64) DEFAULT NULL,
  `film_duration_sec` INT DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_episode_id` (`episode_id`),
  UNIQUE KEY `uk_short_episode_no` (`work_id`, `episode_no`, `deleted_flag`),
  KEY `idx_short_episode_work` (`work_id`, `episode_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧剧集';

DROP TABLE IF EXISTS `short_script`;
CREATE TABLE `short_script` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `script_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `script_version` INT NOT NULL DEFAULT 1,
  `script_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `scene_json` LONGTEXT,
  `estimated_total_sec` INT DEFAULT NULL,
  `rhythm_json` LONGTEXT,
  `total_score` INT DEFAULT NULL,
  `force_ready_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_script_id` (`script_id`),
  UNIQUE KEY `uk_short_script_episode` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单集剧本';

DROP TABLE IF EXISTS `short_script_version`;
CREATE TABLE `short_script_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_id` VARCHAR(64) NOT NULL,
  `script_id` VARCHAR(64) NOT NULL,
  `script_version` INT NOT NULL,
  `scene_json` LONGTEXT,
  `total_score` INT DEFAULT NULL,
  `change_summary` VARCHAR(255) DEFAULT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_script_version_id` (`version_id`),
  KEY `idx_short_script_version` (`script_id`, `script_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧本历史版本';

DROP TABLE IF EXISTS `short_character`;
CREATE TABLE `short_character` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `character_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `character_name` VARCHAR(64) NOT NULL,
  `character_role` VARCHAR(32) DEFAULT NULL,
  `card_status` VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
  `appearance_prompt` VARCHAR(1024) DEFAULT NULL,
  `consistency_seed` BIGINT DEFAULT NULL,
  `ref_image_asset_id` VARCHAR(64) DEFAULT NULL,
  `default_outfit_code` VARCHAR(64) DEFAULT NULL,
  `outfit_json` LONGTEXT,
  `voice_id` VARCHAR(64) DEFAULT NULL,
  `voice_speed` VARCHAR(16) DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_character_id` (`character_id`),
  KEY `idx_short_character_work` (`work_id`, `deleted_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色卡';

DROP TABLE IF EXISTS `short_scene_card`;
CREATE TABLE `short_scene_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scene_card_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `card_type` VARCHAR(16) NOT NULL DEFAULT 'SCENE',
  `card_name` VARCHAR(128) NOT NULL,
  `card_status` VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
  `appearance_prompt` VARCHAR(1024) DEFAULT NULL,
  `ref_image_asset_id` VARCHAR(64) DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_scene_card_id` (`scene_card_id`),
  KEY `idx_short_scene_work` (`work_id`, `deleted_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景卡';

DROP TABLE IF EXISTS `short_shot`;
CREATE TABLE `short_shot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shot_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `shot_no` INT NOT NULL,
  `scene_no` INT DEFAULT NULL,
  `shot_size` VARCHAR(32) DEFAULT 'MEDIUM',
  `camera_move` VARCHAR(32) DEFAULT 'FIXED',
  `shot_duration_sec` INT DEFAULT 3,
  `shot_desc` VARCHAR(1024) DEFAULT NULL,
  `dialogue_text` VARCHAR(1024) DEFAULT NULL,
  `emotion` VARCHAR(32) DEFAULT NULL,
  `lighting` VARCHAR(64) DEFAULT NULL,
  `character_ref_json` LONGTEXT,
  `scene_card_id` VARCHAR(64) DEFAULT NULL,
  `extra_prompt` VARCHAR(1024) DEFAULT NULL,
  `image_prompt` LONGTEXT,
  `previz_text` LONGTEXT,
  `shot_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `blocked_flag` TINYINT NOT NULL DEFAULT 0,
  `risk_level` VARCHAR(16) DEFAULT 'PASS',
  `first_frame_asset_id` VARCHAR(64) DEFAULT NULL,
  `video_asset_id` VARCHAR(64) DEFAULT NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `cost_spent_cent` INT NOT NULL DEFAULT 0,
  `shot_version` INT NOT NULL DEFAULT 1,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_shot_id` (`shot_id`),
  KEY `idx_short_shot_episode` (`episode_id`, `shot_no`),
  KEY `idx_short_shot_user_status` (`user_id`, `shot_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='镜头';

DROP TABLE IF EXISTS `short_ai_task`;
CREATE TABLE `short_ai_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL,
  `batch_id` VARCHAR(64) DEFAULT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) DEFAULT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `task_type` VARCHAR(16) NOT NULL,
  `task_status` VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
  `target_type` VARCHAR(32) DEFAULT NULL,
  `target_id` VARCHAR(64) DEFAULT NULL,
  `model_code` VARCHAR(64) DEFAULT 'mock',
  `request_json` LONGTEXT,
  `result_json` LONGTEXT,
  `progress_percent` INT NOT NULL DEFAULT 0,
  `retry_count` INT NOT NULL DEFAULT 0,
  `fail_reason` VARCHAR(512) DEFAULT NULL,
  `fail_reason_code` VARCHAR(32) DEFAULT NULL,
  `auto_next_flag` TINYINT NOT NULL DEFAULT 0,
  `cost_cent` INT NOT NULL DEFAULT 0,
  `queued_time` DATETIME DEFAULT NULL,
  `started_time` DATETIME DEFAULT NULL,
  `finished_time` DATETIME DEFAULT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_ai_task_id` (`task_id`),
  KEY `idx_short_ai_task_user` (`user_id`, `task_status`),
  KEY `idx_short_ai_task_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务';

DROP TABLE IF EXISTS `short_asset`;
CREATE TABLE `short_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `asset_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) DEFAULT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `shot_id` VARCHAR(64) DEFAULT NULL,
  `asset_type` VARCHAR(16) NOT NULL,
  `storage_key` LONGTEXT,
  `file_url` VARCHAR(1024) DEFAULT NULL,
  `file_size_byte` INT DEFAULT NULL,
  `duration_sec` INT DEFAULT NULL,
  `width` INT DEFAULT NULL,
  `height` INT DEFAULT NULL,
  `source_task_id` VARCHAR(64) DEFAULT NULL,
  `selected_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_asset_id` (`asset_id`),
  KEY `idx_short_asset_shot` (`shot_id`, `asset_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材';

DROP TABLE IF EXISTS `short_film`;
CREATE TABLE `short_film` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `film_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `film_asset_id` VARCHAR(64) DEFAULT NULL,
  `film_duration_sec` INT DEFAULT NULL,
  `compose_status` VARCHAR(32) NOT NULL DEFAULT 'NONE',
  `expired_flag` TINYINT NOT NULL DEFAULT 0,
  `subtitle_stale_flag` TINYINT NOT NULL DEFAULT 0,
  `compose_task_id` VARCHAR(64) DEFAULT NULL,
  `composed_time` DATETIME DEFAULT NULL,
  `export_json` LONGTEXT,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_film_id` (`film_id`),
  KEY `idx_short_film_episode` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成片';

DROP TABLE IF EXISTS `short_review_record`;
CREATE TABLE `short_review_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `review_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) NOT NULL,
  `episode_id` VARCHAR(64) NOT NULL,
  `creator_user_id` VARCHAR(64) NOT NULL,
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `risk_level` VARCHAR(16) DEFAULT 'PASS',
  `reviewer_user_id` VARCHAR(64) DEFAULT NULL,
  `review_comment` VARCHAR(1024) DEFAULT NULL,
  `reject_item_json` LONGTEXT,
  `submitted_time` DATETIME DEFAULT NULL,
  `reviewed_time` DATETIME DEFAULT NULL,
  `deleted_flag` TINYINT NOT NULL DEFAULT 0,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_review_id` (`review_id`),
  KEY `idx_short_review_status` (`review_status`, `submitted_time`),
  KEY `idx_short_review_episode` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核';

DROP TABLE IF EXISTS `short_compliance_hit`;
CREATE TABLE `short_compliance_hit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hit_id` VARCHAR(64) NOT NULL,
  `work_id` VARCHAR(64) DEFAULT NULL,
  `episode_id` VARCHAR(64) DEFAULT NULL,
  `shot_id` VARCHAR(64) DEFAULT NULL,
  `check_scope` VARCHAR(16) NOT NULL,
  `risk_level` VARCHAR(16) NOT NULL,
  `risk_category` VARCHAR(32) NOT NULL,
  `hit_text` VARCHAR(512) DEFAULT NULL,
  `suggestion` VARCHAR(512) DEFAULT NULL,
  `scene_no` INT DEFAULT NULL,
  `line_no` INT DEFAULT NULL,
  `shot_no` INT DEFAULT NULL,
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_compliance_hit_id` (`hit_id`),
  KEY `idx_short_compliance_episode` (`episode_id`, `check_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规预检命中';
