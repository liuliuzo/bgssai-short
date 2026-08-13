import { useCallback, useEffect, useId, useState } from 'react'
import { loadWxLoginScript, mountWechatFastLogin, parseWechatQrConnect } from '../lib/wechatFastLogin'
import './wechatFastLogin.css'

export function WechatFastLoginDialog({ open, authorizeUrl, onClose }) {
  const rawId = useId().replace(/:/g, '')
  const containerId = `wx-login-${rawId}`

  useEffect(() => {
    if (!open || !authorizeUrl) return undefined
    const params = parseWechatQrConnect(authorizeUrl)
    if (!params) {
      window.location.assign(authorizeUrl)
      return undefined
    }
    let destroy
    let cancelled = false
    loadWxLoginScript()
      .then(() => {
        if (cancelled) return
        destroy = mountWechatFastLogin(containerId, params)
      })
      .catch(() => {
        if (!cancelled) window.location.assign(authorizeUrl)
      })
    return () => {
      cancelled = true
      if (destroy) destroy()
    }
  }, [open, authorizeUrl, containerId])

  if (!open || !authorizeUrl) return null

  return (
    <div className="wx-fast-login-mask" role="dialog" aria-modal="true">
      <div className="wx-fast-login-panel">
        <div className="wx-fast-login-head">
          <h2 className="wx-fast-login-title">微信登录</h2>
          <button type="button" className="wx-fast-login-close" onClick={onClose} aria-label="关闭">
            ×
          </button>
        </div>
        <p className="wx-fast-login-hint">
          若本机已打开并登录微信 PC 版，将自动识别，请在微信客户端确认；也可继续用手机扫码。首次授权会自动创建本站账号，无需再注册。
        </p>
        <div className="wx-fast-login-box" id={containerId} />
        <div className="wx-fast-login-actions">
          <button type="button" className="wx-fast-login-link" onClick={() => window.location.assign(authorizeUrl)}>
            打开微信登录页
          </button>
          <button type="button" className="wx-fast-login-link" onClick={onClose}>
            取消
          </button>
        </div>
      </div>
    </div>
  )
}

export function useWechatFastLogin() {
  const [open, setOpen] = useState(false)
  const [authorizeUrl, setAuthorizeUrl] = useState(null)

  const startWechatFastLogin = useCallback((url) => {
    setAuthorizeUrl(url)
    setOpen(true)
  }, [])

  const wechatFastLoginDialog = (
    <WechatFastLoginDialog open={open} authorizeUrl={authorizeUrl} onClose={() => setOpen(false)} />
  )

  return { startWechatFastLogin, wechatFastLoginDialog }
}
