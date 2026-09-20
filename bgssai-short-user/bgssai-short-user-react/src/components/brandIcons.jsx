/**
 * OAuth / brand marks: officially published platform assets (PNG), not redraws.
 * Sources: src/assets/oauth/SOURCES.md (aligned with bgssai-blog).
 */
import wechatPng from '../assets/oauth/wechat.png'
import alipayPng from '../assets/oauth/alipay.png'
import douyinPng from '../assets/oauth/douyin.png'
import baiduPng from '../assets/oauth/baidu.png'

function brandImg(src, label) {
  return function BrandIcon({ size = 18, className, style, title = label }) {
    return (
      <img
        src={src}
        width={size}
        height={size}
        className={className}
        style={{ display: 'block', objectFit: 'contain', ...style }}
        alt=""
        title={title}
        aria-label={title}
        draggable={false}
      />
    )
  }
}

export const BrandWechat = brandImg(wechatPng, '微信')
export const BrandAlipay = brandImg(alipayPng, '支付宝')
export const BrandDouyin = brandImg(douyinPng, '抖音')
export const BrandBaidu = brandImg(baiduPng, '百度')

export function BrandBgssai({ size = 18, className, style, title = 'BGSSAI' }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      className={className}
      style={style}
      role="img"
      aria-label={title}
    >
      <rect width="24" height="24" rx="6" fill="#071d49" />
      <path
        fill="#fff"
        d="M7.2 6.4h5.1c2.4 0 3.9 1.2 3.9 3.1 0 1.2-.6 2.1-1.7 2.6 1.4.4 2.2 1.5 2.2 2.9 0 2.1-1.6 3.6-4.3 3.6H7.2V6.4zm2.5 4.7h2.3c1.1 0 1.7-.5 1.7-1.3s-.6-1.3-1.7-1.3H9.7v2.6zm0 5.4h2.6c1.2 0 1.9-.6 1.9-1.5s-.7-1.5-1.9-1.5H9.7v3z"
      />
    </svg>
  )
}

// Login / Settings OAuth channels (product contract: WECHAT / ALIPAY / DOUYIN / BAIDU).
// Order matches bgssai-blog login grid.
export const OAUTH_PROVIDERS = [
  { key: 'bgssai', label: 'BGSSAI 账号', color: '#071d49', Icon: BrandBgssai },
  { key: 'wechat', label: '微信', color: '#07c160', Icon: BrandWechat },
  { key: 'alipay', label: '支付宝', color: '#1677ff', Icon: BrandAlipay },
  { key: 'douyin', label: '抖音', color: '#161823', Icon: BrandDouyin },
  { key: 'baidu', label: '百度', color: '#2932e1', Icon: BrandBaidu },
]
