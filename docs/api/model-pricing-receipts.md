# 模型与执行对账契约

目录保留 billing_multiplier（number|null）、billing_unit、price_version、pricing_status、currency、input_fen_per_million、output_fen_per_million、cached_read_fen_per_million、price_unit、image_fen_per_unit、audio_fen_per_second、video_fen_per_second。缺项保持 null/未配置。显示与调用模型来源一致；本产品既有商业定价单列，不与网关价格混用。

持久化 tokenhub_usage_receipt：user_id、work_ref、task_ref、hub、endpoint、key_fingerprint、model、request_id、receipt_json、reconciled、next_poll_at；唯一键 hub/request_id。每个派发先 REQUIRES_NEW 提交记录，失败即不调用供应商；完成或异常均尝试 GET /v1/usage/{request_id}，原凭证认证。后台每分钟恢复每批最多50条 pending，失败五分钟后重试，已获得 pending 回执一分钟后补查。只读查询不会再次执行供应商任务/扣商业余额。指纹查找原凭证，轮换不匹配保持 pending。

用户 GET 对账列表按会话 user_id 过滤，支持 work_ref/task_ref，最多100条；输出 receipt 含 request_id/raw_tokens/billed_tokens/billing_multiplier/billing_unit/price_version/billing_status/debit_fen/quoted_debit_fen/currency/reservation_status，未知为 pending/null。列表里的 hub 固定为 cn，不回显网关主机。界面展示 0.5× / 1× / 1.5×，不展示中枢品牌。只信匹配 request_id 的网关 receipt，跨用户无数据。Long 路径 /api/ai/usage，Short 路径 /bgssai/user/ai-usage/list。作品面板提供刷新；记录后端持久化，不依赖浏览器。管理员部署前执行新增表 DDL，环境仅 properties 区分。

选择器只合并境内目录与本地已启用的境内模型。境外模型不进入选择器，失败时也不改走境外。回退模型的费用以该次实际 receipt 为准。Long 外层事务失败不回滚已经派发的对账记录；Short worker 线程保存 user/work/task 上下文。新增表也提供独立 sql/UPGRADE-20261015-usage-receipt.sql，尚未在线执行。

对账完成条件：金额状态为合法非pending终态且billed_tokens为非负整数；额度已知但金额pending，或金额已落账但额度尚未恢复，都必须继续GET。

官方网关只认境内 .cn 主机。境外 .com 与历史 -global.com 不是短剧的调用或回退地址。
