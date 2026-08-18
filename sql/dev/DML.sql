-- =====================================================================
-- sql/dev/DML.sql — 开发环境专属种子
-- =====================================================================
-- 在共享 DDL 与共享 DML 之后执行。清库 init 顺序:
--   mysql ... < sql/DDL.sql
--   mysql ... < sql/DML.sql
--   mysql ... < sql/dev/DML.sql
-- 本文件只写环境相关值。用户端域名每次执行覆盖为 https://dev.user.bgssai-short.com。
-- 管理端域名不作为 OAuth / 支付回调域。
-- =====================================================================

USE `bgssai_short`;

SET NAMES utf8mb4;


-- 当前无环境专属种子行。登录 OAuth redirect_uri / 支付 notify-url 写在
-- application-*.properties，不入本表。后续若有随环境变化的库内值，只改
-- sql/dev/DML.sql 与 sql/prod/DML.sql，保持 STEP 名与列清单一致、只改字面量。
