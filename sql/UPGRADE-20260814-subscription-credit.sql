-- 2026-08-14 订阅套餐 + 加量包 + 支付（新库请直接跑 DDL.sql）
USE `bgssai_short`;

-- ----------------------------
-- 订阅套餐 / 加量 credit / 支付订单 / 商户配置
-- ----------------------------
-- ----------------------------
-- subscription_plan
-- ----------------------------
-- existing rows kept subscription_plan;
CREATE TABLE IF NOT EXISTS subscription_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  plan_code VARCHAR(32) NOT NULL COMMENT '套餐编码',
  plan_name VARCHAR(64) NOT NULL COMMENT '套餐名称',
  price_cent INT NOT NULL DEFAULT 0 COMMENT '售价(分)',
  monthly_balance_cent INT NOT NULL DEFAULT 0 COMMENT '开通发放的周期额度(分)',
  monthly_limit_cent INT NOT NULL DEFAULT 0 COMMENT '本期用量上限(分)',

  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
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
-- existing rows kept user_subscription;
CREATE TABLE IF NOT EXISTS user_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id VARCHAR(64) NOT NULL COMMENT '用户',
  plan_code VARCHAR(32) NOT NULL COMMENT '当前套餐',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active / expired',
  current_period_start DATETIME DEFAULT NULL COMMENT '本期开始',
  current_period_end DATETIME DEFAULT NULL COMMENT '本期结束',
  source VARCHAR(32) DEFAULT NULL COMMENT 'order / grant / free',
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
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
-- existing rows kept user_quota;
CREATE TABLE IF NOT EXISTS user_quota (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id VARCHAR(64) NOT NULL COMMENT '用户',
  balance_cent INT NOT NULL DEFAULT 0 COMMENT '本期套餐剩余(分)',
  monthly_limit_cent INT NOT NULL DEFAULT 0 COMMENT '本期用量上限(分)',
  extra_balance_cent INT NOT NULL DEFAULT 0 COMMENT '加量钱包(分)',
  month_spent_cent INT NOT NULL DEFAULT 0 COMMENT '本期已消耗(分)',
  month_reset_date DATE DEFAULT NULL COMMENT '月度重置日',
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
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
-- existing rows kept credit_pack;
CREATE TABLE IF NOT EXISTS credit_pack (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  pack_code VARCHAR(32) NOT NULL COMMENT '加量包编码',
  name VARCHAR(64) NOT NULL COMMENT '名称',
  credit_cent INT NOT NULL COMMENT '入账额度(分)',
  price_cent INT NOT NULL COMMENT '售价(分)',
  
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
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
-- existing rows kept payment_order;
CREATE TABLE IF NOT EXISTS payment_order (
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
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_no (order_no),
  KEY idx_payment_order_user (user_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';
-- ----------------------------
-- quota_change_log
-- ----------------------------
-- existing rows kept quota_change_log;
CREATE TABLE IF NOT EXISTS quota_change_log (
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
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
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
-- existing rows kept wechat_pay_config;
CREATE TABLE IF NOT EXISTS wechat_pay_config (
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
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wechat_pay_config_mch_id (mch_id),
  KEY idx_wechat_pay_config_collect (is_collection_active, is_enabled, del_flag),
  KEY idx_wechat_pay_config_routing (mch_id, env_mode, is_enabled, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付商户配置';
-- ----------------------------
-- alipay_config
-- ----------------------------
-- existing rows kept alipay_config;
CREATE TABLE IF NOT EXISTS alipay_config (
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
  del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
  creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
  modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_alipay_config_app_id (app_id),
  KEY idx_alipay_config_collect (is_collection_active, is_enabled, del_flag),
  KEY idx_alipay_config_routing (app_id, env_mode, is_enabled, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝商户配置';
