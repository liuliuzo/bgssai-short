import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import authApi from '../../api/auth'
import { useUserStore, isAllowedRole } from '../../store/user'
import { safeReturnPath } from '../../lib/safeReturn'
import './login.css'

const CODE_OK = new Set([0, 200, '0', '200'])

function IconEye({ concealed = false, size = 18 }) {
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
      {concealed ? (
        <>
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
          <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
          <line x1="1" y1="1" x2="23" y2="23" />
        </>
      ) : (
        <>
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </>
      )}
    </svg>
  )
}

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const loginSucceed = (result) => {
    if (!isAllowedRole(result)) {
      setError('无权限：当前账号不是管理员，请联系系统管理员')
      return
    }
    setUserInfo(result)
    const back = safeReturnPath(new URLSearchParams(window.location.search).get('redirect'))
    navigate(back || '/home', { replace: true })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username.trim() || !password) return
    setLoading(true)
    setError(null)
    try {
      const { data } = await authApi.login({
        login_type: 'password',
        identifier: username.trim(),
        password,
      })
      const r = (data && data.result) || {}
      if (CODE_OK.has(data && data.code) && r.jwt_token) {
        loginSucceed(r)
      } else if (CODE_OK.has(data && data.code)) {
        setError('登录失败，请稍后重试')
      } else {
        setError((data && data.message) || '登录失败')
      }
    } catch (err) {
      setError((err && err.message) || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  const disabled = loading || !username.trim() || !password

  return (
    <div className="admin-login-page">
      <div className="admin-login-card">
        <img src="/brand/illustrations/admin-welcome.svg" alt="" aria-hidden="true" width="600" height="400" decoding="async" style={{ display: 'block', width: '100%', maxWidth: 220, height: 108, objectFit: 'contain', margin: '0 auto 16px', pointerEvents: 'none' }} />
        <div className="admin-login-head">
          <img className="admin-login-mark" src="/brand/bgss-mark-tile.png" alt="" width={52} height={52} />
          <h1 className="admin-login-title">BGSSAI 短剧 · 运营管理后台</h1>
          <p className="admin-login-sub">管理控制台仅限授权管理员账号登录</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="admin-login-field">
            <label htmlFor="admin-username">用户名 / 邮箱</label>
            <input
              id="admin-username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入用户名或邮箱"
              autoComplete="username"
              disabled={loading}
              required
            />
          </div>

          <div className="admin-login-field admin-login-field-password">
            <label htmlFor="admin-password">密码</label>
            <div className="admin-login-pw">
              <input
                id="admin-password"
                type={showPw ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                autoComplete="current-password"
                disabled={loading}
                required
              />
              <button
                className="admin-login-pw-toggle"
                type="button"
                onClick={() => setShowPw((v) => !v)}
                aria-label={showPw ? '隐藏密码' : '显示密码'}
                aria-pressed={showPw}
                aria-controls="admin-password"
              >
                <IconEye concealed={showPw} size={18} />
              </button>
            </div>
          </div>

          {error && <div className="admin-login-error">{error}</div>}

          <button type="submit" className="admin-login-submit" disabled={disabled}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  )
}
