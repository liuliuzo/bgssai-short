import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Spin, message } from 'antd'
import authApi from '../../api/auth'
import { useUserStore, isAllowedRole } from '../../store/user'

const CODE_OK = new Set([0, 200, '0', '200'])

export default function OAuthCallback() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const setUserInfo = useUserStore((s) => s.setUserInfo)
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const code = params.get('code')
    const state = params.get('state')
    let platform = 'wechat'
    try {
      const pending = JSON.parse(sessionStorage.getItem('oauth_pending') || '{}')
      if (pending.platform) platform = pending.platform
    } catch {
      /* ignore */
    }
    sessionStorage.removeItem('oauth_pending')

    if (!code || !state) {
      message.error('授权信息缺失，请重新登录')
      navigate('/login', { replace: true })
      return
    }

    authApi
      .oauthCallback({ platform, code, state })
      .then(({ data }) => {
        const result = data?.result || {}
        if (!CODE_OK.has(data?.code) || !result.jwt_token) {
          navigate('/login', { replace: true })
          return
        }
        if (!isAllowedRole(result)) {
          message.error('无权限：账号角色不允许登录用户端')
          navigate('/login', { replace: true })
          return
        }
        setUserInfo(result)
        message.success(result.is_new_user ? '已自动创建账号并登录' : '登录成功')
        navigate('/home', { replace: true })
      })
      .catch(() => {
        navigate('/login', { replace: true })
      })
  }, [navigate, params, setUserInfo])

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
      <Spin tip="正在完成授权登录" />
    </div>
  )
}
