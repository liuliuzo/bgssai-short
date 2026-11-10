# Bgssai Bot 下载页开发站视觉校准

版本：2026-11-09。参考：https://dev.user.bgssai-long.com/download/bot。本需求仅补齐下载页面视觉差异，覆盖本仓现有下载入口（Web 仓包括 user/admin）。取代此前 Long 风格说明中的标题、留白与卡片尺寸；其余业务口径沿用。

## 验收

- 主标题 24px、32px 行高，衬线字体；原生端按对应点数和平台排版适配。已有应用外壳标题不因内嵌下载列表而改变。
- 内容最大宽度 880px，桌面和窄屏留白 32px 20px 60px；原生端保留安全区域。
- 双径向浅绿/暖纸渐变叠加原线性渐变。白色表格卡片 24px 内距、4px 圆角、#f0f0f0 边框；表格分隔线 #f0f0f0。
- 保留 44px 触控目标、键盘焦点和横向滚动，避免复制线上手机表格逐字折行缺陷。
- 官方名称保持 Bgssai Bot。保留本仓发布状态、真实下载 URL、版本/签名/架构、语言和返回路径；不得凭原型制造可下载包。

## 边界

仅下载页样式。无 API、路由、数据库、支付或安装器逻辑变化。开发站此次返回 HTTP 200，资源 Last-Modified 为 2026-11-05；它不是最新 develop 已部署的证据。本 PR 不部署。

原型：`docs/demo-static/web/user/bot-download-dev-reference.html`。设计：`docs/architecture/bot-download-dev-reference.md`。
