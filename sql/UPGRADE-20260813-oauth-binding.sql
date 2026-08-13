-- =====================================================================
-- BGSS Short Drama 存量库升级脚本
-- 文件：sql/UPGRADE-20260813-oauth-binding.sql
-- 适用范围：已经存在且有数据的库，新增三方授权登录绑定表。
-- 重跑安全：CREATE TABLE IF NOT EXISTS。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `oauth_binding` (
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
