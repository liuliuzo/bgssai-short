# 用户端壳内窄栏（HOW）

需求权威：[`../feature/mobile-in-bot-shell.md`](../feature/mobile-in-bot-shell.md)。对照产品线 `MOBILE-IN-BOT-SHELL.md`。

- 检测：`src/lib/bgssaiShell.js` 读 `bgssai_shell` / `chat_pane`，写入 `sessionStorage`，给 `html` 打 `data-bgssai-shell="1"`。
- 布局：`MainLayout` 壳内藏侧栏、出底栏；登录页藏 Hero。样式只挂在 `html[data-bgssai-shell="1"]`。
- 独立桌面：无参数、无 session 标记时零改动。
- 安全：参数不是凭证；不进 JWT、不进 query 密钥、不自动登录。
- 范围：仅用户端。管理端本轮不改。无 DDL / DML。
