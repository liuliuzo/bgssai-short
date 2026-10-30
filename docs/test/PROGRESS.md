# liuliuzo/bgssai-short 测试进度

> **Infra 2026-10-31 Asia/Shanghai**: company LLM wiring [PR#164](https://github.com/liuliuzo/bgssai-short/pull/164) merged; `dev-short-deploy` #51 SUCCESS; DEV MySQL `ai_model_config` defaults TEXT/IMAGE/VOICE → bgssai-chat/image/tts. Browser FTC wave in progress (`FULL-QA-FTC-2026-10-31.md`); suite pass/fail columns unchanged until browser results.

2026-10-30 补入尚未进入既有功能清单的用例，结果均为待测。历史结果不改。

统计更新：2026-10-22。本表按 `docs/test/TC-*.md` 的“结果”列统计，包含既有记录、2026-10-17 的 76 条用例与 2026-10-19 新增 4 条用例；不重新认定历史通过代表当前版本通过，不重复统计 `docs/review/test-cases`。2026-10-19 四条用例结果为阻塞：模拟测试已做，真实 Tube、四端播放和成片画面未验收。2026-10-17 用例结果未改。

| 范围 | 待测 | 通过 | 失败 | 阻塞 | 不适用 |
| --- | --- | --- | --- | --- | --- |
| 当前目录合计 | 65 | 4 | 0 | 17 | 0 |

| 套件 | 待测 | 通过 | 失败 | 阻塞 | 不适用 |
| --- | --- | --- | --- | --- | --- |
| [TC-dev-20261015.md](TC-dev-20261015.md) | 0 | 4 | 0 | 0 | 0 |
| [TC-feature-gaps-20261017.md](TC-feature-gaps-20261017.md) | 12 | 0 | 0 | 0 | 0 |
| [TC-opc-common-20261017.md](TC-opc-common-20261017.md) | 35 | 0 | 0 | 3 | 0 |
| [TC-opc-platform-20261017.md](TC-opc-platform-20261017.md) | 0 | 0 | 0 | 10 | 0 |
| [TC-opc-product-20261017.md](TC-opc-product-20261017.md) | 16 | 0 | 0 | 0 | 0 |
| [TC-opc-product-20261019.md](TC-opc-product-20261019.md) | 0 | 0 | 0 | 4 | 0 |
| [TC-icp-filing-footer.md](TC-icp-filing-footer.md) | 2 | 0 | 0 | 0 | 0 |

## 历史进度原文（原日期与结果保留）

<!-- OPC-PROGRESS-HISTORY -->
# bgssai-short 测试进度

| 项 | 值 |
| --- | --- |
| 仓库 | bgssai-short |
| 案例目录 | `docs/test/` |
| 统计范围 | 只统计本目录 `TC-*.md` |
| 更新 | 2026-10-15 |
| 待测 | 0 |
| 通过 | 4 |
| 失败 | 0 |
| 阻塞 | 0 |
| 不适用 | 0 |

## 功能进度

| 功能 | 案例 | 待测 | 通过 | 失败 | 阻塞 | 不适用 | 最近执行 | 备注 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 登录 | TC-dev-20261015 | 0 | 2 | 0 | 0 | 0 | 2026-10-15 | 开发环境一轮 |
| 短剧 | TC-dev-20261015 | 0 | 1 | 0 | 0 | 0 | 2026-10-15 | 列表、创建、回读成功 |
| 管理端 | TC-dev-20261015 | 0 | 1 | 0 | 0 | 0 | 2026-10-15 | 创作者列表可打开 |

## 历史套件

[docs/review/test-cases](../review/test-cases/README.md) 中已有成套案例时，执行前的起草记录见 [STATUS.md](../review/test-cases/STATUS.md)。本表不重复计入那些文件。
