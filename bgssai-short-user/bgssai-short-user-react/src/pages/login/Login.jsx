import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, message } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import authApi from '../../api/auth'
import { useWechatFastLogin } from '../../components/WechatFastLoginDialog'
import { parseWechatQrConnect } from '../../lib/wechatFastLogin'
import { useUserStore, isAllowedRole } from '../../store/user'
import './login.css'

// User login page (password mode).
// Unified response envelope: { code, message, success, result }; success is code '0' or '200'.
const CODE_OK_STR_0 = '0'
const CODE_OK_STR_200 = '200'
const CODE_OK_NUM = 200

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [loading, setLoading] = useState(false)
  const [oauthLoading, setOauthLoading] = useState(null)
  const [form] = Form.useForm()
  const { startWechatFastLogin, wechatFastLoginDialog } = useWechatFastLogin()

  const loginSucceed = (result) => {
    if (!isAllowedRole(result)) {
      message.error('无权限：账号角色不允许登录用户端')
      return
    }
    setUserInfo(result)
    navigate('/home', { replace: true })
  }

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const { data } = await authApi.login({
        login_type: 'password',
        identifier: values.identifier,
        password: values.password,
      })
      const code = data && data.code
      const r = (data && data.result) || {}
      if (code == CODE_OK_NUM || code === CODE_OK_STR_0 || code === CODE_OK_STR_200) {
        if (r.jwt_token) {
          loginSucceed(r)
        } else {
          message.error('登录失败，请重试')
        }
      }
      // Other business error codes are surfaced by the axios interceptor via message.warning.
    } finally {
      setLoading(false)
    }
  }

  const startOAuth = async (platform) => {
    if (oauthLoading) return
    setOauthLoading(platform)
    try {
      const { data } = await authApi.oauthAuthorizeUrl({ platform })
      const url = data?.result?.authorize_url
      const code = data && data.code
      const ok = code == CODE_OK_NUM || code === CODE_OK_STR_0 || code === CODE_OK_STR_200
      if (!ok || !url) return
      sessionStorage.setItem('oauth_pending', JSON.stringify({ platform }))
      if (platform === 'wechat' && parseWechatQrConnect(url)) {
        startWechatFastLogin(url)
        return
      }
      window.location.assign(url)
    } finally {
      setOauthLoading(null)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <img className="login-logo-mark" src="/brand/bgss-icon.svg" alt="BGSS" />
          <div className="login-logo-name">AI 短剧制作平台</div>
          <div className="login-logo-sub">User Console</div>
        </div>

        <Form form={form} onFinish={onFinish}>
          <Form.Item
            name="identifier"
            rules={[{ required: true, message: '请输入账号' }]}
            style={{ marginBottom: 14 }}
          >
            <Input size="large" placeholder="账号" prefix={<UserOutlined />} />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
            style={{ marginBottom: 20 }}
          >
            <Input.Password size="large" placeholder="密码" prefix={<LockOutlined />} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
              className="login-btn"
            >
              登 录
            </Button>
          </Form.Item>
        </Form>

        <div className="login-oauth-divider">或使用第三方账号</div>
        <p className="login-oauth-note">首次授权即自动创建账号，无需再注册</p>
        <div className="login-oauth-row">
          {[
            { key: 'wechat', label: '微信' },
            { key: 'alipay', label: '支付宝' },
            { key: 'douyin', label: '抖音' },
          ].map((p) => (
            <button
              key={p.key}
              type="button"
              className={`login-oauth-btn${p.key === 'wechat' ? ' login-oauth-btn-wechat' : ''}`}
              disabled={!!oauthLoading}
              onClick={() => startOAuth(p.key)}
            >
              {oauthLoading === p.key ? '跳转中...' : `${p.label}登录`}
            </button>
          ))}
        </div>

        <div className="login-copyright">© 2026 昆山兵贵神速智能科技有限公司</div>
      </div>
      {wechatFastLoginDialog}
    </div>
  )
}
