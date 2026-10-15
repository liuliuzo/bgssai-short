# 模型与执行对账契约

目录保留 billing_multiplier（number|null）、billing_unit、price_version、pricing_status、currency、input_fen_per_million、output_fen_per_million、cached_read_fen_per_million、price_unit、image_fen_per_unit、audio_fen_per_second、video_fen_per_second。缺项保持 null/未配置。显示与调用模型来源一致；本产品既有商业定价单列，不与网关价格混用。

持久化 tokenhub_usage_receipt：user_id、work_ref、task_ref、hub、endpoint、key_fingerprint、model、request_id、receipt_json、reconciled、next_poll_at；唯一键 hub/request_id。每个派发先 REQUIRES_NEW 提交记录，失败即不调用供应商；完成或异常均尝试 GET /v1/usage/{request_id}，原凭证认证。后台每分钟恢复每批最多50条 pending，失败五分钟后重试，已获得 pending 回执一分钟后补查。只读查询不会再次执行供应商任务/扣商业余额。指纹查找原凭证，轮换不匹配保持 pending。

用户 GET 对账列表按会话 user_id 过滤，支持 work_ref/task_ref，最多100条；输出 receipt 含 request_id/raw_tokens/billed_tokens/billing_multiplier/billing_unit/price_version/billing_status/debit_fen/quoted_debit_fen/currency/reservation_status，未知为 pending/null。只信匹配 request_id 的网关 receipt，跨用户无数据。Long 路径 /api/ai/usage，Short 路径 /bgssai/user/ai-usage/list。作品面板提供刷新；记录后端持久化，不依赖浏览器。管理员部署前执行新增表 DDL，Jenkins 部署 develop，环境仅 properties 区分。

选择器合并目录模型与本地已配置模型，并以实际 primary 路由的 endpoint 读取价格，避免把 CN 价格贴到 Global 调用。回退模型的费用以该次实际 receipt 为准。Long 外层事务失败不回滚已经派发的对账记录；Short worker 线程保存 user/work/task 上下文。新增表也提供独立 sql/UPGRADE-20261015-usage-receipt.sql，尚未在线执行。
