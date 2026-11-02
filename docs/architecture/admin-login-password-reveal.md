# 管理端登录密码可见切换

需求：[`../feature/admin-login-password-reveal.md`](../feature/admin-login-password-reveal.md)。
原型：[`../demo-static/web/admin/login.html`](../demo-static/web/admin/login.html)。
契约：[`../api/auth-login.md`](../api/auth-login.md)。登录接口不改。

管理端 `Login.jsx` 用本地状态控制密码框 `type`：默认 `password`，点击后为 `text`。按钮 `type="button"`，`aria-controls` 指向密码框，`aria-pressed` 与是否明文一致。图标仅作装饰。

不新增后端字段、接口或数据库变更。用户端既有 `showPw` 不在本次范围。
