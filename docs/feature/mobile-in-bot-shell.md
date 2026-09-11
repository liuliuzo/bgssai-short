# 用户端壳内窄栏布局（mobile-in-bot-shell）

对照产品线 `MOBILE-IN-BOT-SHELL.md`。本仓只做用户端 MVP；管理端本轮跳过。

## 一、背景与目标

- 背景：bgssai-bot / Chat 把短剧用户端嵌在对话旁的窄栏里。独立桌面侧栏约 220px，在 320–480px 触控栏里不可用。
- 目标：识别壳参数后切换触控窄栏；无参数时独立桌面零改动。
- 关联角色：`USER`。`PLATFORM_ADMIN` 不在本切片。

## 二、需求澄清（WHAT）

- 用户故事：作为创作者，我希望在 Bot 对话旁的窄栏里用手指点完登录、作品和任务，而不把独立电脑浏览器挤成手机页。
- 业务规则：
  1. 壳模式只看查询参数 `bgssai_shell=1` 或 `chat_pane=1`（`1` / `true` / `yes`）。search 与 hash 均可。
  2. 显式 `0` / `false` / `no` 退出壳模式。
  3. 进入后写入 `sessionStorage.bgssai_shell=1`，SPA 丢 query 仍保持；关标签即清。
  4. **禁止**用窗口宽度单独判定壳模式。独立桌面、独立手机浏览器都不是 Bot 壳。
  5. 参数只改布局。不改鉴权、不提升权限、不携带密钥、不自动登录。
  6. 壳内：隐藏左侧栏；顶栏压缩；底栏主导航（触控热区 >= 44px）；内容单列；尊重 `safe-area-inset-*`。
  7. 不新增接口，不改 `sql/`。

## 三、验收标准

1. `?bgssai_shell=1` 或 `?chat_pane=1` 进入窄栏：无侧栏、有底栏、登录为单列。
2. 无上述参数时，独立桌面布局与本切片前一致。
3. 壳内跳转到无 query 的路由后仍保持窄栏；`?bgssai_shell=0` 恢复独立布局。
4. 文档与原型不出现密钥、token、口令样例。
5. 管理端不改。

## 四、接口契约

无新接口。页面仍走既有用户端契约。壳参数不是 API 字段，不进请求体。

## 五、流程

见 [`flow-mobile-in-bot-shell.html`](./flow-mobile-in-bot-shell.html)。

## 六、数据模型影响

无。

## 七、原型

- 涉及端：web 用户端
- 页面：[`../demo-static/web/user/shell-pane.html`](../demo-static/web/user/shell-pane.html)
- 跳转：登记 [`../demo-static/web/user/PAGE-FLOW.html`](../demo-static/web/user/PAGE-FLOW.html)（布局模式，不是新路由）
- 映射：[`../api/page-api-mapping.md`](../api/page-api-mapping.md)

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`shell-pane.html` + 用户端 `PAGE-FLOW.html`）
- [x] 设计与契约（本文件 + 映射表；无安全密钥变更）
- [x] 代码（用户端 React；无 SQL）
