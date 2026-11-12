# 用户登录页整理（详设，2026-09-26）

> 需求：`docs/feature/login-page-cleanup-20260926.md`
> 原型：`docs/demo-static/web/user/login.html`

| 文件 | 改动 |
| --- | --- |
| `bgssai-short-user/bgssai-short-user-react/src/pages/login/Login.jsx` | 删左侧 `aside.login-hero` 与副标题；`LOGIN_METHODS` = password / email / sms 平铺（role=tab，默认 sms）；label 加 `sr-only`；`countdowns = { sms, email }`，`countdown` 取当前方式；主按钮按方式显示「登录」或「登录 / 注册」；发码提示去掉 `product#seq`；OAuth 按钮去掉 `title` |
| `login.css` | `.login-shell` 单列；`.login-panel-brand` 常显；删 `.login-hero*`、`.login-sub`、`.login-note` / `.login-field-note`；新增 `.sr-only` |

登录接口无改动。`/en` 的 302 在用户端与管理端的 `EnglishEntryRedirectFilter`。

同意句允许在卡片内换行。空提交只在未通过的字段下写一条 `role=alert`（密码页账号为空是「请输入账号」，挂在账号框下）。同一校验不再 `message.warning`，也不在主按钮下重复。
Ant Design `button.autoInsertSpace` 关闭，两个汉字的按钮不再被插入空格。页面跳转与字段不变，原型 HTML 不改。
`/en`、`/en/`、`/en/*` 的 GET / HEAD 由 `EnglishEntryRedirectFilter` 返回 302，`Location` 为 `/login` 并带上原查询串。前端路由仍保留；文档若仍被返回，则用 `location.replace` 离开 `/en`。浏览器保留 hash。

## 2026-11-12 区号箭头（t484）

`PhoneDialSearchSelect` 的 `.phone-dial-combo-caret` 由 border 实心三角改为两段圆角描边构成的 V 形 chevron；`aria-expanded=true` 时仍 `rotate(180deg)`。不改区号数据、点击区与登录契约。管理端无此控件。
