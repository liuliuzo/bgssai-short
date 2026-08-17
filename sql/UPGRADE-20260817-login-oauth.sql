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
-- 用户端 POST /bgssai/login/oauth/authorize-url 读这些键（LoginOAuthSecrets）。
-- 行已存在且值非空时不覆盖。一行一条 INSERT。
-- ------------------------------------------------------------------
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-id', 'wx496f923c7d4ac64e', '登录页微信开放平台网站应用 AppID(snsapi_login)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.wechat.app-secret', '812fbfc6570dda328805ce87d8e7fbd4', '登录页微信开放平台网站应用 AppSecret', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.app-id', '2021006186639146', '登录页支付宝网页授权 AppID(auth_user)', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.app-private-key', 'MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDY0ivwR1lqxBWJs9Xw9fL20bGrAmome8hXNf3YIq4doULMsox9L6hn3GEU2ad+ktCgKdL9FW2gqD6Qa6gNoUuyHRPLzzHdYKGFVQcwCEINEDK/I4K9ghWOziXapCGjB8IcNBId1ykWGEbLlzS+Rw1fuYM3qvTxz1b2qu1J7f7qmBizSSs+GK56RasuUGh3DFgpUbk+5o2TTK4fW6eP+zP7WDHyIhMC4Zf8KzW8yqu5y5onbpx0S8TWEYd0FwWz2a58YCihYZlxb1mGGKNKP8l7lANDlX/B+VAFIQEZK1XTvlQs5Vkx+YdZZ0/PJ5yYpW+I5lWn/8VWVj2+xKo8y8EXAgMBAAECggEAOybvnNBiWSngtF4vLltzKxofTgfNHO2lRQGyXL5tYSEHH+BQSrDvUVKC29TbiMEWELQIX0pyy6NK2ky8nudTxuZ+6Zn/wSUlO8JsQRLnrjeumI3gE8KDDiA4nJMtqBQsRVSBYsm56FC8M7vvREAPKn/qiAvoYnYOBCKytPoskyRlkL4gDi07fSAMyNAc1W9ieAiF59yYJeF9+Nw7fH0zDviZfKGO2/jGLQrrYFazAtuZLXKPzVFNsAoFW+YOHlr5m4bSlcj5/Lv/0n7tF6vYNHHwi2yRFJ/tQ152qwoFcs5Cu9gtzGOIk/XodjVeuCtLh1kqwiuUQkT8FBqVA4jdMQKBgQD/vzPlY57y6zdaGdWt9nTI8YKUEX7x26fe3+/1kmezd2ecT3qHJ6W3T4ECSznA8Sz4t5v8lxQx892o7YIsRMLF+FmcJXYWQ/MJWSPEce7VoOeL9fF4EeJZIlfGmUZ9tc8uJWMxAM3ZurZPfIrZVRHKV7lZ1e34ok06drTkFy0jhwKBgQDZCRtA6MvwADQ19qnat6Co8uHPSO5kLYtA//Ibv3CzhWXg93PfXboW3vopUSnHbnizl2p8TL0ydfZPZW+za7WGQDHYU0qjRpmFcFzKMJS1G7YjVj6LIAG/Ily5wjK96tZOXnadVZzOR41Ds68kGoxPxLaoV2UEbJlOLZXPnAn58QKBgCejzTlq3YItbyDqDDKwlp1mwaSNqa13LGfhMeJQVP5+LXMO5qD4RjlJe2eNnWAIgb3zR5w5Foo/kEXUum5+Ieppego3zDvbQFpclKyoNCqsi/cnocvIidri9dP0gfaOLZSretpnk4CB7iwX07Wn99/zBOmlvQLitTGwoI0SAo8VAoGAOgNzI4CWssp7KkmK0YnwCfkSXwQRpPl/lZpUrxIXIAVyXBYJBRXani5wFAc78bEX1nmM3zjsAccCVeq5Vu5dkUsrh7WL5+UBOGf2hZ1Cwr5rizqMcUfW765Rtt2wDo/tRPl/H0M5LPJ3Ujd/g1kNVG0yngDuYaZdvEX3G42MJSECgYBZPZ8uwMqQW0T25eINvFYKZER7VZHOgKUxp+8gXZxN1aHhbURs3VV/K18PcKfXZDTbywAFkhViHsv5CIlC3qaAeoG7mTO5FQxsXGR7APA3bJlXTA9mOy4W8sxV7IQDppa0wu5UIGkp6nZ79RsBo0mL6BGZmV/HGqau/Jd1oYe7/Q==', '登录页支付宝应用私钥 PKCS8 Base64', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.alipay.alipay-public-key', 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgq518/qXFF4HTvOJeSa3xzWsa8+3Xs4GUl8/3StoaAK+KTxpBrwGU2hYKW3zSK1ROseN7upkYPX6vBTYCYxKrJzmOlYW5ckp/PfXtTiX04JNdtHAQQDUW8uULGtOn3RwioRMRUX+QGI1oaa1Pv9BQtGWpn18N+n68n9B5y1FUAYKnpcXd1fv0IU1fng0N3elA6RHsFxYs03vVCco41oEa6TBn7CO4Rvx5Cy1oj6h9uaHWLIt2M/ddpKzUmXm7cDdo3xFvedIwkjhwGJFLph/x/0sFC3n1i9pUSoYTA4gZheZD3ph1nEFnSR3z3+UPcLSmo6f5hhXcGCyjdN6nL6FUQIDAQAB', '登录页支付宝公钥', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.douyin.client-key', 'aw59ctbcbd9mljzb', '登录页抖音开放平台网站应用 Client Key', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
INSERT INTO `sys_secret` (`secret_key`, `secret_value`, `description`, `enabled`, `updated_by`) VALUES ('bgssai.oauth.douyin.client-secret', 'b4c470568bbe083afdd73837eec01a1e', '登录页抖音开放平台网站应用 Client Secret', 1, 'system') ON DUPLICATE KEY UPDATE `secret_value` = IF(`secret_value` IS NULL OR `secret_value` = '', VALUES(`secret_value`), `secret_value`);
-- STEP LOGIN-OAUTH-1 END
