# 模型定价与真实用量对账（2026-10-15）

模型选择保留网关返回的 billing_multiplier、billing_unit、price_version、pricing_status、currency 与各计费单价。显示真实倍率（如已配置的 0.5×/1×/1.5×），无配置明确待确认，不按模型名猜倍率，不填假价格。商业套餐 costCent/product_tokens 单位和余额不变。

每次真实模型执行先持久化用户、作品/任务、hub、request_id、模型和凭证指纹，再发请求。使用 X-Request-Id 关联真实 receipt；输出成功不等于计费完成。失败和超时也保留 pending，不能凭时长、估算或输出长度伪造 billed_tokens。重启通过数据库待对账记录继续 GET 查询，不重发生成任务。幂等身份为 hub + request_id，原始 API key 不入对账表、不返回用户。

用户只能读取自己的记录；制作界面显示每次请求真实 raw_tokens、billed_tokens、0.5× / 1× / 1.5×、金额币种、billing_status，不展示中枢品牌或网关主机。重复刷新不扣商业余额，不再次乘倍率。凭证轮换后原凭证不可用则保持 pending，不能拿另一账号对账。上线前应用 sql/DDL.sql 中新增表，线上执行与真实供应商联调单独验收。

对账完成条件：金额状态为合法非pending终态且billed_tokens为非负整数；额度已知但金额pending，或金额已落账但额度尚未恢复，都必须继续GET。

官方网关只认境内 .cn 主机。境外主机不是短剧的调用或回退地址。
