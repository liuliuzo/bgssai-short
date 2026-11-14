# 法律页外链安全说明

状态：**需法务审阅**。需求见 [`../feature/legal-fan-out.md`](../feature/legal-fan-out.md)。

- 本仓不托管服务条款 / 隐私政策等正文，避免产品仓与官网条款漂移。
- 链接只指向官网 `https://www.bgssai.com` 权威路径（中文无前缀、英文 `/en/`，均带尾斜杠），新标签打开，不携带 JWT、口令或 token。
- 禁止 `www.bgssai.cn`，禁止 `/legal/user-agreement`、`children-privacy`、`personal-info-collection`。
- 管理端不新增法律页路由，不把运营后台做成条款托管点。
- 产品页脚不展示 ICP 备案号。`苏ICP备2024070062号-15` 仍是本域名已核准号码，只用于拒绝其他编造号码，不链到工信部页脚。禁止在页脚或登录区编造其他 ICP 备案号、公安备案号、增值电信业务经营许可证号。
- 本切片不改鉴权、CORS、密钥与回调验签。
