import LegalFooter from './components/LegalFooter'
import { Routes, Route, Navigate } from 'react-router-dom'
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
  if (!userInfo || !isAllowedRole(userInfo)) {
    return <Navigate to="/login" replace />
  }
  return children
}

function RequireGuest({ children }) {
  const userInfo = useUserStore((s) => s.userInfo)
  if (userInfo && isAllowedRole(userInfo)) {
    return <Navigate to="/home" replace />
  }
  return children
}

function NotFoundRedirect() {
  const userInfo = useUserStore((s) => s.userInfo)
  return <Navigate to={userInfo && isAllowedRole(userInfo) ? '/home' : '/login'} replace />
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

      <Route path="*" element={<NotFoundRedirect />} />
    </Routes>
    <LegalFooter />
    </>
  )
}
