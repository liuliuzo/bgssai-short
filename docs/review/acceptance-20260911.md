# bgssai-short 验收与同步记录

日期：2026-09-11。需求版本：2026-09-11。

基线：`3fcf8235138e6289bef8fe3baf4c9e7200361cc4`（本轮 fetch 的 origin/develop）；本次修复在独立 codex 工作分支，最终 commit/PR 见 GitHub。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。

- **已阻断假成功**：未配置 Provider / Media / 非 local 的 OBS 静默 LocalFile 回落，以及 `stub://` 等占位 URL，均不得报 SUCCEEDED/READY 商业成功（见 `docs/feature/short-01-commercial-gate.md`）。
- **Short→Media**：区分 PENDING（交接受理、`commercial_ready=false`）与 FAIL（明确失败）；不得把 PENDING 计为 succeeded。
- **仍 PENDING**：真实模型生成、OBS 成片、Short→Media 直播 E2E（不伪造供应商凭证）。

此前 Windows 字幕滤镜盘符问题已修复并保留。

## 执行结果

**PASS**：mvn -B -ntp test；{'tests': 208, 'failures': 0, 'errors': 0, 'skipped': 2}

测试通过只覆盖上述命令，不代表所有功能或线上环境通过。浏览器控制工具本轮不可用；原型仅执行静态核对。未执行付费模型、短信/邮件投递、第三方用户授权、真实站点发布或手机安装。

## 五层产出定位

| 层 | 纳入指纹的文件数 | 状态 |
| --- | ---: | --- |
| requirements | 44 | 已记录内容指纹，语义按本报告复验 |
| prototype | 45 | 已记录内容指纹，语义按本报告复验 |
| design | 29 | 已记录内容指纹，语义按本报告复验 |
| database | 4 | 已记录内容指纹，语义按本报告复验 |
| code | 1109 | 已记录内容指纹，语义按本报告复验 |

检查器唯一实现位于骨架仓 `scripts/verify_artifact_sync.py`。本仓快照为 [artifact-baseline.json](artifact-baseline.json)。指纹检查发现新增/删除/修改后必须复验；更新快照不能自动提升功能结论。

## 源码及契约证据

- [bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/aiprovider/FfmpegComposeProvider.java](../../bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/aiprovider/FfmpegComposeProvider.java)
- [bgssai-short-user/bgssai-short-user/src/test/java/com/bgssai/shortai/aiprovider/FfmpegComposeProviderTest.java](../../bgssai-short-user/bgssai-short-user/src/test/java/com/bgssai/shortai/aiprovider/FfmpegComposeProviderTest.java)
- [docs/feature/film-publish-media.md](../../docs/feature/film-publish-media.md)
- [docs/feature/film-compose-export.md](../../docs/feature/film-compose-export.md)
- [docs/demo-static/web/user/compose-export.html](../../docs/demo-static/web/user/compose-export.html)

## 数据库与交付边界

产品源码/SQL 与验收快照在本分支记录；线上数据库执行状态为 NOT_VERIFIED。数据库目录为空的产品不能据此伪造 SQL，已有 JSON/Prisma 持久化应按真实实现评估。历史数据库批次保持只读，不把静态指纹称为已执行的迁移。

所有交付只走工作分支提交、push 和 draft PR；由用户在 GitHub 人工合并。已知缺口不因提交 PR 而关闭。
