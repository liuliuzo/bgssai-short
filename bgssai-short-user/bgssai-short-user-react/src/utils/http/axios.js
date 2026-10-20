import axios from 'axios'
import { message } from 'antd'
import { useUserStore } from '../../store/user'
import { loginHref } from '../../lib/safeReturn'

// Same-origin /bgssai in production (SPA served by the admin backend).
// Local dev: Vite proxy forwards /bgssai to localhost:8080 (see vite.config.js).
const baseURL = import.meta.env.VITE_API_BASE || '/bgssai'

const instance = axios.create({
  baseURL,
  timeout: 50000,
})

// Request interceptor: inject the JWT token (header Jwttoken).
instance.interceptors.request.use(
  (config) => {
    const userInfo = useUserStore.getState().userInfo
    if (userInfo && userInfo.jwt_token) {
      config.headers['Jwttoken'] = userInfo.jwt_token
    }
    return config
  },
  (error) => Promise.reject(error),
)

const gotoLogin = () => {
  useUserStore.getState().clearUserInfo()
  if (!window.location.pathname.startsWith('/login') && window.location.pathname !== '/oauth-callback') {
    window.location.href = loginHref()
  }
}

// Response interceptor: handle unified Response envelope { code, message, success, result }
// and HTTP-level errors.
instance.interceptors.response.use(
  (res) => {
    if (res.data && res.data.code == 2003) {
      message.error('账号已在其他设备登录')
      useUserStore.getState().clearUserInfo()
      window.location.href = loginHref({ reason: 'session_kicked' })
      return Promise.reject(new Error('session_kicked'))
    }
    if (res.data && res.data.code == 2002) {
      gotoLogin()
      return Promise.reject(new Error('token_expired'))
    }
    if (res.data && res.data.code == 4003) {
      message.error(res.data.message || '无权限访问')
      gotoLogin()
      return Promise.reject(new Error('forbidden'))
    }
    if (
      res.data &&
      res.data.code != null &&
      res.data.code != 200 &&
      res.data.code != '0' &&
      res.data.code != '200'
    ) {
      message.warning(res.data.message)
    }
    return res
  },
  (error) => {
    if (String(error?.response?.data?.code) === '2003') {
      gotoLogin()
      window.location.href = loginHref({ reason: 'session_kicked' })
      return Promise.reject(new Error('账号已在其他设备登录'))
    }
    if (error && error.response) {
      const status = error.response.status
      switch (status) {
        case 400:
          message.error('请求错误')
          break
        case 401:
          message.error('未授权，请重新登录')
          gotoLogin()
          break
        case 403:
          message.error('无权限访问')
          break
        case 404:
          message.error('请求错误，未找到相应资源')
          break
        case 408:
          message.error('请求超时，请检查网络后重试')
          break
        case 500:
          message.error('服务暂时异常，请稍后重试')
          break
        case 502:
          message.error('网络异常，请稍后重试')
          break
        case 503:
          message.error('服务暂时不可用，请稍后重试')
          break
        case 504:
          message.error('网络超时，请稍后重试')
          break
        default:
          message.error('请求失败，请稍后重试')
      }
    } else {
      message.error('连接服务器失败，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export { instance }
