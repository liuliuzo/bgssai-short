# 网站 ICP 备案展示

已备案并对外开放的网站，必须在页脚展示与该域名对应的 ICP 备案号，并链接工信部备案查询站。本条是强制规则。不得从产品页脚删除备案号。

## 强制规则

1. 按华为云 / 工信部备案要求：已经完成备案并且对外开放的网站，必须在页脚展示对应的 ICP 备案号，链接到工信部官方站点 `https://beian.miit.gov.cn/`。链接使用 `target="_blank"` 与 `rel="noopener noreferrer"`。
2. 这是强制项。不得从产品页脚移除备案号。
3. 使用该产品域名对应的完整备案号。不得编造、缩写或调换后缀。`-1` 至 `-17` 各自属于下表中的域名，不能互相替换。
4. 保留五条官网法律链接（`https://www.bgssai.com/{slug}/` 与 `https://www.bgssai.com/en/{slug}/`）。登录同意句仍只有用户协议和隐私政策。展示备案号不代替、也不减少这五条法律链接。

本仓域名是 `bgssai-short.com`，页脚只用 **苏ICP备2024070062号-15**。用户端与管理端的登录页和登录后外壳都展示这一行。中文备案号不翻译。不推导公安备案、经营性许可证或原生 App 备案号。不新增接口、鉴权、配置、数据库表或数据。

## 公司备案号对照

| 备案号 | 域名 |
| --- | --- |
| 苏ICP备2024070062号-1 | bgssai.com |
| 苏ICP备2024070062号-2 | bgssai-blog.com |
| 苏ICP备2024070062号-3 | bgssai-marklens.com |
| 苏ICP备2024070062号-4 | bgssai-publish.com |
| 苏ICP备2024070062号-5 | bgssai-magic.com |
| 苏ICP备2024070062号-6 | bgssai-voiceunion.com |
| 苏ICP备2024070062号-7 | bgssai-geo.cn |
| 苏ICP备2024070062号-8 | bgssai-wiki.com |
| 苏ICP备2024070062号-9 | bgssai-hr.com |
| 苏ICP备2024070062号-10 | bgssai-chat.cn |
| 苏ICP备2024070062号-11 | bgssai-saas.cn |
| 苏ICP备2024070062号-12 | bgssai-tokenhub.cn |
| 苏ICP备2024070062号-13 | bgssai-financial.com |
| 苏ICP备2024070062号-14 | bgssai-note.com |
| 苏ICP备2024070062号-15 | bgssai-short.com |
| 苏ICP备2024070062号-16 | bgssai-web.com |
| 苏ICP备2024070062号-17 | bgssai.cn |

## 本产品页脚

- 用户端页脚是一行浅灰中文：版权，然后五条官网法律链接，然后 `苏ICP备2024070062号-15`。
- 管理端页脚是这五条的中英对照、版权行，以及独立的备案行。
- 五条官网页面：用户协议、隐私政策、个人信息收集清单、第三方信息共享清单、应用权限说明。地址只使用 `https://www.bgssai.com/{slug}/` 与 `/en/{slug}/`。
- 登录同意勾选句只有《用户协议》和《隐私政策》，不写入备案号，也不写入另外三份法律页。
- 用户端页脚居中，窄屏可以换行，备案号本身不从中间断开。不得覆盖正文或客服入口。
- 登录后页脚排在内容区下方，不盖住镜头、首帧、人话或抽屉按钮，也不拦截这些点击。

2026-10-20 主办者审核通过通知确认 `bgssai-short.com` 的网站备案号为 `苏ICP备2024070062号-15`。2026-11-14 起，页脚同时保留五条官网法律外链和该备案号。早前「产品页脚不展示备案号」的口径已撤销。

原型见 `../demo-static/web/{user,admin}/icp-filing-footer.html`；设计见 `../architecture/icp-filing-footer.md`。验收分别记录源码、构建、PR 合并、Jenkins 部署及线上复核，dev/prod 均从 `develop` 部署并使用各自的 properties。
