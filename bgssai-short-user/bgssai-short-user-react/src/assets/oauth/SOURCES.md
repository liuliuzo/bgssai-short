# OAuth brand assets — sources

Officially published platform assets (or lossless packaging of those bitmaps).
**Not** hand redraws / Simple Icons clones.

| File | Platform | Status | Official source | Retrieved |
|---|---|---|---|---|
| `wechat.png` / `.svg` | WeChat | Official | https://res.wx.qq.com/open/zh_CN/htmledition/res/assets/res-design-download/icon64_wx_logo.png | 2026-09-11 |
| `alipay.png` / `.svg` | Alipay | Official | https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*fvA9RriFISEAAAAAgCAAAAgAAQAAAQ/支付宝收银台视觉规范和素材.zip (extracted `支付宝logo-方形.png`) | 2026-09-11 |
| `weibo.png` / `.svg` | Weibo | Official | http://www.sinaimg.cn/blog/developer/wiki/LOGO_64x64.png | 2026-09-11 |
| `baidu.png` / `.svg` | Baidu | Official site icon | https://www.baidu.com/favicon.ico (no separate OAuth logo pack URL provided for this change) | 2026-09-11 |
| `douyin.png` / `.svg` | Douyin | **TODO** | No public official download URL available. Current files are a **neutral placeholder** (not a Douyin brand redraw). Replace when Douyin Open Platform publishes a public asset pack. | 2026-09-11 |

Notes:

- `.svg` files embed the same PNG as base64 so existing `<img src="…/*.svg">` references keep working.
- Official bitmaps are scaled to 128×128 with Lanczos for UI density; originals remain at the download URLs above.
- Product OAuth login currently exposes WeChat / Alipay / Douyin / Baidu. Weibo assets are kept for mobile/demo parity and future binding UI.
