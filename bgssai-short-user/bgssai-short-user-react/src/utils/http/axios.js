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

// Concurrent requests can all observe the same dead session; only the first
// one clears the session and navigates, the rest just reject.
let sessionRedirectStarted = false

const handleSessionExpired = (reason) => {
  if (sessionRedirectStarted) return
  sessionRedirectStarted = true
  useUserStore.getState().clearUserInfo()
  if (!window.location.pathname.startsWith('/login') && window.location.pathname !== '/oauth-callback') {
    window.location.href = loginHref(reason ? { reason } : undefined)
  }
}

// Same failure reported by several in-flight requests renders as one toast.
const showError = (text) => {
  if (text) message.error({ content: text, key: text })
}

const HTTP_ERROR_TEXT = {
  400: '请求错误',
  401: '未授权，请重新登录',
  403: '无权限访问',
  404: '请求错误，未找到相应资源',
  408: '请求超时，请检查网络后重试',
  500: '服务暂时异常，请稍后重试',
  502: '网络异常，请稍后重试',
  503: '服务暂时不可用，请稍后重试',
  504: '网络超时，请稍后重试',
}

// Response interceptor: handle unified Response envelope { code, message, success, result }
// and HTTP-level errors.
instance.interceptors.response.use(
  (res) => {
    if (res.data && res.data.code == 2003) {
      // The login page shows the session-kicked notice from the reason param.
      handleSessionExpired('session_kicked')
      return Promise.reject(new Error('session_kicked'))
    }
    if (res.data && res.data.code == 2002) {
      handleSessionExpired()
      return Promise.reject(new Error('token_expired'))
    }
    if (res.data && res.data.code == 4003) {
      showError(res.data.message || '无权限访问')
      handleSessionExpired()
      return Promise.reject(new Error('forbidden'))
    }
    if (
      res.data &&
      res.data.code != null &&
      res.data.code != 200 &&
      res.data.code != '0' &&
      res.data.code != '200' &&
      res.data.message
    ) {
      message.warning({ content: res.data.message, key: res.data.message })
    }
    return res
  },
  (error) => {
    if (String(error?.response?.data?.code) === '2003') {
      handleSessionExpired('session_kicked')
      return Promise.reject(new Error('账号已在其他设备登录'))
    }
    if (error && error.response) {
      const status = error.response.status
      const serverMessage = typeof error.response.data?.message === 'string'
        && error.response.data.message.trim()
        ? error.response.data.message
        : null
      showError(serverMessage || HTTP_ERROR_TEXT[status] || '请求失败，请稍后重试')
      if (status === 401) handleSessionExpired()
    } else if (error?.code === 'ECONNABORTED') {
      showError('请求超时，请检查网络后重试')
    } else {
      showError('连接服务器失败，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export { instance }
