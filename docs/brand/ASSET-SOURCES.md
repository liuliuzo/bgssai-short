# Brand asset sources

## Official BGSSAI platform marks (in-repo)

| Asset | Canonical path | Also synced to | Source |
| --- | --- | --- | --- |
| BGSS mark | `docs/demo-static/web/assets/bgss-mark.png` | `bgssai-short-user/.../public/brand/`, `bgssai-short-admin/.../public/brand/`, backend `static/brand/` | Official BGSSAI product mark (2×2 BGSS block letterform; blue + gold). |
| BGSS mark tile | `docs/demo-static/web/assets/bgss-mark-tile.png` | same `public/brand/` trees | Official square/tile variant for login hero and panel brand slots. |
| Favicon PNG / ICO / apple-touch | React `public/favicon.png`, `favicon.ico`, `apple-touch-icon.png` | backend `static/` mirrors | Derived from the official BGSS mark. |

## Open-platform app icon

| Asset | Path | Source |
| --- | --- | --- |
| Alipay open-platform app icon (320×320) | `payment/alipay/app_icon_320.png` | Regenerated from `docs/demo-static/web/assets/bgss-mark.png` (scale + pad to 320×320). This is the **BGSSAI** app icon submitted to Alipay Open Platform — not Alipay's trademark. |

## OAuth channel buttons (user login)

Aligned with **bgssai-blog**: login OAuth tiles show each platform's officially published brand mark (PNG), not a redraw and not the BGSSAI mark for every channel.

| File | Platform | Status | Notes |
| --- | --- | --- | --- |
| `bgssai-short-user/.../src/assets/oauth/wechat.png` | WeChat | Official | Same pack as bgssai-blog `src/assets/oauth/` |
| `.../alipay.png` | Alipay | Official | Same pack |
| `.../douyin.png` | Douyin | Placeholder pending official pack | Same pack as blog |
| `.../baidu.png` | Baidu | Official site icon packaging | Same pack |
| BGSSAI channel | BGSSAI | In-repo SVG mark | `BrandBgssai` in `brandIcons.jsx` |

Provenance detail: `bgssai-short-user/bgssai-short-user-react/src/assets/oauth/SOURCES.md`.

Channel **labels** remain plain text (微信 / 支付宝 / 抖音 / 百度 / BGSSAI 账号). Do not hand-draw trademark SVG paths.

## Liquid Glass

Brand marks sit on Liquid Glass surfaces. Marks themselves stay opaque raster / SVG assets; chrome around them uses translucent fills + `backdrop-filter`.

## Rules for future changes

1. Prefer officially published platform bitmaps (or lossless packaging). Do not invent trademark SVG path recreations.
2. When replacing BGSSAI marks, update every sync copy listed above in one commit.
3. Record OAuth asset provenance in `src/assets/oauth/SOURCES.md`.
