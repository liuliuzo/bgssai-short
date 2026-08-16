const WX_LOGIN_JS = 'https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js'

export function parseWechatQrConnect(authorizeUrl) {
  try {
    const url = new URL(authorizeUrl)
    if (!url.hostname.endsWith('weixin.qq.com') || !url.pathname.includes('qrconnect')) {
      return null
    }
    const appid = url.searchParams.get('appid')
    const redirectUri = url.searchParams.get('redirect_uri')
    const state = url.searchParams.get('state') || ''
    const scope = url.searchParams.get('scope') || 'snsapi_login'
    if (!appid || !redirectUri) return null
    return { appid, redirectUri, state, scope }
  } catch {
    return null
  }
}

let scriptPromise = null

export function loadWxLoginScript() {
  if (window.WxLogin) return Promise.resolve()
  if (scriptPromise) return scriptPromise
  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = WX_LOGIN_JS
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      scriptPromise = null
      reject(new Error('wxLogin.js'))
    }
    document.head.appendChild(script)
  })
  return scriptPromise
}

export function mountWechatFastLogin(containerId, params) {
  // 注意：官方 wxLogin.js 把 onCleanup 挂在「传入的 options 对象」上，而不是 new 出来的实例上，
  // 所以必须持有 options 引用才能解绑它注册的 message 监听器。
  const options = {
    self_redirect: false,
    id: containerId,
    appid: params.appid,
    scope: params.scope,
    redirect_uri: encodeURIComponent(params.redirectUri),
    state: params.state,
    stylelite: 1,
  }
  new window.WxLogin(options)
  return () => {
    if (options.onCleanup) options.onCleanup()
    const el = document.getElementById(containerId)
    if (el) el.innerHTML = ''
  }
}

export function isWechatNativePayUrl(value) {
  if (!value) return false
  return /^weixin:\/\/wxpay\//i.test(value) || /wx\.tenpay\.com/i.test(value)
}

export function launchWechatPcPay(codeUrl) {
  const iframe = document.createElement('iframe')
  iframe.src = codeUrl
  iframe.style.display = 'none'
  iframe.setAttribute('aria-hidden', 'true')
  document.body.appendChild(iframe)
  window.setTimeout(() => iframe.remove(), 4000)
}
