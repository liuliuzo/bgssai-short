# 终态回调的条件更新

Worker.failIfRunning 返回本次 RUNNING→FAILED 条件更新是否命中；条件包含任务 id、RUNNING 与读取的 retry_count（null 保持 null），未命中不得调用镜头/成片终态回调。取消接口同样按读取的 task_status/retry_count 条件更新，失配拒绝，不覆盖已经成功或重新恢复的任务。

ShotGenerateOutcome 重读原任务，要求同 id/task_id/user/work/episode/target/type、相同 retry_count 且当前 FAILED/CANCELED。再读取该归属镜头及其他任务，计算就绪状态。显式 Mapper restoreBoundMediaAfterTerminalTask 使用绑定参数，在一次 UPDATE 内再次验证任务终态/重试版本/更新时间与归属；镜头 id/归属/deleted/status/updated_time/shot_version/prompt_version/首帧及视频资产均匹配快照；任何同镜同类型 id 更大的任务或其他 QUEUED/RUNNING/PAUSED 任务阻止回退。UPDATE 只写 shot_status/updated_time，不写资产或账单。

两级读取便于解释，最终 SQL 才是并发门槛。任务 retry_count 区分同一任务的恢复次数；镜头版本弥补 DATETIME 秒精度。旧任务失败通知不再改变已恢复任务镜头，即使任务在应用检查后被重新排队，也由 SQL 拒绝。真实 H2/MyBatis 回归验证终态改为 RUNNING、重试版本变化、后续任务、同秒镜头版本变化与资产变化的零行拒绝。

AiTaskService 的对外 view 统一仅在 FAILED 暴露失败码/安全失败原因，MCP get_render_task 继续委托此视图；不清除持久历史信息，也不产生模型调用或计费。本次同步官方 prototype-manifest，包含已合并 #207 的两页原型及绑定代码漂移。
