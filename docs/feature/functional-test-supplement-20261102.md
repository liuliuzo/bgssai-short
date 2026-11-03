# bgssai-short：2026-11-02 功能测试补充

本轮仅把当前需求细则中的边界和故障路径补入测试设计，不改变产品功能契约。基线 `3548e63947aac4cdfdbd96770767d126d7337a99`；原案例与运行状态保留。旧目录已覆盖的主要功能见 [扫描清单](../test/SCAN-20261102.json)。

| 需求ID | 验收主题 | 既有需求来源 |
| --- | --- | --- |
| REQ-SHORT-SCAN-20261102-001 | 中文入口别名和未知路由 | [docs/feature/chinese-only-en-alias.md](../../docs/feature/chinese-only-en-alias.md) |
| REQ-SHORT-SCAN-20261102-002 | Tube 受理未就绪与失败分别统计 | [docs/feature/short-01-commercial-gate.md](../../docs/feature/short-01-commercial-gate.md) |
| REQ-SHORT-SCAN-20261102-003 | 结算恢复不重复生成或折算 | [docs/feature/model-pricing-receipts.md](../../docs/feature/model-pricing-receipts.md) |

所有新增案例均为待测；源码存在、目录校验通过或 PR 创建不等于产品运行通过。
