-- =====================================================================
-- DDL.sql — AI 短剧制作平台建库脚本（单一真值，仓库根 sql/DDL.sql）
-- =====================================================================
-- 管理端与用户端共用同一个库 bgssai_short。当前仅含两张基础表：
--   sys_user   用户表（登录鉴权）
--   login_log  登录记录表（示例分页接口的数据源）
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
