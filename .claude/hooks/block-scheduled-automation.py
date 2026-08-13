#!/usr/bin/env python3
"""PreToolUse 闸门：阻止 AI 创建「会反复自唤起、持续消耗 token」的循环任务。

背景：2026-07-28 外部 AI 助手在未获授权的情况下创建了每 15 分钟运行一次的
heartbeat 轮询作业（voiceunion-pr），持续约一天，耗尽当周包含用量并额外产生
约 6 欧元 credits 费用。该作业靠**平台侧定时触发器反复唤起 AI 会话**运行，
而非宿主机 crontab。

判定标准只有一条：**它是否会在无人值守下反复唤起 AI 会话、持续消耗 token**。
是则拦截，否则放行。

因此本闸门：
  - 拦截会话 / 平台级的周期性调度工具（CronCreate、ScheduleWakeup、
    create_trigger 的 cron_expression 等）——这是事故的真实路径；
  - 拦截「调度器 + AI CLI」与「循环 + AI CLI」的组合；
  - **不拦**项目自身的运维调度：数据库备份 cron、日志清理 timer、
    普通 CI 的 schedule 触发器、Spring @Scheduled、前端 setInterval 等,
    这些不经过 AI 会话，一个 token 也不消耗。

输入：Claude Code PreToolUse 事件 JSON（stdin）。
输出：命中时输出 permissionDecision=deny 的 JSON；未命中静默退出 0。
"""

import json
import os
import re
import sys

# AI CLI 调用形态：命令位出现 claude / codex / gemini / aider 等。
# 负向后顾避开 ".claude/hooks" 这类路径；后随空白或行尾才算命令调用。
AI_CLI = r"(?<![\w-])(?:claude-code|claude|codex|gemini|aider)(?:\s|$)"

# 宿主机调度器 / 常驻循环。单独出现不拦，只有与 AI CLI 同现才拦。
SCHEDULER = (
    r"(?:^|[;&|]\s*|\s)(?:crontab|schtasks|batch)\b"
    r"|(?:^|[;&|]\s*|\s)launchctl\s+(?:load|bootstrap|submit)\b"
    r"|(?:^|[;&|]\s*|\s)systemd-run\b[^\n]*--on"
    r"|systemctl\s+(?:enable|start)[^\n]*\.timer\b"
    r"|(?:^|[;&|]\s*)at\s+(?:now\b|-f\b|\d{1,2}:\d{2}\b)"
    r"|\d+\s+\*\s+\*\s+\*"            # 裸 cron 表达式片段
    r"|\*/\d+\s+\*\s+\*\s+\*"
)

LOOP = (
    r"while\s+(?:true|:)\b"
    r"|\buntil\s+false\b"
    r"|for\s*\(\(\s*;\s*;\s*\)\)"
    r"|(?:^|[;&|]\s*)watch\s+(?:-\S+\s+)*\S"
)

BASH_RULES = [
    (
        re.compile(r"(?:%s)[\s\S]*(?:%s)" % (SCHEDULER, AI_CLI)),
        "用系统调度器周期性拉起 AI CLI",
    ),
    (
        re.compile(r"(?:%s)[\s\S]*(?:%s)" % (AI_CLI, SCHEDULER)),
        "用系统调度器周期性拉起 AI CLI",
    ),
    (
        re.compile(r"(?:%s)[\s\S]*(?:%s)[\s\S]*\bsleep\b" % (LOOP, AI_CLI)),
        "常驻循环内反复调用 AI CLI",
    ),
    (
        re.compile(r"(?:%s)[^\n]*--(?:cron|schedule|heartbeat|watch)\b" % AI_CLI),
        "AI CLI 的定时 / 心跳参数",
    ),
    (
        re.compile(r"(?:%s)\s*(?:schedule|cron|heartbeat)\b" % AI_CLI),
        "AI CLI 的定时 / 心跳子命令",
    ),
]

# 会话 / 平台级调度工具：这类工具的用途本身就是「稍后再唤起 AI」。
# 判定函数返回 None 表示放行，返回字符串表示拦截原因。


def check_cron_create(ti):
    # recurring 默认 true；一次性提醒（recurring=false）放行。
    if ti.get("recurring") is False:
        return None
    return "CronCreate 周期性任务（recurring）会反复唤起本会话"


def check_schedule_wakeup(ti):
    if ti.get("stop") is True:
        return None
    return "ScheduleWakeup 自唤起（/loop 自定节奏）会反复唤起本会话"


def check_create_trigger(ti):
    # 一次性 Routine（run_once_at）放行；cron_expression 为周期性，拦截。
    if ti.get("cron_expression"):
        return "Routine 的 cron_expression 会按周期反复唤起 AI 会话"
    return None


TOOL_RULES = {
    "CronCreate": check_cron_create,
    "ScheduleWakeup": check_schedule_wakeup,
    "mcp__Claude_Code_Remote__create_trigger": check_create_trigger,
    "mcp__Claude_Code_Remote__update_trigger": check_create_trigger,
}

# 仓库内触发器声明：仅当「周期性触发」与「AI CLI 调用」同时出现才拦——
# 普通 CI 的 schedule 触发器不消耗 token，不在拦截之列。
WORKFLOW_PATH = re.compile(r"(?:^|/)\.github/workflows/[^/]+\.ya?ml$")
WORKFLOW_SCHEDULE = re.compile(r"^\s*(?:schedule\s*:|-?\s*cron\s*:)", re.M)
WORKFLOW_AI = re.compile(
    r"anthropics/claude-code-action"
    r"|@anthropic-ai/claude-code"
    r"|(?<![\w-])(?:claude-code|claude|codex)(?:\s|$)",
    re.M,
)

REASON_TAIL = (
    " 本仓库禁止 AI 创建会反复自唤起、持续消耗 token 的循环任务："
    "该类任务在无人值守下按周期执行，成本上限由运行时长而非单次开销决定"
    "（已发生过一次未授权的 15 分钟心跳耗尽当周用量并额外产生费用的事故）。"
    "如确有周期性需求，请先向仓库负责人报备用途、间隔与预计消耗并获批，"
    "获批后改由人工手动触发，或由负责人在 .claude/settings.json 登记例外。"
    " 注意：本闸门只管「会反复唤起 AI 会话」的调度——"
    "项目自身的运维调度（数据库备份 cron、日志清理 timer、普通 CI 的 schedule "
    "触发器、Spring @Scheduled、前端 setInterval 等）不消耗 token，一律放行。"
)


def deny(reason):
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason + REASON_TAIL,
        }
    }, ensure_ascii=False))
    sys.exit(0)


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    tool = payload.get("tool_name") or ""
    tool_input = payload.get("tool_input") or {}
    if not isinstance(tool_input, dict):
        sys.exit(0)

    checker = TOOL_RULES.get(tool)
    if checker is not None:
        reason = checker(tool_input)
        if reason:
            deny("已拦截：{}。".format(reason))
        sys.exit(0)

    if tool == "Bash":
        command = tool_input.get("command") or ""
        for pattern, label in BASH_RULES:
            if pattern.search(command):
                deny("已拦截：命令疑似{}。".format(label))
        sys.exit(0)

    if tool in ("Write", "Edit", "NotebookEdit"):
        path = (tool_input.get("file_path") or "").replace(os.sep, "/")
        if not WORKFLOW_PATH.search(path):
            sys.exit(0)
        body = "\n".join(
            str(tool_input.get(key) or "")
            for key in ("content", "new_string", "new_source")
        )
        if WORKFLOW_SCHEDULE.search(body) and WORKFLOW_AI.search(body):
            deny("已拦截：{} 疑似配置了「定时触发 + 调用 AI」的工作流。".format(path))
        sys.exit(0)

    sys.exit(0)


if __name__ == "__main__":
    main()
