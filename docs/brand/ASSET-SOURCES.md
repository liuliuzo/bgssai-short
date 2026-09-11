# Brand asset sources

This product ships **only officially published BGSSAI platform marks** in UI chrome,
favicons, OAuth button glyphs, and open-platform app icons. Third-party trademark
logos (WeChat / Alipay / Douyin / Baidu / Google / GitHub brand marks) must not be
embedded as SVG paths, icon fonts, or raster copies.

## Official platform marks (in-repo)

| Asset | Canonical path | Also synced to | Source |
| --- | --- | --- | --- |
| BGSS mark | `docs/demo-static/web/assets/bgss-mark.png` | `bgssai-short-user/.../public/brand/`, `bgssai-short-admin/.../public/brand/`, backend `static/brand/` | Official BGSSAI product mark (2×2 BGSS block letterform; blue + gold). Standardized in-repo via `chore(brand): standardize product logo assets`. |
| BGSS mark tile | `docs/demo-static/web/assets/bgss-mark-tile.png` | same `public/brand/` trees | Official square/tile variant for login hero and panel brand slots. |
| Favicon PNG / ICO / apple-touch | React `public/favicon.png`, `favicon.ico`, `apple-touch-icon.png` | backend `static/` mirrors | Derived from the official BGSS mark (same letterform). |

## Open-platform app icon

| Asset | Path | Source |
| --- | --- | --- |
| Alipay open-platform app icon (320×320) | `payment/alipay/app_icon_320.png` | Regenerated from `docs/demo-static/web/assets/bgss-mark.png` (scale + pad to 320×320). This is the **BGSSAI** app icon submitted to Alipay Open Platform — not Alipay's trademark. |

Regenerate command:

```bash
ffmpeg -y -i docs/demo-static/web/assets/bgss-mark.png \
  -vf "scale=320:320:force_original_aspect_ratio=decrease,pad=320:320:(ow-iw)/2:(oh-ih)/2:black" \
  payment/alipay/app_icon_320.png
```

## OAuth channel buttons (user login)

| UI element | What is shown | What is NOT shown |
| --- | --- | --- |
| Channel button glyph | Official BGSSAI mark (`/brand/bgss-mark.png`) | WeChat / Alipay / Douyin / Baidu trademark logos |
| Channel button label | Plain text: 微信 / 支付宝 / 抖音 / 百度 / BGSSAI 账号 | Brand logotypes |

Implementation: `bgssai-short-user/bgssai-short-user-react/src/components/brandIcons.jsx`.
Previous simple-icons trademark path recreations were removed.

## Liquid Glass

Brand marks sit on Liquid Glass surfaces (`liquid-glass.css` + React `global.css`).
Marks themselves stay opaque raster assets; chrome around them uses translucent
fills + `backdrop-filter`.

## Rules for future changes

1. Do not reintroduce third-party trademark SVGs or CDNs for logos.
2. When replacing marks, update every sync copy listed above in one commit.
3. Record the source path and regeneration command in this file.
