import { Routes, Route, Navigate } from 'react-router-dom'
import { useUserStore, isAllowedRole } from './store/user'
import MainLayout from './layouts/MainLayout'
import Login from './pages/login/Login'
import Home from './pages/home/Home'
import LoginLogsPage from './pages/loginLogs/LoginLogsPage'

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
        <Route path="/login-logs" element={<LoginLogsPage />} />
        <Route path="/" element={<Navigate to="/home" replace />} />
      </Route>

      <Route path="*" element={<NotFoundRedirect />} />
    </Routes>
  )
}
