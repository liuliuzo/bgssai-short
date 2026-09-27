# 用户登录页整理（详设，2026-09-26）

> 需求：`docs/feature/login-page-cleanup-20260926.md`
> 原型：`docs/demo-static/web/user/login.html`

| 文件 | 改动 |
| --- | --- |
| `bgssai-short-user/bgssai-short-user-react/src/pages/login/Login.jsx` | 删左侧 `aside.login-hero` 与副标题；`LOGIN_METHODS` = password / email / sms 平铺（role=tab，默认 sms）；label 加 `sr-only`；`countdowns = { sms, email }`，`countdown` 取当前方式；主按钮按方式显示「登录」或「登录 / 注册」；发码提示去掉 `product#seq`；OAuth 按钮去掉 `title` |
| `login.css` | `.login-shell` 单列；`.login-panel-brand` 常显；删 `.login-hero*`、`.login-sub`、`.login-note` / `.login-field-note`；新增 `.sr-only` |

后端无改动。
