import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { message } from 'antd'
import authApi from '../../api/auth'
import { OAUTH_PROVIDERS } from '../../components/brandIcons'
import { useWechatFastLogin } from '../../components/WechatFastLoginDialog'
import { parseWechatQrConnect } from '../../lib/wechatFastLogin'
import { useUserStore, isAllowedRole } from '../../store/user'
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

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [loginMethod, setLoginMethod] = useState('password')
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [smsCode, setSmsCode] = useState('')
  const [email, setEmail] = useState('')
  const [emailCode, setEmailCode] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [busy, setBusy] = useState(false)
  const [sending, setSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const { startWechatFastLogin, wechatFastLoginDialog } = useWechatFastLogin()

  useEffect(() => {
    if (countdown <= 0) return undefined
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000)
    return () => clearTimeout(timer)
  }, [countdown])

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
        const phoneTrim = phone.trim()
        if (!/^1[3-9]\d{9}$/.test(phoneTrim)) {
          message.warning('请输入正确的手机号')
          return
        }
        const { data } = await authApi.sendSms({ phone: phoneTrim })
        if (CODE_OK.has(data && data.code)) {
          setCountdown(60)
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
          setCountdown(60)
          message.success('验证码已发送')
        }
      }
    } finally {
      setSending(false)
    }
  }

  const submit = async () => {
    setBusy(true)
    try {
      if (loginMethod === 'sms') {
        if (!/^1[3-9]\d{9}$/.test(phone.trim()) || !/^\d{6}$/.test(smsCode.trim())) {
          message.warning('请输入手机号和 6 位验证码')
          return
        }
        const { data } = await authApi.loginSms({ phone: phone.trim(), code: smsCode.trim() })
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

  const startOAuth = async (platform) => {
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
    <div className="login-shell">
      <aside className="login-hero">
        <div className="login-hero-top">
          <img className="login-hero-logo" src="/brand/bgss-mark-tile.png" alt="" />
          <span className="login-hero-mark">BGSSAI 短剧</span>
          <span className="login-hero-mark-sub">Short</span>
        </div>
        <div>
          <h1 className="login-hero-title">一句话开始做短剧</h1>
          <p className="login-hero-desc">
            从作品到剧本、分镜与成片，把短剧制作收成一条可管理的流水线。
          </p>
          <ul className="login-hero-features">
            <li className="login-hero-feat">
              <span className="login-hero-dot" aria-hidden="true" />
              <div>
                <strong>三问体验</strong>
                <span>先看会是什么样、连不连贯、管起来方不方便，再决定往下生成。</span>
              </div>
            </li>
            <li className="login-hero-feat">
              <span className="login-hero-dot" aria-hidden="true" />
              <div>
                <strong>剧本与分镜</strong>
                <span>从一句话长出剧本、分镜与文字预演，镜头语言不必从零学。</span>
              </div>
            </li>
            <li className="login-hero-feat">
              <span className="login-hero-dot" aria-hidden="true" />
              <div>
                <strong>成片生成</strong>
                <span>画面、配音、字幕与合成按管线推进，额度花在看得见的镜头上。</span>
              </div>
            </li>
          </ul>
        </div>
        <div className="login-hero-foot">先看懂画面，再花生成额度。</div>
      </aside>

      <main className="login-main">
        <div className="login-panel">
          <div className="login-panel-brand">
            <img className="login-panel-logo" src="/brand/bgss-mark-tile.png" alt="" />
            <span className="s">短剧 · Short</span>
          </div>

          <h2 className="login-head">欢迎回来</h2>
          <p className="login-sub">登录以继续你的短剧制作</p>

          <div className="login-seg" role="tablist" aria-label="登录方式">
            <button type="button" className={`login-seg-btn${loginMethod === 'password' ? ' active' : ''}`} onClick={() => setLoginMethod('password')}>密码登录</button>
            <button type="button" className={`login-seg-btn${loginMethod === 'sms' ? ' active' : ''}`} onClick={() => setLoginMethod('sms')}>手机验证码</button>
            <button type="button" className={`login-seg-btn${loginMethod === 'email' ? ' active' : ''}`} onClick={() => setLoginMethod('email')}>邮箱验证码</button>
          </div>

          {loginMethod === 'sms' && (
            <>
              <div className="field">
                <label htmlFor="login-phone">手机号</label>
                <input id="login-phone" className="input" value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))} placeholder="11 位手机号" maxLength={11} />
              </div>
              <div className="field">
                <label htmlFor="login-sms-code">短信验证码</label>
                <div className="login-code-row">
                  <input id="login-sms-code" className="input" value={smsCode} onChange={(e) => setSmsCode(e.target.value.replace(/\D/g, ''))} placeholder="6 位验证码" maxLength={6} />
                  <button className="login-code-btn" type="button" onClick={sendCode} disabled={sending || countdown > 0}>
                    {countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '获取验证码'}
                  </button>
                </div>
                <p className="login-note">未注册的手机号将自动创建账号</p>
              </div>
            </>
          )}

          {loginMethod === 'email' && (
            <>
              <div className="field">
                <label htmlFor="login-email">邮箱</label>
                <input id="login-email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" />
              </div>
              <div className="field">
                <label htmlFor="login-email-code">邮箱验证码</label>
                <div className="login-code-row">
                  <input id="login-email-code" className="input" value={emailCode} onChange={(e) => setEmailCode(e.target.value.replace(/\D/g, ''))} placeholder="6 位验证码" maxLength={6} />
                  <button className="login-code-btn" type="button" onClick={sendCode} disabled={sending || countdown > 0}>
                    {countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '获取验证码'}
                  </button>
                </div>
                <p className="login-note">未注册的邮箱将自动创建账号</p>
              </div>
            </>
          )}

          {loginMethod === 'password' && (
            <>
          <div className="field">
            <label htmlFor="login-identifier">账号</label>
            <input
              id="login-identifier"
              className="input"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="请输入账号"
              autoComplete="username"
              disabled={busy}
              onKeyDown={(e) => e.key === 'Enter' && submit()}
            />
          </div>

          <div className="field">
            <label htmlFor="login-password">密码</label>
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
            {busy ? '处理中...' : '登录'}
          </button>

          <div className="login-oauth-divider">或使用第三方账号</div>
          <p className="login-note" style={{ textAlign: 'center', marginTop: 0 }}>
            首次授权即自动创建账号，无需再注册
          </p>
          <div className="oauth-grid">
            {OAUTH_PROVIDERS.map((o) => (
              <button
                className="oauth-btn"
                type="button"
                key={o.key}
                onClick={() => startOAuth(o.key)}
                disabled={busy}
                title={`使用${o.label}登录`}
              >
                <span className="oauth-icon" style={{ boxShadow: `inset 0 0 0 1.5px ${o.color}2e` }}>
                  <o.Icon size={20} />
                </span>
                {o.label}
              </button>
            ))}
          </div>
        </div>
      </main>
      {wechatFastLoginDialog}
    </div>
  )
}
