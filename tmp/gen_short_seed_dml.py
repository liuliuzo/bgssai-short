# -*- coding: utf-8 -*-
"""Generate short-production + subscription seed DML and append to sql/DML.sql."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DML = ROOT / "sql" / "DML.sql"
MARKER_BEGIN = "-- ===== BEGIN SHORT-BIZ-SEED ====="
MARKER_END = "-- ===== END SHORT-BIZ-SEED ====="

GENRES = ["REVENGE", "CEO", "TIME_TRAVEL", "SWEET", "SUSPENSE", "FANTASY"]
VISUALS = ["MODERN_URBAN", "ANCIENT_CHINESE", "ANIME", "CINEMATIC"]
WORK_STATUSES = ["DRAFT", "SCRIPTING", "PRODUCING", "COMPOSING", "REVIEWING", "COMPLETED", "ARCHIVED"]
PLANS = ["FREE", "PRO", "MAX5", "MAX10", "MAX20"]
PLAN_BAL = {"FREE": 10000, "PRO": 100000, "MAX5": 500000, "MAX10": 1000000, "MAX20": 2000000}


def sql_str(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_int(value: int | None) -> str:
    return "NULL" if value is None else str(value)


def insert(table: str, cols: list[str], vals: list[str]) -> str:
    return (
        f"INSERT INTO `{table}` ({', '.join('`' + c + '`' for c in cols)}) "
        f"VALUES ({', '.join(vals)});"
    )


def svg(title: str, subtitle: str, color: str = "#2563eb") -> str:
    t = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = subtitle.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return (
        f"<svg xmlns='http://www.w3.org/2000/svg' width='1080' height='1920' viewBox='0 0 1080 1920'>"
        f"<rect width='1080' height='1920' fill='{color}'/>"
        f"<rect x='80' y='700' width='920' height='420' rx='24' fill='#0f172a' opacity='0.55'/>"
        f"<text x='540' y='860' text-anchor='middle' fill='#fff' font-size='64' font-family='sans-serif'>{t}</text>"
        f"<text x='540' y='980' text-anchor='middle' fill='#e2e8f0' font-size='36' font-family='sans-serif'>{s}</text>"
        f"<text x='540' y='1780' text-anchor='middle' fill='#fff' font-size='28' font-family='sans-serif'>seed 9:16</text>"
        f"</svg>"
    )


def scene_json(ep_title: str, synopsis: str) -> str:
    # compact JSON, double quotes
    return (
        '[{"scene_no":1,"scene_location":"冲突现场","scene_summary":'
        + _j(synopsis)
        + ',"line_list":[{"line_no":1,"character_name":"女主","line_text":"退婚书我签，但有个条件。","emotion":"COLD","estimated_sec":3},'
        '{"line_no":2,"character_name":"男主","line_text":"你凭什么跟我谈条件？","emotion":"ANGRY","estimated_sec":3}]},'
        '{"scene_no":2,"scene_location":"三年后","scene_summary":"女主带着资源回归。","line_list":['
        '{"line_no":1,"character_name":"女主","line_text":"董事会，现在换我坐。","emotion":"CALM","estimated_sec":3},'
        '{"line_no":2,"character_name":"配角","line_text":"她怎么回来了？","emotion":"SHOCK","estimated_sec":2}]},'
        '{"scene_no":3,"scene_location":"对峙","scene_summary":'
        + _j(ep_title + "的集末钩子。")
        + ',"line_list":[{"line_no":1,"character_name":"女主","line_text":"下一次，轮到你们求我。","emotion":"COLD","estimated_sec":3},'
        '{"line_no":2,"character_name":"男主","line_text":"等着瞧。","emotion":"TENSE","estimated_sec":2}]}]'
    )


def outline_json(title: str, pitch: str, count: int, genre: str) -> str:
    parts = []
    for i in range(1, count + 1):
        if i == 1:
            et, es, eh = "开场", pitch + "。第 1 集当众爆发冲突。", "她抬眼：这事没完。"
        elif i == count:
            et, es, eh = "终局", title + "迎来收束，旧账当场清算。", "镜头定格在她转身离开的背影。"
        else:
            et, es, eh = f"第{i}集", f"第 {i} 集推进：{pitch} 的下一手。", "反派冷笑：你以为这就赢了？"
        parts.append(
            "{"
            f'"episode_no":{i},"episode_title":{_j(et)},"episode_synopsis":{_j(es)},"ending_hook":{_j(eh)}'
            "}"
        )
    return "[" + ",".join(parts) + "]"


def _j(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def rhythm_json() -> str:
    return '{"beat_score":78,"hook_score":82,"pacing_note":"前紧后稳"}'


def build() -> str:
    lines: list[str] = []
    a = lines.append

    a(MARKER_BEGIN)
    a("-- Short 业务铺底：订阅/额度 + 短剧制作闭环（作品/剧集/剧本/角色/分镜/任务/成片/审核/合规）")
    a("-- 挂在现有 USER-VOLUME-SEED 之后；虚构演示数据，禁止生产执行。")
    a("-- demo 主账号持完整闭环样例；seeduser001~080 各持轻量作品便于列表联调。")
    a("")

    # cleanup for re-run of this section
    a("-- 幂等清理本段业务种子（不影响账号 / 套餐目录 / OAuth 密钥）")
    a("DELETE FROM `short_compliance_hit` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_review_record` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_film` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_asset` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_ai_task` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_shot` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_scene_card` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_character` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_script_version` WHERE `version_id` LIKE 'sv-dm-%' OR `version_id` LIKE 'sv-sd-%';")
    a("DELETE FROM `short_script` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_episode` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `short_work` WHERE `work_id` LIKE 'w-dm-%' OR `work_id` LIKE 'w-sd-%';")
    a("DELETE FROM `quota_change_log` WHERE `creator` = 'seed' AND `remark` LIKE 'seed:%';")
    a("DELETE FROM `payment_order` WHERE `order_no` LIKE 'PO-SEED-%';")
    a("DELETE FROM `user_quota` WHERE `user_id` = 'u-demo-0001' OR `user_id` LIKE 'u-sd-%';")
    a("DELETE FROM `user_subscription` WHERE `user_id` = 'u-demo-0001' OR `user_id` LIKE 'u-sd-%';")
    a("")

    # ---- subscription / quota ----
    a("-- ##### 订阅与额度 #####")
    # demo = PRO
    a(
        insert(
            "user_subscription",
            ["user_id", "plan_code", "status", "current_period_start", "current_period_end", "source", "is_delete", "creator"],
            [
                sql_str("u-demo-0001"),
                sql_str("PRO"),
                sql_str("active"),
                sql_str("2026-08-01 00:00:00"),
                sql_str("2026-09-01 00:00:00"),
                sql_str("order"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "user_quota",
            [
                "user_id",
                "balance_cent",
                "monthly_limit_cent",
                "extra_balance_cent",
                "month_spent_cent",
                "month_reset_date",
                "is_delete",
                "creator",
            ],
            [
                sql_str("u-demo-0001"),
                "86500",
                "100000",
                "5000",
                "13500",
                sql_str("2026-09-01"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "payment_order",
            [
                "order_no",
                "user_id",
                "order_type",
                "biz_code",
                "biz_name",
                "amount_cent",
                "grant_cent",
                "payment_channel",
                "status",
                "paid_at",
                "is_delete",
                "creator",
            ],
            [
                sql_str("PO-SEED-DEMO-PRO"),
                sql_str("u-demo-0001"),
                sql_str("subscription"),
                sql_str("PRO"),
                sql_str("专业"),
                "9900",
                "100000",
                sql_str("WECHAT"),
                sql_str("paid"),
                sql_str("2026-08-01 10:00:00"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "payment_order",
            [
                "order_no",
                "user_id",
                "order_type",
                "biz_code",
                "biz_name",
                "amount_cent",
                "grant_cent",
                "payment_channel",
                "status",
                "paid_at",
                "is_delete",
                "creator",
            ],
            [
                sql_str("PO-SEED-DEMO-PACK50"),
                sql_str("u-demo-0001"),
                sql_str("credit_pack"),
                sql_str("pack_50"),
                sql_str("加量 50 元"),
                "4900",
                "5000",
                sql_str("ALIPAY"),
                sql_str("paid"),
                sql_str("2026-08-10 15:30:00"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "quota_change_log",
            [
                "user_id",
                "operator_user_id",
                "change_type",
                "balance_delta_cent",
                "extra_delta_cent",
                "balance_after_cent",
                "extra_after_cent",
                "monthly_limit_cent",
                "remark",
                "is_delete",
                "creator",
            ],
            [
                sql_str("u-demo-0001"),
                sql_str("u-demo-0001"),
                sql_str("grant_plan"),
                "100000",
                "0",
                "100000",
                "0",
                "100000",
                sql_str("seed:开通 PRO"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "quota_change_log",
            [
                "user_id",
                "operator_user_id",
                "change_type",
                "balance_delta_cent",
                "extra_delta_cent",
                "balance_after_cent",
                "extra_after_cent",
                "monthly_limit_cent",
                "remark",
                "is_delete",
                "creator",
            ],
            [
                sql_str("u-demo-0001"),
                sql_str("u-demo-0001"),
                sql_str("grant_pack"),
                "0",
                "5000",
                "100000",
                "5000",
                "100000",
                sql_str("seed:加量包 pack_50"),
                "0",
                sql_str("seed"),
            ],
        )
    )
    a(
        insert(
            "quota_change_log",
            [
                "user_id",
                "operator_user_id",
                "change_type",
                "balance_delta_cent",
                "extra_delta_cent",
                "balance_after_cent",
                "extra_after_cent",
                "monthly_limit_cent",
                "remark",
                "is_delete",
                "creator",
            ],
            [
                sql_str("u-demo-0001"),
                sql_str("u-demo-0001"),
                sql_str("consume"),
                "-13500",
                "0",
                "86500",
                "5000",
                "100000",
                sql_str("seed:制作消耗"),
                "0",
                sql_str("seed"),
            ],
        )
    )

    for i in range(1, 81):
        uid = f"u-sd-{i:04d}"
        plan = PLANS[(i - 1) % len(PLANS)]
        bal = PLAN_BAL[plan]
        spent = (i * 37) % max(1, bal // 5)
        extra = 0 if i % 4 else 2000
        a(
            insert(
                "user_subscription",
                [
                    "user_id",
                    "plan_code",
                    "status",
                    "current_period_start",
                    "current_period_end",
                    "source",
                    "is_delete",
                    "creator",
                ],
                [
                    sql_str(uid),
                    sql_str(plan),
                    sql_str("active" if i % 17 else "expired"),
                    sql_str("2026-08-01 00:00:00"),
                    sql_str("2026-09-01 00:00:00"),
                    sql_str("free" if plan == "FREE" else "order"),
                    "0",
                    sql_str("seed"),
                ],
            )
        )
        a(
            insert(
                "user_quota",
                [
                    "user_id",
                    "balance_cent",
                    "monthly_limit_cent",
                    "extra_balance_cent",
                    "month_spent_cent",
                    "month_reset_date",
                    "is_delete",
                    "creator",
                ],
                [
                    sql_str(uid),
                    sql_int(bal - spent),
                    sql_int(bal),
                    sql_int(extra),
                    sql_int(spent),
                    sql_str("2026-09-01"),
                    "0",
                    sql_str("seed"),
                ],
            )
        )
        if i % 10 == 0:
            a(
                insert(
                    "payment_order",
                    [
                        "order_no",
                        "user_id",
                        "order_type",
                        "biz_code",
                        "biz_name",
                        "amount_cent",
                        "grant_cent",
                        "payment_channel",
                        "status",
                        "paid_at",
                        "is_delete",
                        "creator",
                    ],
                    [
                        sql_str(f"PO-SEED-U{i:03d}"),
                        sql_str(uid),
                        sql_str("subscription"),
                        sql_str(plan),
                        sql_str(plan),
                        sql_int(0 if plan == "FREE" else PLAN_BAL[plan] // 10),
                        sql_int(bal),
                        sql_str("WECHAT" if i % 2 else "ALIPAY"),
                        sql_str("paid" if i % 3 else "created"),
                        sql_str("2026-08-05 12:00:00") if i % 3 else "NULL",
                        "0",
                        sql_str("seed"),
                    ],
                )
            )

    a("")
    a("-- ##### demo 完整短剧闭环样例 #####")

    # Work definitions for demo
    demo_works = [
        {
            "work_id": "w-dm-001",
            "title": "退婚三年后她成了首富",
            "pitch": "被退婚的落魄千金三年后带着资本回归，收购前未婚夫的公司",
            "genre": "REVENGE",
            "status": "PRODUCING",
            "ep_count": 4,
            "visual": "MODERN_URBAN",
            "spent": 12850,
            "budget": 300000,
            "kind": "producing",
        },
        {
            "work_id": "w-dm-002",
            "title": "重生之我在豪门当保姆",
            "pitch": "重生回到豪门别墅，她决定先摸清所有人的底牌",
            "genre": "TIME_TRAVEL",
            "status": "SCRIPTING",
            "ep_count": 3,
            "visual": "CINEMATIC",
            "spent": 450,
            "budget": None,
            "kind": "scripting",
        },
        {
            "work_id": "w-dm-003",
            "title": "甜宠小娇妻养成记",
            "pitch": "冷面总裁捡到失忆女孩，从此家里多了个小麻烦",
            "genre": "SWEET",
            "status": "DRAFT",
            "ep_count": 24,
            "visual": "ANIME",
            "spent": 0,
            "budget": None,
            "kind": "draft",
        },
        {
            "work_id": "w-dm-004",
            "title": "穿越之女帝归来",
            "pitch": "现代女强人穿越成废柴公主，朝堂风云一夜间改写",
            "genre": "FANTASY",
            "status": "COMPLETED",
            "ep_count": 3,
            "visual": "ANCIENT_CHINESE",
            "spent": 45600,
            "budget": 80000,
            "kind": "completed",
        },
        {
            "work_id": "w-dm-005",
            "title": "悬疑法医手记",
            "pitch": "年轻法医接手一桩被盖棺的旧案，真相指向自己的导师",
            "genre": "SUSPENSE",
            "status": "REVIEWING",
            "ep_count": 2,
            "visual": "CINEMATIC",
            "spent": 22000,
            "budget": 50000,
            "kind": "reviewing",
        },
        {
            "work_id": "w-dm-006",
            "title": "总裁的替身新娘",
            "pitch": "替嫁进豪门的她，发现合同婚姻背后另有交易",
            "genre": "CEO",
            "status": "ARCHIVED",
            "ep_count": 2,
            "visual": "MODERN_URBAN",
            "spent": 3200,
            "budget": None,
            "kind": "archived",
        },
    ]

    uid = "u-demo-0001"
    for w in demo_works:
        emit_demo_work(a, uid, w)

    a("")
    a("-- ##### seeduser 轻量作品（列表联调） #####")
    titles = [
        "她从灰烬里回来",
        "午夜董事会",
        "一纸婚约",
        "旧爱重逢",
        "假千金翻盘",
        "暗夜追凶",
        "仙门弃徒",
        "豪门试爱",
        "重生夺嫡",
        "甜蜜陷阱",
    ]
    for i in range(1, 81):
        uid_s = f"u-sd-{i:04d}"
        wid = f"w-sd-{i:04d}"
        genre = GENRES[(i - 1) % len(GENRES)]
        status = WORK_STATUSES[(i - 1) % len(WORK_STATUSES)]
        visual = VISUALS[(i - 1) % len(VISUALS)]
        title = titles[(i - 1) % len(titles)] + f"·{i:03d}"
        pitch = f"种子用户作品示例 {i}：题材 {genre}，状态 {status}"
        ep_count = 2 if status != "DRAFT" else 3
        outline = outline_json(title, pitch, ep_count, genre) if status != "DRAFT" else None
        a(
            insert(
                "short_work",
                [
                    "work_id",
                    "user_id",
                    "work_title",
                    "one_line_pitch",
                    "genre",
                    "work_status",
                    "episode_count",
                    "episode_duration_sec",
                    "visual_style",
                    "aspect_ratio",
                    "cost_budget_cent",
                    "cost_spent_cent",
                    "skip_level_check_flag",
                    "quality_threshold_score",
                    "outline_json",
                    "archived_flag",
                    "deleted_flag",
                ],
                [
                    sql_str(wid),
                    sql_str(uid_s),
                    sql_str(title),
                    sql_str(pitch),
                    sql_str(genre),
                    sql_str(status),
                    sql_int(ep_count),
                    "90",
                    sql_str(visual),
                    sql_str("VERTICAL_9_16"),
                    "NULL",
                    sql_int((i * 113) % 5000),
                    "0",
                    sql_int(70 + (i % 20)),
                    sql_str(outline) if outline else "NULL",
                    "1" if status == "ARCHIVED" else "0",
                    "0",
                ],
            )
        )
        # light episodes for non-draft
        if status == "DRAFT":
            continue
        for eno in range(1, ep_count + 1):
            eid = f"ep-sd-{i:04d}-{eno}"
            script_st = "READY" if status in ("PRODUCING", "COMPOSING", "REVIEWING", "COMPLETED") else ("DRAFT" if status == "SCRIPTING" else "NONE")
            board_st = "READY" if status in ("COMPOSING", "REVIEWING", "COMPLETED") else ("DRAFT" if status == "PRODUCING" else "NONE")
            dub_st = "READY" if status in ("COMPOSING", "REVIEWING", "COMPLETED") else "NONE"
            compose_st = "READY" if status in ("REVIEWING", "COMPLETED") else ("COMPOSING" if status == "COMPOSING" else "NONE")
            review_st = "APPROVED" if status == "COMPLETED" else ("PENDING" if status == "REVIEWING" else "NOT_SUBMITTED")
            a(
                insert(
                    "short_episode",
                    [
                        "episode_id",
                        "work_id",
                        "user_id",
                        "episode_no",
                        "episode_title",
                        "episode_synopsis",
                        "ending_hook",
                        "script_status",
                        "storyboard_status",
                        "dubbing_status",
                        "compose_status",
                        "review_status",
                        "film_duration_sec",
                        "deleted_flag",
                    ],
                    [
                        sql_str(eid),
                        sql_str(wid),
                        sql_str(uid_s),
                        sql_int(eno),
                        sql_str(f"第{eno}集"),
                        sql_str(pitch),
                        sql_str("下集更刺激"),
                        sql_str(script_st),
                        sql_str(board_st),
                        sql_str(dub_st),
                        sql_str(compose_st),
                        sql_str(review_st),
                        sql_int(88) if compose_st == "READY" else "NULL",
                        "0",
                    ],
                )
            )
            if script_st in ("DRAFT", "READY"):
                sid = f"sp-sd-{i:04d}-{eno}"
                a(
                    insert(
                        "short_script",
                        [
                            "script_id",
                            "episode_id",
                            "work_id",
                            "user_id",
                            "script_version",
                            "script_status",
                            "scene_json",
                            "estimated_total_sec",
                            "rhythm_json",
                            "total_score",
                            "force_ready_flag",
                        ],
                        [
                            sql_str(sid),
                            sql_str(eid),
                            sql_str(wid),
                            sql_str(uid_s),
                            "1",
                            sql_str(script_st),
                            sql_str(scene_json(f"第{eno}集", pitch)),
                            "90",
                            sql_str(rhythm_json()),
                            "80",
                            "0",
                        ],
                    )
                )
            if status in ("REVIEWING", "COMPLETED") and eno == 1:
                a(
                    insert(
                        "short_review_record",
                        [
                            "review_id",
                            "work_id",
                            "episode_id",
                            "creator_user_id",
                            "review_status",
                            "risk_level",
                            "reviewer_user_id",
                            "review_comment",
                            "reject_item_json",
                            "submitted_time",
                            "reviewed_time",
                            "deleted_flag",
                        ],
                        [
                            sql_str(f"rv-sd-{i:04d}"),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str(uid_s),
                            sql_str(review_st),
                            sql_str("PASS" if review_st == "APPROVED" else "WARN"),
                            sql_str("u-admin-0001") if review_st == "APPROVED" else "NULL",
                            sql_str("种子审核样例") if review_st == "APPROVED" else "NULL",
                            "NULL",
                            sql_str("2026-08-20 09:00:00"),
                            sql_str("2026-08-21 11:00:00") if review_st == "APPROVED" else "NULL",
                            "0",
                        ],
                    )
                )

    a("")
    a(MARKER_END)
    return "\n".join(lines) + "\n"


def emit_demo_work(a, uid: str, w: dict) -> None:
    wid = w["work_id"]
    kind = w["kind"]
    outline = None if kind == "draft" else outline_json(w["title"], w["pitch"], w["ep_count"], w["genre"])
    a(
        insert(
            "short_work",
            [
                "work_id",
                "user_id",
                "work_title",
                "one_line_pitch",
                "genre",
                "work_status",
                "episode_count",
                "episode_duration_sec",
                "visual_style",
                "aspect_ratio",
                "cover_asset_id",
                "cost_budget_cent",
                "cost_spent_cent",
                "skip_level_check_flag",
                "quality_threshold_score",
                "outline_json",
                "compose_setting_json",
                "archived_flag",
                "deleted_flag",
            ],
            [
                sql_str(wid),
                sql_str(uid),
                sql_str(w["title"]),
                sql_str(w["pitch"]),
                sql_str(w["genre"]),
                sql_str(w["status"]),
                sql_int(w["ep_count"]),
                "90",
                sql_str(w["visual"]),
                sql_str("VERTICAL_9_16"),
                sql_str(f"as-dm-{wid[-3:]}-cover") if kind in ("producing", "completed", "reviewing") else "NULL",
                sql_int(w["budget"]),
                sql_int(w["spent"]),
                "0",
                "75",
                sql_str(outline) if outline else "NULL",
                sql_str('{"compose_mode":"FULL","fps":30,"resolution":"1080x1920"}')
                if kind in ("completed", "reviewing", "producing")
                else "NULL",
                "1" if kind == "archived" else "0",
                "0",
            ],
        )
    )

    if kind == "draft":
        return

    # characters / scenes for richer works
    chars = [
        (f"ch-dm-{wid[-3:]}-01", "沈知微", "FEMALE_LEAD", "LOCKED", "二十五岁都市女性，短发，冷白皮，利落西装"),
        (f"ch-dm-{wid[-3:]}-02", "顾景深", "MALE_LEAD", "LOCKED", "三十岁精英男性，高鼻深目，深色大衣"),
        (f"ch-dm-{wid[-3:]}-03", "林小夏", "SUPPORTING", "DRAFT" if kind == "scripting" else "LOCKED", "活泼配角，马尾，校园风"),
        (f"ch-dm-{wid[-3:]}-04", "赵总", "ANTAGONIST", "CANDIDATE" if kind == "scripting" else "LOCKED", "反派中年商人，油头，金表"),
    ]
    for cid, name, role, cst, prompt in chars:
        ref = f"as-dm-{wid[-3:]}-{cid[-2:]}" if cst == "LOCKED" else None
        a(
            insert(
                "short_character",
                [
                    "character_id",
                    "work_id",
                    "user_id",
                    "character_name",
                    "character_role",
                    "card_status",
                    "appearance_prompt",
                    "consistency_seed",
                    "ref_image_asset_id",
                    "default_outfit_code",
                    "outfit_json",
                    "voice_id",
                    "voice_speed",
                    "deleted_flag",
                ],
                [
                    sql_str(cid),
                    sql_str(wid),
                    sql_str(uid),
                    sql_str(name),
                    sql_str(role),
                    sql_str(cst),
                    sql_str(prompt),
                    sql_int(10000 + (sum(ord(c) for c in cid) * 97) % 90000),
                    sql_str(ref) if ref else "NULL",
                    sql_str("OUTFIT_DEFAULT"),
                    sql_str('[{"outfit_code":"OUTFIT_DEFAULT","outfit_name":"默认正装","prompt":"黑色西装"}]'),
                    sql_str(f"voice-{cid[-2:]}"),
                    sql_str("1.0"),
                    "0",
                ],
            )
        )
        if ref:
            a(
                insert(
                    "short_asset",
                    [
                        "asset_id",
                        "user_id",
                        "work_id",
                        "asset_type",
                        "storage_key",
                        "width",
                        "height",
                        "selected_flag",
                        "deleted_flag",
                    ],
                    [
                        sql_str(ref),
                        sql_str(uid),
                        sql_str(wid),
                        sql_str("IMAGE"),
                        sql_str(svg(name, "定妆", "#7c3aed")),
                        "1080",
                        "1920",
                        "1",
                        "0",
                    ],
                )
            )

    scenes = [
        (f"sc-dm-{wid[-3:]}-01", "宴会厅", "LOCKED", "金碧辉煌的宴会厅，水晶灯，长桌"),
        (f"sc-dm-{wid[-3:]}-02", "总裁办公室", "LOCKED", "落地窗夜景，深色木桌，城市灯火"),
        (f"sc-dm-{wid[-3:]}-03", "雨夜街道", "DRAFT" if kind == "scripting" else "LOCKED", "霓虹倒影，湿漉漉的柏油路"),
    ]
    for scid, sname, cst, prompt in scenes:
        ref = f"as-dm-{wid[-3:]}-sc{scid[-2:]}" if cst == "LOCKED" else None
        a(
            insert(
                "short_scene_card",
                [
                    "scene_card_id",
                    "work_id",
                    "user_id",
                    "card_type",
                    "card_name",
                    "card_status",
                    "appearance_prompt",
                    "ref_image_asset_id",
                    "deleted_flag",
                ],
                [
                    sql_str(scid),
                    sql_str(wid),
                    sql_str(uid),
                    sql_str("SCENE"),
                    sql_str(sname),
                    sql_str(cst),
                    sql_str(prompt),
                    sql_str(ref) if ref else "NULL",
                    "0",
                ],
            )
        )
        if ref:
            a(
                insert(
                    "short_asset",
                    [
                        "asset_id",
                        "user_id",
                        "work_id",
                        "asset_type",
                        "storage_key",
                        "width",
                        "height",
                        "selected_flag",
                        "deleted_flag",
                    ],
                    [
                        sql_str(ref),
                        sql_str(uid),
                        sql_str(wid),
                        sql_str("IMAGE"),
                        sql_str(svg(sname, "场景卡", "#0ea5e9")),
                        "1080",
                        "1920",
                        "1",
                        "0",
                    ],
                )
            )

    if kind in ("producing", "completed", "reviewing"):
        a(
            insert(
                "short_asset",
                [
                    "asset_id",
                    "user_id",
                    "work_id",
                    "asset_type",
                    "storage_key",
                    "width",
                    "height",
                    "selected_flag",
                    "deleted_flag",
                ],
                [
                    sql_str(f"as-dm-{wid[-3:]}-cover"),
                    sql_str(uid),
                    sql_str(wid),
                    sql_str("IMAGE"),
                    sql_str(svg(w["title"][:8], "封面", "#111827")),
                    "1080",
                    "1920",
                    "1",
                    "0",
                ],
            )
        )

    # episodes
    for eno in range(1, w["ep_count"] + 1):
        eid = f"ep-dm-{wid[-3:]}-{eno:02d}"
        emit_demo_episode(a, uid, w, eid, eno)


def emit_demo_episode(a, uid: str, w: dict, eid: str, eno: int) -> None:
    wid = w["work_id"]
    kind = w["kind"]
    suffix = wid[-3:]

    if kind == "scripting":
        script_st = "DRAFT" if eno == 1 else ("GENERATING" if eno == 2 else "NONE")
        board_st = "NONE"
        dub_st = "NONE"
        compose_st = "NONE"
        review_st = "NOT_SUBMITTED"
        etitle = "开场" if eno == 1 else f"第{eno}集"
    elif kind == "archived":
        script_st, board_st, dub_st, compose_st, review_st = "DRAFT", "DRAFT", "NONE", "NONE", "NOT_SUBMITTED"
        etitle = f"第{eno}集"
    elif kind == "producing":
        # ep1 nearly done, ep2 mid, ep3 early, ep4 empty
        if eno == 1:
            script_st, board_st, dub_st, compose_st, review_st = "READY", "READY", "READY", "READY", "NOT_SUBMITTED"
        elif eno == 2:
            script_st, board_st, dub_st, compose_st, review_st = "READY", "DRAFT", "NONE", "NONE", "NOT_SUBMITTED"
        elif eno == 3:
            script_st, board_st, dub_st, compose_st, review_st = "DRAFT", "NONE", "NONE", "NONE", "NOT_SUBMITTED"
        else:
            script_st, board_st, dub_st, compose_st, review_st = "NONE", "NONE", "NONE", "NONE", "NOT_SUBMITTED"
        etitle = ["开场", "反击", "对峙", "终局"][eno - 1]
    elif kind == "reviewing":
        script_st, board_st, dub_st, compose_st = "READY", "READY", "READY", "READY"
        review_st = "PENDING"
        etitle = f"第{eno}集"
    elif kind == "completed":
        script_st, board_st, dub_st, compose_st, review_st = "READY", "READY", "READY", "READY", "APPROVED"
        etitle = ["开场", "高潮", "终局"][eno - 1]
    else:
        return

    film_asset = f"as-dm-{suffix}-film{eno:02d}" if compose_st == "READY" else None
    a(
        insert(
            "short_episode",
            [
                "episode_id",
                "work_id",
                "user_id",
                "episode_no",
                "episode_title",
                "episode_synopsis",
                "ending_hook",
                "script_status",
                "storyboard_status",
                "dubbing_status",
                "compose_status",
                "review_status",
                "film_asset_id",
                "film_duration_sec",
                "deleted_flag",
            ],
            [
                sql_str(eid),
                sql_str(wid),
                sql_str(uid),
                sql_int(eno),
                sql_str(etitle),
                sql_str(w["pitch"] if eno == 1 else f"{w['title']}第{eno}集推进"),
                sql_str("她抬眼：这事没完。" if eno == 1 else "反派冷笑：你以为这就赢了？"),
                sql_str(script_st),
                sql_str(board_st),
                sql_str(dub_st),
                sql_str(compose_st),
                sql_str(review_st),
                sql_str(film_asset) if film_asset else "NULL",
                "88" if film_asset else "NULL",
                "0",
            ],
        )
    )

    if script_st in ("DRAFT", "READY", "GENERATING"):
        sid = f"sp-dm-{suffix}-{eno:02d}"
        sj = scene_json(etitle, w["pitch"])
        a(
            insert(
                "short_script",
                [
                    "script_id",
                    "episode_id",
                    "work_id",
                    "user_id",
                    "script_version",
                    "script_status",
                    "scene_json",
                    "estimated_total_sec",
                    "rhythm_json",
                    "total_score",
                    "force_ready_flag",
                ],
                [
                    sql_str(sid),
                    sql_str(eid),
                    sql_str(wid),
                    sql_str(uid),
                    "2" if script_st == "READY" else "1",
                    sql_str("DRAFT" if script_st == "GENERATING" else script_st),
                    sql_str(sj),
                    "90",
                    sql_str(rhythm_json()),
                    "86" if script_st == "READY" else "72",
                    "0",
                ],
            )
        )
        a(
            insert(
                "short_script_version",
                ["version_id", "script_id", "script_version", "scene_json", "total_score", "change_summary"],
                [
                    sql_str(f"sv-dm-{suffix}-{eno:02d}-1"),
                    sql_str(sid),
                    "1",
                    sql_str(sj),
                    "70",
                    sql_str("初稿"),
                ],
            )
        )
        if script_st == "READY":
            a(
                insert(
                    "short_script_version",
                    ["version_id", "script_id", "script_version", "scene_json", "total_score", "change_summary"],
                    [
                        sql_str(f"sv-dm-{suffix}-{eno:02d}-2"),
                        sql_str(sid),
                        "2",
                        sql_str(sj),
                        "86",
                        sql_str("定稿"),
                    ],
                )
            )

    # shots for producing/reviewing/completed when board ready or draft
    if board_st in ("DRAFT", "READY"):
        shot_statuses = (
            ["LOCKED", "LOCKED", "LOCKED", "LOCKED", "LOCKED", "LOCKED"]
            if board_st == "READY"
            else ["LOCKED", "VIDEO_READY", "IMAGE_READY", "PENDING", "FAILED", "IMAGE_GENERATING"]
        )
        for sno, sst in enumerate(shot_statuses, start=1):
            shid = f"sh-dm-{suffix}-{eno:02d}-{sno:02d}"
            img_asset = f"as-dm-{suffix}-s{eno:02d}i{sno:02d}" if sst not in ("PENDING", "FAILED", "IMAGE_GENERATING") else None
            vid_asset = f"as-dm-{suffix}-s{eno:02d}v{sno:02d}" if sst in ("VIDEO_READY", "LOCKED") else None
            blocked = 1 if sst == "FAILED" else 0
            risk = "WARN" if sno == 4 else ("BLOCK" if sst == "FAILED" else "PASS")
            a(
                insert(
                    "short_shot",
                    [
                        "shot_id",
                        "episode_id",
                        "work_id",
                        "user_id",
                        "shot_no",
                        "scene_no",
                        "shot_size",
                        "camera_move",
                        "shot_duration_sec",
                        "shot_desc",
                        "dialogue_text",
                        "emotion",
                        "lighting",
                        "character_ref_json",
                        "scene_card_id",
                        "extra_prompt",
                        "image_prompt",
                        "previz_text",
                        "shot_status",
                        "blocked_flag",
                        "risk_level",
                        "first_frame_asset_id",
                        "video_asset_id",
                        "retry_count",
                        "cost_spent_cent",
                        "shot_version",
                        "deleted_flag",
                    ],
                    [
                        sql_str(shid),
                        sql_str(eid),
                        sql_str(wid),
                        sql_str(uid),
                        sql_int(sno),
                        sql_int(1 + (sno - 1) // 2),
                        sql_str(["MEDIUM", "CLOSE", "LONG", "PANORAMA", "EXTREME_CLOSE", "MEDIUM"][sno - 1]),
                        sql_str(["FIXED", "PUSH", "PAN", "FOLLOW", "HANDHELD", "PULL"][sno - 1]),
                        "3",
                        sql_str(f"{etitle}镜头{sno}：角色对峙与反应"),
                        sql_str("退婚书我签，但有个条件。" if sno % 2 else "你凭什么跟我谈条件？"),
                        sql_str("COLD" if sno % 2 else "ANGRY"),
                        sql_str("冷色侧光"),
                        sql_str(f'["ch-dm-{suffix}-01","ch-dm-{suffix}-02"]'),
                        sql_str(f"sc-dm-{suffix}-0{(1 if sno < 4 else 2)}"),
                        "NULL",
                        sql_str(f"vertical 9:16, {w['visual']}, shot {sno}"),
                        sql_str(f"预演：镜{sno} 女主抬眼对峙"),
                        sql_str(sst),
                        sql_int(blocked),
                        sql_str(risk),
                        sql_str(img_asset) if img_asset else "NULL",
                        sql_str(vid_asset) if vid_asset else "NULL",
                        "1" if sst == "FAILED" else "0",
                        sql_int((50 if img_asset else 0) + (150 if vid_asset else 0)),
                        "1",
                        "0",
                    ],
                )
            )
            if img_asset:
                a(
                    insert(
                        "short_asset",
                        [
                            "asset_id",
                            "user_id",
                            "work_id",
                            "episode_id",
                            "shot_id",
                            "asset_type",
                            "storage_key",
                            "width",
                            "height",
                            "source_task_id",
                            "selected_flag",
                            "deleted_flag",
                        ],
                        [
                            sql_str(img_asset),
                            sql_str(uid),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str(shid),
                            sql_str("IMAGE"),
                            sql_str(svg(f"镜{sno}", etitle, "#2563eb")),
                            "1080",
                            "1920",
                            sql_str(f"tk-dm-{suffix}-img-{eno:02d}-{sno:02d}"),
                            "1",
                            "0",
                        ],
                    )
                )
                a(
                    insert(
                        "short_ai_task",
                        [
                            "task_id",
                            "batch_id",
                            "user_id",
                            "work_id",
                            "episode_id",
                            "task_type",
                            "task_status",
                            "target_type",
                            "target_id",
                            "model_code",
                            "progress_percent",
                            "cost_cent",
                            "queued_time",
                            "started_time",
                            "finished_time",
                        ],
                        [
                            sql_str(f"tk-dm-{suffix}-img-{eno:02d}-{sno:02d}"),
                            sql_str(f"bt-dm-{suffix}-{eno:02d}"),
                            sql_str(uid),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str("IMAGE"),
                            sql_str("SUCCEEDED"),
                            sql_str("SHOT"),
                            sql_str(shid),
                            sql_str("mock"),
                            "100",
                            "50",
                            sql_str("2026-08-18 10:00:00"),
                            sql_str("2026-08-18 10:00:01"),
                            sql_str("2026-08-18 10:00:05"),
                        ],
                    )
                )
            if vid_asset:
                a(
                    insert(
                        "short_asset",
                        [
                            "asset_id",
                            "user_id",
                            "work_id",
                            "episode_id",
                            "shot_id",
                            "asset_type",
                            "storage_key",
                            "duration_sec",
                            "width",
                            "height",
                            "source_task_id",
                            "selected_flag",
                            "deleted_flag",
                        ],
                        [
                            sql_str(vid_asset),
                            sql_str(uid),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str(shid),
                            sql_str("VIDEO"),
                            sql_str(svg(f"视频{sno}", etitle, "#059669")),
                            "3",
                            "1080",
                            "1920",
                            sql_str(f"tk-dm-{suffix}-vid-{eno:02d}-{sno:02d}"),
                            "1",
                            "0",
                        ],
                    )
                )
                a(
                    insert(
                        "short_ai_task",
                        [
                            "task_id",
                            "batch_id",
                            "user_id",
                            "work_id",
                            "episode_id",
                            "task_type",
                            "task_status",
                            "target_type",
                            "target_id",
                            "model_code",
                            "progress_percent",
                            "cost_cent",
                            "queued_time",
                            "started_time",
                            "finished_time",
                        ],
                        [
                            sql_str(f"tk-dm-{suffix}-vid-{eno:02d}-{sno:02d}"),
                            sql_str(f"bt-dm-{suffix}-{eno:02d}"),
                            sql_str(uid),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str("VIDEO"),
                            sql_str("SUCCEEDED"),
                            sql_str("SHOT"),
                            sql_str(shid),
                            sql_str("mock"),
                            "100",
                            "150",
                            sql_str("2026-08-18 10:01:00"),
                            sql_str("2026-08-18 10:01:01"),
                            sql_str("2026-08-18 10:01:08"),
                        ],
                    )
                )
            if sst == "FAILED":
                a(
                    insert(
                        "short_ai_task",
                        [
                            "task_id",
                            "batch_id",
                            "user_id",
                            "work_id",
                            "episode_id",
                            "task_type",
                            "task_status",
                            "target_type",
                            "target_id",
                            "model_code",
                            "progress_percent",
                            "fail_reason",
                            "fail_reason_code",
                            "retry_count",
                            "cost_cent",
                            "queued_time",
                            "started_time",
                            "finished_time",
                        ],
                        [
                            sql_str(f"tk-dm-{suffix}-fail-{eno:02d}-{sno:02d}"),
                            sql_str(f"bt-dm-{suffix}-{eno:02d}"),
                            sql_str(uid),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str("IMAGE"),
                            sql_str("FAILED"),
                            sql_str("SHOT"),
                            sql_str(shid),
                            sql_str("mock"),
                            "40",
                            sql_str("内容命中拦截词"),
                            sql_str("CONTENT_BLOCKED"),
                            "1",
                            "0",
                            sql_str("2026-08-18 10:02:00"),
                            sql_str("2026-08-18 10:02:01"),
                            sql_str("2026-08-18 10:02:03"),
                        ],
                    )
                )
                a(
                    insert(
                        "short_compliance_hit",
                        [
                            "hit_id",
                            "work_id",
                            "episode_id",
                            "shot_id",
                            "check_scope",
                            "risk_level",
                            "risk_category",
                            "hit_text",
                            "suggestion",
                            "scene_no",
                            "line_no",
                            "shot_no",
                        ],
                        [
                            sql_str(f"hit-dm-{suffix}-{eno:02d}-{sno:02d}"),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str(shid),
                            sql_str("SHOT"),
                            sql_str("BLOCK"),
                            sql_str("VIOLENCE"),
                            sql_str("血腥描写"),
                            sql_str("改为暗示性冲突，避免直接血腥场面"),
                            "2",
                            "1",
                            sql_int(sno),
                        ],
                    )
                )
            if risk == "WARN":
                a(
                    insert(
                        "short_compliance_hit",
                        [
                            "hit_id",
                            "work_id",
                            "episode_id",
                            "shot_id",
                            "check_scope",
                            "risk_level",
                            "risk_category",
                            "hit_text",
                            "suggestion",
                            "scene_no",
                            "shot_no",
                        ],
                        [
                            sql_str(f"hit-dm-{suffix}-w{eno:02d}-{sno:02d}"),
                            sql_str(wid),
                            sql_str(eid),
                            sql_str(shid),
                            sql_str("SHOT"),
                            sql_str("WARN"),
                            sql_str("OTHER"),
                            sql_str("情绪激烈对白"),
                            sql_str("可保留，注意语气温和度"),
                            "1",
                            sql_int(sno),
                        ],
                    )
                )

    if compose_st == "READY":
        film_id = f"fl-dm-{suffix}-{eno:02d}"
        film_asset = f"as-dm-{suffix}-film{eno:02d}"
        export_json = (
            '{"resolution":"1080x1920","fps":30,"versions":["FULL","NO_SUBTITLE"],'
            '"track_list":[{"track_type":"VIDEO","clip_count":6},{"track_type":"AUDIO","clip_count":6},'
            '{"track_type":"SUBTITLE","clip_count":6}]}'
        )
        a(
            insert(
                "short_film",
                [
                    "film_id",
                    "episode_id",
                    "work_id",
                    "user_id",
                    "film_asset_id",
                    "film_duration_sec",
                    "compose_status",
                    "expired_flag",
                    "subtitle_stale_flag",
                    "compose_task_id",
                    "composed_time",
                    "export_json",
                    "deleted_flag",
                ],
                [
                    sql_str(film_id),
                    sql_str(eid),
                    sql_str(wid),
                    sql_str(uid),
                    sql_str(film_asset),
                    "88",
                    sql_str("READY"),
                    "0",
                    "0",
                    sql_str(f"tk-dm-{suffix}-cmp-{eno:02d}"),
                    sql_str("2026-08-19 16:00:00"),
                    sql_str(export_json),
                    "0",
                ],
            )
        )
        a(
            insert(
                "short_asset",
                [
                    "asset_id",
                    "user_id",
                    "work_id",
                    "episode_id",
                    "asset_type",
                    "storage_key",
                    "duration_sec",
                    "width",
                    "height",
                    "source_task_id",
                    "selected_flag",
                    "deleted_flag",
                ],
                [
                    sql_str(film_asset),
                    sql_str(uid),
                    sql_str(wid),
                    sql_str(eid),
                    sql_str("FILM"),
                    sql_str(export_json),
                    "88",
                    "1080",
                    "1920",
                    sql_str(f"tk-dm-{suffix}-cmp-{eno:02d}"),
                    "1",
                    "0",
                ],
            )
        )
        a(
            insert(
                "short_ai_task",
                [
                    "task_id",
                    "batch_id",
                    "user_id",
                    "work_id",
                    "episode_id",
                    "task_type",
                    "task_status",
                    "target_type",
                    "target_id",
                    "model_code",
                    "progress_percent",
                    "cost_cent",
                    "queued_time",
                    "started_time",
                    "finished_time",
                ],
                [
                    sql_str(f"tk-dm-{suffix}-cmp-{eno:02d}"),
                    sql_str(f"bt-dm-{suffix}-cmp"),
                    sql_str(uid),
                    sql_str(wid),
                    sql_str(eid),
                    sql_str("COMPOSE"),
                    sql_str("SUCCEEDED"),
                    sql_str("EPISODE"),
                    sql_str(eid),
                    sql_str("mock"),
                    "100",
                    "80",
                    sql_str("2026-08-19 15:59:00"),
                    sql_str("2026-08-19 15:59:01"),
                    sql_str("2026-08-19 16:00:00"),
                ],
            )
        )

    if review_st in ("PENDING", "APPROVED", "REJECTED"):
        reject = None
        if review_st == "REJECTED":
            reject = '[{"reject_code":"VIOLENCE","reject_desc":"画面过于激烈","locate":"shot_no=5"}]'
        a(
            insert(
                "short_review_record",
                [
                    "review_id",
                    "work_id",
                    "episode_id",
                    "creator_user_id",
                    "review_status",
                    "risk_level",
                    "reviewer_user_id",
                    "review_comment",
                    "reject_item_json",
                    "submitted_time",
                    "reviewed_time",
                    "deleted_flag",
                ],
                [
                    sql_str(f"rv-dm-{suffix}-{eno:02d}"),
                    sql_str(wid),
                    sql_str(eid),
                    sql_str(uid),
                    sql_str(review_st),
                    sql_str("PASS" if review_st == "APPROVED" else "WARN"),
                    sql_str("u-admin-0001") if review_st == "APPROVED" else "NULL",
                    sql_str("内容合规，通过") if review_st == "APPROVED" else "NULL",
                    sql_str(reject) if reject else "NULL",
                    sql_str("2026-08-20 09:30:00"),
                    sql_str("2026-08-21 14:00:00") if review_st == "APPROVED" else "NULL",
                    "0",
                ],
            )
        )

    # scripting TEXT task sample
    if kind == "scripting" and eno == 2:
        a(
            insert(
                "short_ai_task",
                [
                    "task_id",
                    "batch_id",
                    "user_id",
                    "work_id",
                    "episode_id",
                    "task_type",
                    "task_status",
                    "target_type",
                    "target_id",
                    "model_code",
                    "progress_percent",
                    "cost_cent",
                    "queued_time",
                    "started_time",
                ],
                [
                    sql_str(f"tk-dm-{suffix}-txt-{eno:02d}"),
                    sql_str(f"bt-dm-{suffix}-txt"),
                    sql_str(uid),
                    sql_str(wid),
                    sql_str(eid),
                    sql_str("TEXT"),
                    sql_str("RUNNING"),
                    sql_str("EPISODE"),
                    sql_str(eid),
                    sql_str("mock"),
                    "55",
                    "15",
                    sql_str("2026-08-22 11:00:00"),
                    sql_str("2026-08-22 11:00:01"),
                ],
            )
        )


def main() -> None:
    fragment = build()
    text = DML.read_text(encoding="utf-8")
    if MARKER_BEGIN in text:
        start = text.index(MARKER_BEGIN)
        end = text.index(MARKER_END, start) + len(MARKER_END)
        # keep trailing newline after old block
        after = text[end:]
        if after.startswith("\n"):
            after = after[1:]
        text = text[:start].rstrip() + "\n\n" + fragment + after
    else:
        text = text.rstrip() + "\n\n" + fragment
    DML.write_text(text, encoding="utf-8")
    print(f"updated {DML} (+{fragment.count(chr(10))} lines)")


if __name__ == "__main__":
    main()
