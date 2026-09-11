/**
 * OAuth / brand icons — officially published BGSSAI platform marks only.
 *
 * Do NOT embed third-party trademark SVGs (WeChat / Alipay / Douyin / Baidu logos).
 * Channel identity is the text label; the glyph is always the BGSSAI mark.
 *
 * Asset source of truth: docs/brand/ASSET-SOURCES.md
 * Mark files: /brand/bgss-mark.png (synced from docs/demo-static/web/assets/).
 */

const MARK_SRC = '/brand/bgss-mark.png'

function PlatformMarkIcon({ size = 18, className, style, title = 'BGSSAI' }) {
  return (
    <img
      src={MARK_SRC}
      width={size}
      height={size}
      alt=""
      title={title}
      className={className}
      style={{
        borderRadius: Math.max(3, Math.round(size * 0.18)),
        objectFit: 'contain',
        display: 'block',
        ...style,
      }}
      role="img"
      aria-label={title}
    />
  )
}

/** @deprecated name kept for import compatibility — renders BGSSAI mark, not WeChat trademark */
export function BrandWechat(props) {
  return <PlatformMarkIcon title="微信登录" {...props} />
}

/** @deprecated name kept for import compatibility — renders BGSSAI mark, not Alipay trademark */
export function BrandAlipay(props) {
  return <PlatformMarkIcon title="支付宝登录" {...props} />
}

/** @deprecated name kept for import compatibility — renders BGSSAI mark, not Baidu trademark */
export function BrandBaidu(props) {
  return <PlatformMarkIcon title="百度登录" {...props} />
}

/** @deprecated name kept for import compatibility — renders BGSSAI mark, not Douyin trademark */
export function BrandDouyin(props) {
  return <PlatformMarkIcon title="抖音登录" {...props} />
}

export function BrandBgssai(props) {
  return <PlatformMarkIcon title="BGSSAI 账号" {...props} />
}

export { PlatformMarkIcon }

// OAuth channels: label text names the provider; icon is always the platform mark.
export const OAUTH_PROVIDERS = [
  { key: 'wechat', label: '微信', color: '#07c160', Icon: BrandWechat },
  { key: 'alipay', label: '支付宝', color: '#1677ff', Icon: BrandAlipay },
  { key: 'douyin', label: '抖音', color: '#161823', Icon: BrandDouyin },
  { key: 'baidu', label: '百度', color: '#2932e1', Icon: BrandBaidu },
  { key: 'bgssai', label: 'BGSSAI 账号', color: '#071d49', Icon: BrandBgssai },
]
