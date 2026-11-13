# 视频任务恢复设计

AiTaskService.resumeVideo 校验 UserContext 归属、失败/取消状态、公司国内模型、目标镜头和后续任务；UsageReceiptService 从 user/work/task/model 筛选唯一真实回执。CAS 将原任务改为 QUEUED 并在现有 request_json 写入内部 resume_request_id。这个字段不来自 HTTP/MCP 入参，不新建任务或账单。

ShortAiTaskWorker 只将内部恢复标记传给视频适配器；TencentTokenHubAiProvider 用当前 UsageScope 再核对唯一回执、request ID、模型、国内 endpoint 与 API key SHA256。复用原回执，调用 GET /v1/videos/generations/by-request/{request_id}?model=bgssai-video，随后复用既有轮询/下载。任何失败都不调用 POST。正常新任务仍使用现有提交路径。

网关按持久提交request ID找到唯一任务并核实原key/user/provider/model/operation；未知或歧义返回404。Short按原工作线程验证真实视频、存储并结算同一task ID。客户端不输入供应商任务ID或任意资源URL。公司视频恢复失败不得自动降级为新生成。

VIDEO提示词以 shot_desc 为剧情主干，追加 extra_prompt 与运镜；缺少描述才使用 image_prompt。已有任务恢复不改变上游已提交的提示词，成片质量必须独立检查。

Worker 恢复完成后先持久化 selected_flag=0 的真实 VIDEO 候选，再重读同一 user/work/episode/shot 与同镜 VIDEO 任务；镜头更新时间或状态变化、已有视频、锁定、后续任务或其他活动任务均拒绝落镜。命名 Mapper 方法 attachRecoveredVideoIfUnchanged 使用 id、归属、deleted_flag、读取的 updated_time/shot_status 与原 null/blank video_asset_id 的 CAS，参数全部绑定，并在同一 SQL 内核验原任务仍 RUNNING、不存在后续或其他活动视频任务，关闭查询后插入任务的窗口。只有 CAS 命中一行后才选择该资产并触发成片失效/下一镜标记；CAS 失败保留未选候选并报告冲突。实体和 DDL 字段不变。

MediaMetadata 对真实视频字节解析得到的 duration_sec 用于资产和用量；shot_duration_sec 仅是请求及合成计划，不回写覆盖真实视频时长。回归使用已有合法 MP4 fixture 经真实 ffprobe，并通过实际 Worker 调用及 MyBatis/H2 条件更新验证并发拒绝、候选选择顺序和时长保留。

公司视频在 Short 侧串行：已有一条执行中时，其余保持排队。列表按队列实时给出 `queue_ahead`、`elapsed_sec`、`progress_hint`、`status_note`，不落库。轮询超过一小段仍未完成时抛出仍在处理，写回原 `resume_request_id` 后继续查同一回执，不调用新的生成。种子 SVG 可以在恢复落镜时被真实 mp4 替换；已经是对象存储成片的 `video_asset_id` 仍拒绝覆盖。提示词把定妆外貌、服装、场景和道具放在动作描述之前。没有真实参考图时不把占位首帧当成已出图。

镜头 CAS 与资产 selected_flag 更新放在同一短数据库事务；任一步未命中或失败均回滚落镜，已保存的候选仍未选。模型轮询、媒体探测和对象存储不占用该事务。资产 ID 实体读取会 trim，因此 CAS 对非 null 空白资产值按 trim 后空值比较，null 保持独立匹配。

short_shot.updated_time 的 DATETIME 秒精度不能独立标识编辑版本。Worker 原快照与重读结果还须同时比较 shot_version、prompt_version；最终 CAS 对这两个字段同样执行 null-safe 精确比较，null 不视为零。同时间戳而版本变化的 Worker/H2 回归必须拒绝，避免同秒编辑被覆盖。
