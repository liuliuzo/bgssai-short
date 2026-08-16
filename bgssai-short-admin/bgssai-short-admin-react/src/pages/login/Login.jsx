import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, message } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import authApi from '../../api/auth'
import { useUserStore, isAllowedRole } from '../../store/user'
import './login.css'

// Admin login page (password mode).
// Unified response envelope: { code, message, success, result }; success is code '0' or '200'.
const CODE_OK_STR_0 = '0'
const CODE_OK_STR_200 = '200'
const CODE_OK_NUM = 200

export default function Login() {
  const navigate = useNavigate()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  const loginSucceed = (result) => {
    if (!isAllowedRole(result)) {
      message.error('无权限：当前账号不是管理员，请联系系统管理员')
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

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <img className="login-logo-mark" src="/brand/bgss-mark.png" alt="" />
          <div className="login-logo-name">AI 短剧制作平台 管理控制台</div>
          <div className="login-logo-sub">Admin Console</div>
          <div className="login-role-badge">仅限管理员登录</div>
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

        <div className="login-copyright">© 2026 昆山兵贵神速智能科技有限公司</div>
      </div>
    </div>
  )
}
