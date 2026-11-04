import { useLayoutEffect } from 'react'
import LegalFooter from './components/LegalFooter'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { englishPrefixTarget, isEnglishPrefix } from './lib/chineseEntry'
import { safeReturnPath } from './lib/safeReturn'
import { useUserStore, isAllowedRole } from './store/user'
import MainLayout from './layouts/MainLayout'
import Login from './pages/login/Login'
import Home from './pages/home/Home'
import LoginLogsPage from './pages/loginLogs/LoginLogsPage'
import ReviewsPage from './pages/reviews/ReviewsPage'
import CreatorsPage from './pages/creators/CreatorsPage'
import CreditPacksPage from './pages/creditPacks/CreditPacksPage'
import SubscriptionPlansPage from './pages/subscriptionPlans/SubscriptionPlansPage'
import PayMerchantsPage from './pages/payMerchants/PayMerchantsPage'
import QualityTemplatesPage from './pages/qualityTemplates/QualityTemplatesPage'
import AiModelsPage from './pages/aiModels/AiModelsPage'
import AiSecretsPage from './pages/secrets/AiSecretsPage'
import UsageCostPage from './pages/usageCost/UsageCostPage'
import SupportInboxPage from './pages/support/SupportInboxPage'

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
    <>
    <Routes>
      <Route
        path="/login"
        element={
          <RequireGuest>
            <Login />
          </RequireGuest>
        }
      />
      <Route path="/en" element={<EnglishPrefixRedirect />} />
      <Route path="/en/*" element={<EnglishPrefixRedirect />} />

      <Route
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route path="/home" element={<Home />} />
        <Route path="/support" element={<SupportInboxPage />} />
        <Route path="/login-logs" element={<LoginLogsPage />} />
        <Route path="/reviews" element={<ReviewsPage />} />
        <Route path="/creators" element={<CreatorsPage />} />
        <Route path="/credit-packs" element={<CreditPacksPage />} />
        <Route path="/subscription-plans" element={<SubscriptionPlansPage />} />
        <Route path="/pay-merchants" element={<PayMerchantsPage />} />
        <Route path="/ai-models" element={<AiModelsPage />} />
        <Route path="/ai-secrets" element={<AiSecretsPage />} />
        <Route path="/usage-cost" element={<UsageCostPage />} />
        <Route path="/quality-templates" element={<QualityTemplatesPage />} />
        <Route path="/" element={<Navigate to="/home" replace />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
    <LegalFooter />
    </>
  )
}
