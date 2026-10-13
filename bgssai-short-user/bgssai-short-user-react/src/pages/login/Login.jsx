import BotDownloadLayout from './BotDownloadLayout'
import ProductMotion from '../../components/ProductMotion.jsx'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { message } from 'antd'
import authApi from '../../api/auth'
import { OAUTH_PROVIDERS } from '../../components/brandIcons'
import { useWechatFastLogin } from '../../components/WechatFastLoginDialog'
import { parseWechatQrConnect } from '../../lib/wechatFastLogin'
import { useUserStore, isAllowedRole } from '../../store/user'
import { useBgssaiShell } from '../../hooks/useBgssaiShell'
import LegalFooter from '../../components/LegalFooter'
import SupportWidget from '../../components/SupportWidget'
import { PhoneDialField } from '../../components/PhoneDialField'
import { DEFAULT_DIAL_CODE, composeApiPhone, validatePhoneParts } from '../../lib/phoneDial'
import './login.css'

const CODE_OK = new Set([0, 200, '0', '200'])

function IconEye({ size = 18 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

// 登录方式一级平铺（Standards §14.2）：密码 / 邮箱验证码 / 手机验证码，默认手机验证码。
// 字段名只给读屏（sr-only），可见文字只有方式名与占位符，避免同一个词在一屏上写三遍。
const LOGIN_METHODS = [
  { key: 'password', label: '密码' },
  { key: 'email', label: '邮箱验证码' },
  { key: 'sms', label: '手机验证码' },
]

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [loginMethod, setLoginMethod] = useState('sms')
  const [agreed, setAgreed] = useState(false)
  const requireAgreement = () => {
    if (!agreed) { message.warning('请先阅读并同意用户协议与隐私政策'); return false }
    return true
  }
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [dialCode, setDialCode] = useState(DEFAULT_DIAL_CODE)
  const [smsCode, setSmsCode] = useState('')
  const [email, setEmail] = useState('')
  const [emailCode, setEmailCode] = useState('')
  const [showPw, setShowPw] = useState(false)
  useEffect(() => { if (new URLSearchParams(window.location.search).get('reason') === 'session_kicked') message.error('账号已在其他设备登录') }, [])
  const [busy, setBusy] = useState(false)
  const [sending, setSending] = useState(false)
  // 邮箱与手机各自一个重发倒计时（后端按渠道 + 目标分别限频），切换方式不互相影响。
  const [countdowns, setCountdowns] = useState({ sms: 0, email: 0 })
  const { startWechatFastLogin, wechatFastLoginDialog } = useWechatFastLogin()
  const inShell = useBgssaiShell()

  const ticking = countdowns.sms > 0 || countdowns.email > 0
  useEffect(() => {
    if (!ticking) return undefined
    const timer = setTimeout(() => {
      setCountdowns((prev) => ({ sms: Math.max(0, prev.sms - 1), email: Math.max(0, prev.email - 1) }))
    }, 1000)
    return () => clearTimeout(timer)
  }, [countdowns, ticking])
  const countdown = countdowns[loginMethod] || 0

  const loginSucceed = (result) => {
    if (!isAllowedRole(result)) {
      message.error('无权限：账号角色不允许登录用户端')
      return
    }
    setUserInfo(result)
    message.success('登录成功')
    navigate('/home', { replace: true })
  }

  const sendCode = async () => {
    if (countdown > 0 || sending) return
    setSending(true)
    try {
      if (loginMethod === 'sms') {
        const phoneError = validatePhoneParts(dialCode, phone)
        if (phoneError) {
          message.warning(phoneError)
          return
        }
        const { data } = await authApi.sendSms({ phone: composeApiPhone(dialCode, phone) })
        if (CODE_OK.has(data && data.code)) {
          setCountdowns((prev) => ({ ...prev, sms: 60 }))
          // 发码提示不带内部编号（product#seq 只留在短信正文与日志里，Standards §14.2）。
          message.success('验证码已发送')
        }
      } else if (loginMethod === 'email') {
        const emailTrim = email.trim()
        if (!emailTrim.includes('@')) {
          message.warning('请输入正确的邮箱')
          return
        }
        const { data } = await authApi.sendEmail({ email: emailTrim })
        if (CODE_OK.has(data && data.code)) {
          setCountdowns((prev) => ({ ...prev, email: 60 }))
          message.success('验证码已发送')
        }
      }
    } finally {
      setSending(false)
    }
  }

  const submit = async () => {
    if (!requireAgreement()) return
    setBusy(true)
    try {
      if (loginMethod === 'sms') {
        if (validatePhoneParts(dialCode, phone) || !/^\d{6}$/.test(smsCode.trim())) {
          message.warning('请输入手机号和 6 位验证码')
          return
        }
        const { data } = await authApi.loginSms({ phone: composeApiPhone(dialCode, phone), code: smsCode.trim() })
        const r = (data && data.result) || {}
        if (CODE_OK.has(data && data.code) && r.jwt_token) loginSucceed(r)
        return
      }
      if (loginMethod === 'email') {
        if (!email.trim().includes('@') || !/^\d{6}$/.test(emailCode.trim())) {
          message.warning('请输入邮箱和 6 位验证码')
          return
        }
        const { data } = await authApi.loginEmail({ email: email.trim(), email_code: emailCode.trim() })
        const r = (data && data.result) || {}
        if (CODE_OK.has(data && data.code) && r.jwt_token) loginSucceed(r)
        return
      }
      if (!identifier.trim()) {
        message.warning('请输入账号')
        return
      }
      if (!password) {
        message.warning('请输入密码')
        return
      }
      const { data } = await authApi.login({
        login_type: 'password',
        identifier: identifier.trim(),
        password,
      })
      const r = (data && data.result) || {}
      if (CODE_OK.has(data && data.code) && r.jwt_token) {
        loginSucceed(r)
      } else if (CODE_OK.has(data && data.code)) {
        message.error('登录失败，请重试')
      }
    } finally {
      setBusy(false)
    }
  }

  const [openChannels, setOpenChannels] = useState([])
  const visibleProviders = OAUTH_PROVIDERS.filter((o) => openChannels.includes(o.key))

  // 只画后端已配好凭证的渠道：先前入口无条件常驻，点下去才报「该登录方式暂不可用」。
  useEffect(() => {
    let alive = true
    authApi
      .oauthChannels()
      .then(({ data }) => {
        const list = data && data.result && data.result.channels
        if (alive) setOpenChannels(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (alive) setOpenChannels([])
      })
    return () => {
      alive = false
    }
  }, [])

  const startOAuth = async (platform) => {
    if (!requireAgreement()) return
    if (busy) return
    setBusy(true)
    try {
      const { data } = await authApi.oauthAuthorizeUrl({ platform })
      const url = data?.result?.authorize_url
      if (!CODE_OK.has(data && data.code) || !url) return
      sessionStorage.setItem('oauth_pending', JSON.stringify({ platform }))
      if (platform === 'wechat' && parseWechatQrConnect(url)) {
        startWechatFastLogin(url)
        return
      }
      window.location.assign(url)
    } finally {
      setBusy(false)
    }
  }

  return (
    <BotDownloadLayout>
    <div className={`login-shell${inShell ? ' login-shell--pane' : ''}`}>
      <main className="login-main">
        <div className="login-panel">
          <div className="login-panel-brand">
            <img className="login-panel-logo" src="/brand/bgss-mark-tile.png" alt="" />
            <span className="s">短剧 Short</span>
          </div>

          <ProductMotion />
          <h2 className="login-head">欢迎回来</h2>

          <div className="login-seg" role="tablist" aria-label="登录方式">
            {LOGIN_METHODS.map((m) => (
              <button
                key={m.key}
                type="button"
                role="tab"
                aria-selected={loginMethod === m.key}
                className={`login-seg-btn${loginMethod === m.key ? ' active' : ''}`}
                onClick={() => setLoginMethod(m.key)}
              >
                {m.label}
              </button>
            ))}
          </div>

          {loginMethod === 'sms' && (
            <>
              <div className="field">
                <label className="sr-only" htmlFor="login-phone">手机号</label>
                <PhoneDialField
                  id="login-phone"
                  dialCode={dialCode}
                  nationalNumber={phone}
                  onDialCodeChange={setDialCode}
                  onNationalNumberChange={setPhone}
                  wrapperClassName="phone-dial-field input"
                />
              </div>
              <div className="field">
                <label className="sr-only" htmlFor="login-sms-code">短信验证码</label>
                <div className="login-code-row">
                  <input id="login-sms-code" className="input" value={smsCode} onChange={(e) => setSmsCode(e.target.value.replace(/\D/g, ''))} placeholder="6 位验证码" maxLength={6} />
                  <button className="login-code-btn" type="button" onClick={sendCode} disabled={sending || countdown > 0}>
                    {countdown > 0 ? `${countdown} 秒后重发` : sending ? '发送中...' : '获取验证码'}
                  </button>
                </div>
              </div>
            </>
          )}

          {loginMethod === 'email' && (
            <>
              <div className="field">
                <label className="sr-only" htmlFor="login-email">邮箱</label>
                <input id="login-email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="请输入邮箱地址" />
              </div>
              <div className="field">
                <label className="sr-only" htmlFor="login-email-code">邮箱验证码</label>
                <div className="login-code-row">
                  <input id="login-email-code" className="input" value={emailCode} onChange={(e) => setEmailCode(e.target.value.replace(/\D/g, ''))} placeholder="6 位验证码" maxLength={6} />
                  <button className="login-code-btn" type="button" onClick={sendCode} disabled={sending || countdown > 0}>
                    {countdown > 0 ? `${countdown} 秒后重发` : sending ? '发送中...' : '获取验证码'}
                  </button>
                </div>
              </div>
            </>
          )}

          {loginMethod === 'password' && (
            <>
          <div className="field">
            <label className="sr-only" htmlFor="login-identifier">账号</label>
            <input
              id="login-identifier"
              className="input"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="手机号 / 邮箱 / 账号"
              autoComplete="username"
              disabled={busy}
              onKeyDown={(e) => e.key === 'Enter' && submit()}
            />
          </div>

          <div className="field">
            <label className="sr-only" htmlFor="login-password">密码</label>
            <div style={{ position: 'relative' }}>
              <input
                id="login-password"
                className="input"
                type={showPw ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                style={{ paddingRight: 42 }}
                autoComplete="current-password"
                disabled={busy}
                onKeyDown={(e) => e.key === 'Enter' && submit()}
              />
              <button
                className="pw-toggle"
                type="button"
                onClick={() => setShowPw((v) => !v)}
                aria-label={showPw ? '隐藏密码' : '显示密码'}
                aria-pressed={showPw}
                title="显示 / 隐藏密码"
              >
                <IconEye size={18} />
              </button>
            </div>
          </div>
            </>
          )}

          <button className="btn btn-primary btn-block btn-lg" disabled={busy} onClick={submit}>
            {busy ? '处理中...' : loginMethod === 'password' ? '登录' : '登录 / 注册'}
          </button>

          {visibleProviders.length > 0 ? (
          <>
          <div className="login-oauth-divider">第三方账号登录</div>
          <div className="oauth-grid">
            {visibleProviders.map((o) => (
              <button
                className="oauth-btn"
                type="button"
                key={o.key}
                onClick={() => startOAuth(o.key)}
                disabled={busy}
              >
                <span className="oauth-icon" style={{ boxShadow: `inset 0 0 0 1.5px ${o.color}2e` }}>
                  <o.Icon size={20} />
                </span>
                {o.label}
              </button>
            ))}
          </div>
          </>
          ) : null}
          <LegalFooter variant="consent" checked={agreed} onChange={setAgreed} />
        </div>
      </main>
      {wechatFastLoginDialog}
      <SupportWidget />
    </div>
    </BotDownloadLayout>
  )
}
