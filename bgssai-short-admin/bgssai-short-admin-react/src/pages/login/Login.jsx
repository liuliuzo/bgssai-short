import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import authApi from '../../api/auth'
import { useUserStore, isAllowedRole } from '../../store/user'
import { safeReturnPath } from '../../lib/safeReturn'
import './login.css'

const CODE_OK = new Set([0, 200, '0', '200'])

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
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
        <div className="admin-login-head">
          <img className="admin-login-mark" src="/brand/bgss-mark-tile.png" alt="" width={52} height={52} />
          <h1 className="admin-login-title">BGSSAI 短剧 · 运营管理后台</h1>
          <p className="admin-login-sub">仅限 ADMIN 角色登录</p>
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
            <input
              id="admin-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
              autoComplete="current-password"
              disabled={loading}
              required
            />
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
