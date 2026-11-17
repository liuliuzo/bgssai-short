/**
 * OAuth / brand marks: officially published platform assets (PNG), not redraws.
 * Sources: src/assets/oauth/SOURCES.md (aligned with bgssai-blog).
 */
import wechatPng from '/brand/oauth/wechat.svg'
import alipayPng from '/brand/oauth/alipay.svg'
import douyinPng from '/brand/oauth/douyin.svg'
import baiduPng from '/brand/oauth/baidu.svg'
import bgssaiSvg from '/brand/oauth/bgssai.svg'

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
export const BrandBgssai = brandImg(bgssaiSvg, 'BGSSAI')

// Login / Settings OAuth channels (product contract: WECHAT / ALIPAY / DOUYIN / BAIDU).
// Order matches bgssai-blog login grid.
export const OAUTH_PROVIDERS = [
  { key: 'bgssai', label: '用 Chat 登录', color: '#3366cc', Icon: BrandBgssai },
  { key: 'wechat', label: '微信', color: '#07c160', Icon: BrandWechat },
  { key: 'alipay', label: '支付宝', color: '#1677ff', Icon: BrandAlipay },
  { key: 'douyin', label: '抖音', color: '#161823', Icon: BrandDouyin },
  { key: 'baidu', label: '百度', color: '#2932e1', Icon: BrandBaidu },
]
