# 管理端登录密码可见切换

> 原型：`docs/demo-static/web/admin/login.html`
> 详设：`docs/architecture/admin-login-password-reveal.md`
> 契约：`docs/api/auth-login.md`（请求体不变）
> 配图：[`flow-admin-login-password-reveal.html`](./flow-admin-login-password-reveal.html)

## 背景

管理端登录密码框一直是掩码，输错后无法在本页核对。用户端密码登录已可点击显示或隐藏，管理端仍缺这一步。

## 需求

1. 管理端登录密码默认掩码。密码框内按钮可切换为明文，再点恢复掩码。
2. 按钮是独立控件：不提交表单；读屏名称为「显示密码」或「隐藏密码」；按下状态与当前是否明文一致；键盘可聚焦。
3. 不改 `POST /bgssai/admin/login` 的路径、请求体或响应。用户端现有开关保持不动。

## 验收

1. 打开管理端登录，密码框为掩码；点击后可见刚输入的字符；再点恢复掩码。
2. 点击该按钮不会发起登录。登录请求体仍只有 `login_type`、`identifier`、`password`。
