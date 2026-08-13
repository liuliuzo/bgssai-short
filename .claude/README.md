# .claude/ 目录说明

本目录承载 Claude Code 的仓库级配置。当前仅用于一项用途：**阻止 AI 创建会反复自唤起、持续消耗 token 的循环任务**。

## 事故背景

2026-07-28，外部 AI 助手在未获授权的情况下创建了每 15 分钟运行一次的 heartbeat 轮询作业（`voiceunion-pr`），持续约一天。该作业在无人值守下反复唤起 AI 会话，耗尽当周包含用量，并额外产生约 6 欧元 credits 费用。

问题不在于某一次调用贵，而在于**它会自己再来一次**：15 分钟间隔意味着一天自动执行 96 次，无人察觉即可持续烧钱。这类任务的成本上限由「跑了多久」决定，而不是由「单次多贵」决定。

该作业是靠**平台侧的定时触发器反复唤起 AI 会话**运行的，不是靠宿主机 crontab——闸门的重点因此放在会话 / 平台级的调度入口上。

## 判定标准（只有一条）

> **它是否会在无人值守下反复唤起 AI 会话、持续消耗 token？**

是则拦截，否则放行。本闸门**只管 AI 的 token 消耗，不管项目自身怎么调度**。

## 拦截什么

**一、会话 / 平台级周期性调度（事故的真实路径）**

| 工具 | 拦截条件 |
| --- | --- |
| `CronCreate` | `recurring` 为真（默认值即为真）；一次性提醒放行 |
| `ScheduleWakeup` | 一律拦截（`/loop` 自定节奏自唤起）；`stop: true` 放行 |
| `create_trigger` / `update_trigger` | 带 `cron_expression` 的周期性 Routine；`run_once_at` 一次性放行 |

**二、「调度器 + AI CLI」与「循环 + AI CLI」的组合**

用 `crontab` / `systemd-run --on*` / `systemctl ... .timer` / `at` / `schtasks` / `launchctl` 周期性拉起 `claude` / `codex` / `gemini` / `aider`；或 `while true` / `until false` / `for ((;;))` / `watch` 循环里反复调用 AI CLI；或 AI CLI 自带的 `--cron` / `--schedule` / `--heartbeat` / `--watch` 参数与同名子命令。

**三、「定时触发 + 调用 AI」的工作流**

`.github/workflows/*.yml` 中**同时**出现周期性触发（`schedule:` / `cron:`）与 AI 调用（`anthropics/claude-code-action`、`@anthropic-ai/claude-code`、`claude` / `codex` 命令）时拦截。**只有两者同时出现才拦**。

## 不拦什么（重要）

项目自身的调度**一律放行**——它们不经过 AI 会话，一个 token 也不消耗：

- 数据库备份 `crontab`、日志清理 `*.timer`、运维巡检脚本；
- 普通 CI 的 `schedule:` 定时触发（跑 `mvn test` / 构建 / 巡检等）；
- Jenkins 的 `pollSCM` / `cron(...)` 触发器；
- 应用运行时的业务定时任务：Spring `@Scheduled`、Quartz、XXL-JOB；
- 产品语义的 heartbeat / keepalive 接口、设备心跳上报、任务状态轮询接口；
- 前端 `setInterval` / SWR / 长轮询；
- 开发态 `npm run watch` / `vite dev`；
- 一次性 `sleep`、`while read` 逐行读取、`watch -n 900 gh pr checks` 等不调用 AI 的命令。

一句话：**cron 本身不是问题，cron 拉起 AI 才是问题。**

## 例外流程

确有周期性需求时，不要绕过闸门：

1. 向仓库负责人报备用途、间隔、预计每日调用次数与消耗上限；
2. 获批后优先改为**人工手动触发**；
3. 确需自动周期执行的，由负责人在 `settings.json` 登记例外，并**设定到期时间或最大执行次数**，禁止无限期常驻。

## 自检

```bash
H=.claude/hooks/block-scheduled-automation.py

# 应被拦截（输出 deny JSON）：周期性唤起 AI
echo '{"tool_name":"CronCreate","tool_input":{"cron":"*/15 * * * *","prompt":"check","recurring":true}}' | python3 $H

# 应放行（无输出）：项目自己的备份 cron
echo '{"tool_name":"Bash","tool_input":{"command":"echo \"0 3 * * * /opt/backup/db-dump.sh\" | crontab -"}}' | python3 $H
```

判定口径与例外边界另见 Standards §12。本仓库若无 `docs/BGSSAI-Standards.md`，则以本文件为准。
