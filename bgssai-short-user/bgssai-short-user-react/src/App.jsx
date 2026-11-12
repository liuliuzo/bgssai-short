import { useLayoutEffect } from 'react'
import LegalFooter from './components/LegalFooter'
import { Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom'
import { englishPrefixTarget, isEnglishPrefix } from './lib/chineseEntry'
import { safeReturnPath } from './lib/safeReturn'
import { useUserStore, isAllowedRole } from './store/user'
import MainLayout from './layouts/MainLayout'
import Login from './pages/login/Login'
import BotDownloadPage from './pages/download/BotDownloadPage'
import OAuthCallback from './pages/login/OAuthCallback'
import Home from './pages/home/Home'
import LoginLogsPage from './pages/loginLogs/LoginLogsPage'
import McpTokens from './pages/mcp/McpTokens'
import SubscriptionPage from './pages/subscription/SubscriptionPage'
import AccountSettingsPage from './pages/account/AccountSettingsPage'
import UsageBillingPage from './pages/usage/UsageBillingPage'
import WorksPage from './pages/works/WorksPage'
import WorkStudio from './pages/works/WorkStudio'
import TasksPage from './pages/tasks/TasksPage'

function RequireAuth({ children }) {
  const userInfo = useUserStore((s) => s.userInfo)
  const location = useLocation()
  if (!userInfo || !isAllowedRole(userInfo)) {
    const back = `${location.pathname}${location.search}`
    return <Navigate to={`/login?redirect=${encodeURIComponent(back)}`} replace />
  }
  return children
}

function RequireGuest({ children }) {
  const userInfo = useUserStore((s) => s.userInfo)
  const location = useLocation()
  if (userInfo && isAllowedRole(userInfo)) {
    const back = safeReturnPath(new URLSearchParams(location.search).get('redirect'))
    return <Navigate to={back || '/home'} replace />
  }
  return children
}

/** Logged-in users keep the app shell; guests still reach the public download page. */
function SoftAuthShell() {
  const userInfo = useUserStore((s) => s.userInfo)
  if (userInfo && isAllowedRole(userInfo)) {
    return <MainLayout />
  }
  return <Outlet />
}

function EnglishPrefixRedirect() {
  const location = useLocation()
  const target = englishPrefixTarget(location.pathname, location.search, location.hash) || '/login'
  useLayoutEffect(() => {
    if (isEnglishPrefix(window.location.pathname)) {
      window.location.replace(target)
    }
  }, [target])
  return <Navigate to={target} replace />
}

function NotFoundPage() {
  const location = useLocation()
  const alias = englishPrefixTarget(location.pathname, location.search, location.hash)
  if (alias) return <EnglishPrefixRedirect />
  const userInfo = useUserStore((s) => s.userInfo)
  const authed = userInfo && isAllowedRole(userInfo)
  return (
    <div style={{ maxWidth: 520, margin: '80px auto', padding: 24, textAlign: 'center' }}>
      <h2 style={{ marginTop: 0 }}>页面不存在</h2>
      <p style={{ color: '#666' }}>你访问的地址无效或已被移除。</p>
      <a href={authed ? '/home' : '/login'}>{authed ? '返回首页' : '去登录'}</a>
    </div>
  )
}

export default function App() {
  return (
    <div className="app-shell">
    <div className="app-routes">
    <Routes>
      <Route
        path="/login"
        element={
          <RequireGuest>
            <Login />
          </RequireGuest>
        }
      />
      <Route path="/oauth-callback" element={<OAuthCallback />} />
      <Route path="/en" element={<EnglishPrefixRedirect />} />
      <Route path="/en/*" element={<EnglishPrefixRedirect />} />

      <Route element={<SoftAuthShell />}>
        <Route path="/download/bot" element={<BotDownloadPage />} />
      </Route>

      <Route
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route path="/home" element={<Home />} />
        <Route path="/login-logs" element={<LoginLogsPage />} />
        <Route path="/mcp-tokens" element={<McpTokens />} />
        <Route path="/subscription" element={<SubscriptionPage />} />
        <Route path="/account" element={<AccountSettingsPage />} />
        <Route path="/usage" element={<UsageBillingPage />} />
        <Route path="/works" element={<WorksPage />} />
        <Route path="/works/:workId" element={<WorkStudio />} />
        <Route path="/works/:workId/:tab" element={<WorkStudio />} />
        <Route path="/tasks" element={<TasksPage />} />
        <Route path="/" element={<Navigate to="/home" replace />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
    </div>
    <LegalFooter />
    </div>
  )
}
