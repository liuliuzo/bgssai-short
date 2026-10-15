# 模型定价与对账验收（2026-10-15）

需求 → HTML 原型 → API/数据设计 → 代码/SQL 按顺序落盘；本 PR 四层契约一致。模型目录只展示真实配置，包含小数倍率、计费单位、价格版本、金额币种。价格来源与实际 primary endpoint 一致；回退调用各有独立 receipt。没有修改套餐余额、costCent/product_tokens 单位或再次乘倍率。

新增真实 MyBatis Mapper XML + H2 MySQL 模式集成测试：派发记录独立事务提交并在外层回滚后保留；新的服务实例恢复 pending，只发 GET 对账；原凭证指纹隔离；错误 request_id 不采纳；用户互相不可读；迟到 pending 更新不能覆盖终态；每次实际请求使用独立 ID；无数据为 pending/null。

完整 Maven 测试已通过（具体数量见 PR），相关最终变更定向测试与 npm test、Vite production build 通过。真实供应商收费调用、跨主机生产 MySQL 验收、浏览器人工验收未执行。sql/DDL.sql 与 sql/UPGRADE-20261015-usage-receipt.sql 已同步但未在线执行，部署前由数据库/Jenkins流程执行。dev/prod 均部署 develop，各用原环境 properties；此次未部署、未合并。

边界：旧网关未支持 receipt、凭证轮换丢失原 key、供应商未上报真实用量时保持 pending，后台只补查不重发生成。最近100条列表按用户与作品/任务隔离。账本不存 API key；只存不可逆凭证指纹。任务失败可能已经产生供应商用量，必须保留记录。媒体的时长与商业计费次数不冒充原始 token。

Additional recovery regression: continue polling when quota is known but money remains pending, and when money is settled but quota is not yet recovered. Added one transaction-fixture test per backend; focused receipt suite rerun after this change.

追加官方域名别名复核：精确 URL host 同时识别 CN、Global .com 与历史 -global.com，执行、模型配置解析及目录保留明确配置的原端点，拒绝相似后缀/路径冒充。TencentTokenHubAiProviderTest、TokenHubClientSupportTest、UsageReceiptServiceTest 合计 24/24 通过。
