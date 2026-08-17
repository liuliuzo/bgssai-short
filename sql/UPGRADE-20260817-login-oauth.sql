-- 存量库：登录页微信 / 支付宝 / 抖音真实凭证初始化（LoginOAuthSecrets）。
-- 新库随 DML 一起执行；已有库单独跑本文件。行已存在且非空时不覆盖。
USE `bgssai_short`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_secret` (
  `secret_key`   VARCHAR(128) NOT NULL                           COMMENT '密钥标识,对应代码中原配置键',
  `secret_value` TEXT         NOT NULL                           COMMENT '密钥值(明文存储)',
  `description`  VARCHAR(255) DEFAULT NULL                       COMMENT '说明',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1                 COMMENT '0=停用,1=启用',
  `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`   VARCHAR(64)  DEFAULT NULL                       COMMENT '最后修改人',
  PRIMARY KEY (`secret_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方密钥配置';

-- ##################################################################
-- STEP LOGIN-OAUTH-1: 登录页微信 / 支付宝 / 抖音凭证
-- ##################################################################
-- DEV 清库后由此段恢复 admin 社交登录凭证，无需再手工录入。
-- 用户端 POST /bgssai/login/oauth/authorize-url 读这些键（LoginOAuthSecrets）。
-- 行已存在且值非空时不覆盖。一行一条 INSERT。
-- ------------------------------------------------------------------
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-id', 'wx496f923c7d4ac64e', '登录页微信开放平台网站应用 AppID(snsapi_login)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-secret', '812fbfc6570dda328805ce87d8e7fbd4', '登录页微信开放平台网站应用 AppSecret', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.app-id', '', '登录页支付宝网页授权 AppID(auth_user)；控制台应用尚未创建，清库后仍为空', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.app-private-key', 'MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDkgP1t9nQiVaLMNMdJzwcS6FTAF3H1ytjTTXxfYVofZZjJjgXhkdbdB2/TgyM8zFxurOGplAQj+P6Oj2ofkV8qiegxGJDWhtGb+YHCdlVGJF+CRrhrekA3I58hbwHAHlHr2S8rjFw0OnaLJ6QajzCaQFcqfVqt1bhHdgKnyS4Ql76aBzUiDU2cCkSwwY/pBGU+LtISIQn2fe6IhYmDvuEdp41ixt0yvluxjUkct70eX3HUuNvHi/sXEhFBhanuuv0hoFZtB5hQ2q0QKZkLamYFKldX2aBUWeOxZLYUovR53rK5oh79wx8ft9SrBTsdeydgF8pHzqty8YDFrSqPdJmrAgMBAAECggEAL487cE46CMVrL3aSviWFcuWBckPO99oi/APmLZH9KIjEXrsdNUBeCL4aA9uJE4fzeyOnilyg6f3SuchWzcA7nwZ6GfHjf4srCHxLijaH9CPw7xQaIWCeCfvzjIlmw15sLZsUKsu5izNZ5ge3q8F+27QlkFXr8D+J7fG9hr+kQ1Z1VO+sL/XqCOa63Dsxt+R1WVf4jnuMq/P1s6LMVpfhz3V+MyYf8Y44CAIsB5I3t+5omt9lXL8XFe5O33x2o47bOI6N0Zx6jZP8T27M4B0Kbicho1CiUiQg5gP2vume4bc6o9K+E2fSUWD68g1Pxo+bMpGU1teRWfwaaDV3MB2xfQKBgQD7+5F6J0Q3kDbUMMmHBWVkjm8UKffuzxEXyiqIt6IryudfEmR+sNIzd6HRJIeRlJEpgTr4lpFDK9rtqDEVDB5gVdXKGugPlf3KXcenjI28a74wX9zLgpOV8wl3JBCX3BeJvq1Ix8punzP0jcyD+o9+xHgJ1w+A3jgjYbaiAHLqTwKBgQDoJZigYiV4TFp+8TsG2eo4BtDBrhdsjwBUsQCXKmpXqpPbr9Is94lHN3xOo3nMqkMVDproPxy/UBfsOpgrv5w2j5uMAg4KIbpAf77T917ct/5pkRH2DUAO2btLMoOrqy90XnIkC/16+3mhsT8ZgAA5HY56U3Fch7DYqO1W13Ov5QKBgDuKwF9689WlUC9nr9cl4UWXmovLNvjJGSLj9JUFv15VvcJrWC/PnHvo4uXGmfo+fuat0L945K5YqIzL130/tdXRsojlPrygFq5A5rKitcM2OmS34jZ8jl86ZJwwe3NG4JD963htLVpiaCZ7cz4kcM/loO/CT7mFyQIjTsKNFJplAoGAVuQLlqxlvftGPBFquIvEIShb5sKUvetgglKmF+6mUx3jrezHTFFyrpdUDdMeeb6Y0T7gOGG3DtyZt6/O2UEk5T8nR2ZVfvFU12N5N7l1ZkPwE+9au7AF9AkHE4RayFyQrIvVSSy7udZ6lkzTg2Xnr3kipSKqYangRZglDcYi5QkCgYEA0PPPmIZgVIHfWo6GGTh958ehXYKVqg3NkQwaVJbh62dMZa2QcdonEZaMGUENbBbmAaFtzcl1z/phVTV0lfxjxjXZxfnb8cTS7WOZwoKcbzm2I9MatFnVJYEFDiNRomqLwzNABj0R7AVmj76YWSjpbAOq2VlDSy4/bzHYcD+BtA4=', '登录页支付宝应用私钥 PKCS8 Base64', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.alipay-public-key', 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgq518/qXFF4HTvOJeSa3xzWsa8+3Xs4GUl8/3StoaAK+KTxpBrwGU2hYKW3zSK1ROseN7upkYPX6vBTYCYxKrJzmOlYW5ckp/PfXtTiX04JNdtHAQQDUW8uULGtOn3RwioRMRUX+QGI1oaa1Pv9BQtGWpn18N+n68n9B5y1FUAYKnpcXd1fv0IU1fng0N3elA6RHsFxYs03vVCco41oEa6TBn7CO4Rvx5Cy1oj6h9uaHWLIt2M/ddpKzUmXm7cDdo3xFvedIwkjhwGJFLph/x/0sFC3n1i9pUSoYTA4gZheZD3ph1nEFnSR3z3+UPcLSmo6f5hhXcGCyjdN6nL6FUQIDAQAB', '登录页支付宝公钥', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.douyin.client-key', 'aw59ctbcbd9mljzb', '登录页抖音开放平台网站应用 Client Key', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.douyin.client-secret', 'b4c470568bbe083afdd73837eec01a1e', '登录页抖音开放平台网站应用 Client Secret', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
-- STEP LOGIN-OAUTH-1 END
