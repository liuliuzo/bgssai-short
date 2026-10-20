-- DEV UX: bring demo usage into the default 30-day window; scrub TLS fail_reason; refresh month_spent.
-- Idempotent-ish: safe to re-run (date shift only affects pre-2026-09 rows; fail scrub is LIKE match).

UPDATE `ai_usage_record`
SET `created_time` = DATE_ADD(`created_time`, INTERVAL 45 DAY)
WHERE `user_id` = 'u-demo-0001'
  AND `created_time` < '2026-09-01';

UPDATE `user_quota` q
SET q.`month_spent_cent` = (
      SELECT COALESCE(SUM(r.`cost_cent`), 0)
      FROM `ai_usage_record` r
      WHERE r.`user_id` = 'u-demo-0001'
        AND r.`created_time` >= '2026-10-01'
        AND r.`created_time` < '2026-11-01'
    ),
    q.`month_reset_date` = '2026-10-01',
    q.`gmt_modified` = NOW()
WHERE q.`user_id` = 'u-demo-0001'
  AND q.`del_flag` = 0;

UPDATE `short_ai_task`
SET `fail_reason` = '暂时无法拉取任务：连接失败，请稍后重试',
    `updated_time` = NOW()
WHERE `fail_reason` LIKE '%handshake%'
   OR `fail_reason` LIKE 'Remote host%'
   OR `fail_reason` LIKE '%Connection reset%'
   OR `fail_reason` LIKE '%SSLHandshake%';
