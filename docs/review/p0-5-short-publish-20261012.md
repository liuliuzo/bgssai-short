# P0-5 short：发布到 Media 与成片状态（2026-10-12）

- 仓库：`bgssai-short`
- 范围：只做短剧这一半。Media 平台化与 long 分镜渲染不在本仓。

## 这次补上的缺口

| 项 | 结论 |
| --- | --- |
| 发布 Body 对齐 Media 现行契约（`status=READY`、`approved=true`、幂等键） | 已接上 |
| Media PENDING 无 `media_id` / `play_url` 不再被写成 FAILED | 已接上。界面提示「已受理，尚未可播放」 |
| Media `code=422` / `playable=false` 记失败，用上游文案 | 已接上 |
| `base-url` 为空 | 失败（`4006`），不返回假 `media_id` |
| 镜头视频或配音变更后成片过期，并取消旧 Media 回放 | 已接上 |
| 合成成功清过期；合成失败不再把剧集停在 COMPOSING | 已接上 |
| 过期成片不给下载地址，下载接口 `4012` | 已接上 |

## 仍然没有做完（NOT_VERIFIED）

以下任何一项都**不能**写成通过：

1. **MEDIA 主机未分配。** 本仓没有写入虚构 IP。dev 使用约定域名 `https://dev.user.bgssai-media.com`，prod 使用 `https://www.bgssai-media.com`。ingest-token 在仓库里是空的。主机没起来或地址留空时，发布失败。
2. **Short 工作室 → Media → 播放器直播 E2E** 没跑。Media 侧 UNIQUE upsert 仍要靠对端；本仓单测只锁住 Short 的请求与落库。
3. **付费模型真实出片、华为 OBS 签名播放** 没跑。
4. **仅重烧字幕** 仍走合成任务，本轮没有证明它是低价、不重编码的独立路径。
5. **prod 部署** 是否已经带上 develop 上的登录修复，需要运维在 Jenkins 上确认，本轮没有部署。
6. **多端播放器**（Windows / Web / Android / iOS）没有冒烟。
7. **下架 / 撤回** 仍跳过：Media 没有对应契约。

## 怎么复跑契约测

```bash
mvn -B -ntp -pl bgssai-short-user/bgssai-short-user -am test \
  -Dtest=FilmPublishToMediaServiceTest,RestMediaIngestClientTest,FinishedFilmLifecycleTest,Short01EpisodeProductionAcceptanceTest
```

单测通过只说明契约与状态机，不说明直播发布成功。
