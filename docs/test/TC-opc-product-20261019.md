# bgssai-short：2026-10-19 功能目录用例

需求：[2026-10-19 功能需求](../feature/opc-requirements-20261019.md)。编写日期：2026-10-19。对照：[2026-10-19 OPC 静态差距审计](../feature/opc-gap-audit-20261019.md)。

结果只用待测、通过、失败、阻塞、不适用。功能未实现不能写不适用。2026-10-22 已跑本仓模拟测试。需要真实 Tube、四端播放或成片画面才能做完的步骤没有执行，相关用例记阻塞。不填写口令、Token、密钥或个人资料。

| 用例ID | 关联需求 | 优先级 | 端 | 前置条件与数据 | 测试步骤 | 期望结果 | 结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-PRODUCT-101-P | REQ-SHORT-PRODUCT-101 | P0 | user | 隔离环境；已记录被测版本；一部已导出且审核通过的短剧成片；Tube 测试空间可查询作品号；将该次发布回执做成未知（超时或未回写作品号）。 | 1. 把该成片交给 Tube，并确认 Short 侧发布结果仍为未知；2. 查看 Short 任务与已记录的 Tube 作品号；3. 对同一成片版本执行恢复后再查 Tube。 | 正向判定：Short 任务与 Tube 作品号一一对应，未知状态可见。 | 阻塞 | 正向。2026-10-22 模拟已过：FilmPublishToMediaServiceTest.unknownHandoffRecoversExistingMediaWithoutAnotherIngest、pendingLookupUnknownDoesNotIngestAgain、timeoutIsUnknownAndDoesNotLookLikeFailure；baselineCards.test.js「unknown publish status is visible and is not a success」。卡在真实 Tube 测试空间与四端播放，未核对线上作品号和真实可播。 |
| TC-SHORT-PRODUCT-101-N | REQ-SHORT-PRODUCT-101 | P0 | user | 同上，并保留同一成片版本在结果未知时可以再次提交。 | 1. 在发布结果未知时对同一成片版本连续重试；2. 到 Tube 按该来源成片检索作品。 | 异常判定：重试后 Tube 出现两条相同作品，不能通过。 | 阻塞 | 异常/边界。2026-10-22 模拟已过：FilmPublishToMediaServiceTest.pendingWithRecordedMediaIdAndNotFoundDoesNotCreateSecondWork、mismatchedMediaIdStaysUnknownAndDoesNotIngest、batchUnknownIsNotCountedAsSuccess。卡在真实 Tube 目录检索，未确认线上没有两条相同作品。 |
| TC-SHORT-PRODUCT-102-P | REQ-SHORT-PRODUCT-102 | P1 | user | 隔离环境；已记录被测版本；项目内有已确认的角色版本和分镜时间轴；导出成片绑定的就是这两个版本；准备可抽检的镜头、字幕和时长。 | 1. 打开剪辑工程，在 LOCKED 角色卡上记录「角色版本」，在 LOCKED 时间轴卡上记录「时间轴版本」（含镜号、时长、字幕）；2. 导出成片；3. 抽检镜头，对照角色、字幕和时长。 | 正向判定：抽检镜头的角色、字幕和时长与时间轴一致，且剪辑工程能看到「角色版本」「时间轴版本」。 | 阻塞 | 正向。2026-10-22 模拟已过：EditBaselineCardsTest.lockedCardsShowChineseVersionLabelsAndVoidedIsNotConsistent、filmRecordMatchesTimelineAndRefusesVoidedCharacter。卡在真实成片画面和 9:16、16:9 音画顺序抽检，未打开剪辑工程做人工对照。 |
| TC-SHORT-PRODUCT-102-N | REQ-SHORT-PRODUCT-102 | P1 | user | 同上，并另有一个已作废的角色版本；成片或标记仍可能指向该作废版本。 | 1. 作废当前角色版本并保留作废记录；2. 打开剪辑工程，确认作废角色不再显示「角色版本」，且文案写明不能标为与现行时间轴一致；3. 对照成片画面与作废角色版本。 | 异常判定：成片使用了已作废的角色版本却标一致，不能通过。 | 阻塞 | 异常/边界。2026-10-22 模拟已过：EditBaselineCardsTest.filmRecordMatchesTimelineAndRefusesVoidedCharacter。作废角色记录的 consistent_flag 为 0。卡在对照真实成片画面。失败镜头不覆盖已确认版本见既有 ShotVersionGuardTest，本次未做画面验收。 |

执行记录（2026-10-22，本工作区）：用户模块 `mvn -o -q -pl bgssai-short-user/bgssai-short-user test`，用户前端 `npm test`。证据是模拟的 Tube 客户端和内存中的成片基准。没有连接真实 Tube，没有四端播放，没有对照成片画面，所以四条结果记阻塞。同步 [进度](PROGRESS.md)。
