import { useState } from 'react'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { Dropdown, Drawer, message } from 'antd'
import { DashboardOutlined, ProfileOutlined, ApiOutlined, AccountBookOutlined, WalletOutlined, VideoCameraOutlined, ThunderboltOutlined, LogoutOutlined, DownOutlined , DownloadOutlined, AppstoreOutlined } from '@ant-design/icons'
import { useUserStore } from '../store/user'
import { useBgssaiShell } from '../hooks/useBgssaiShell'
import LegalFooter from '../components/LegalFooter'
import './main-layout.css'

// Navigation items — two sample sections ship with the scaffold; add product routes here.
const NAV_ITEMS = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/works', label: '我的作品', icon: <VideoCameraOutlined /> },
  { key: '/tasks', label: '任务中心', icon: <ThunderboltOutlined /> },
  { key: '/usage', label: '用量与账单', icon: <WalletOutlined /> },
  { key: '/subscription', label: '套餐与加量', icon: <AccountBookOutlined /> },
  { key: '/mcp-tokens', label: 'MCP 连接器', icon: <ApiOutlined /> },
  { key: '/download/bot', label: '下载 BGSSAI BOT', icon: <DownloadOutlined /> },
  { key: '/login-logs', label: '我的登录日志', icon: <ProfileOutlined /> },
]

const SHELL_PRIMARY = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/works', label: '作品', icon: <VideoCameraOutlined /> },
  { key: '/tasks', label: '任务', icon: <ThunderboltOutlined /> },
  { key: '/subscription', label: '套餐', icon: <AccountBookOutlined /> },
]

const SHELL_MORE = NAV_ITEMS.filter((item) => !SHELL_PRIMARY.some((primary) => primary.key === item.key))

function isNavActive(pathname, key) {
  if (key === '/home') return pathname === '/home'
  return pathname === key || pathname.startsWith(`${key}/`)
}

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const userInfo = useUserStore((s) => s.userInfo)
  const clearUserInfo = useUserStore((s) => s.clearUserInfo)
  const inShell = useBgssaiShell()
  const [moreOpen, setMoreOpen] = useState(false)

  const handleLogout = () => {
    clearUserInfo()
    navigate('/login', { replace: true })
    message.success('已退出登录')
  }

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  const initials = userInfo?.real_name
    ? userInfo.real_name.slice(-2)
    : (userInfo?.role_name || 'US').slice(0, 2)

  const go = (key) => {
    setMoreOpen(false)
    navigate(key)
  }

  const moreActive = SHELL_MORE.some((item) => isNavActive(location.pathname, item.key))

  return (
    <div className={`pa-layout lg-bg${inShell ? ' pa-layout--shell' : ''}`}>
      {!inShell && (
        <aside className="pa-sidebar">
          <div className="pa-sidebar-brand">
            <div className="pa-sidebar-brand-main">
              <img
                className="pa-sidebar-brand-logo"
                src="/brand/bgss-mark.png"
                alt=""
              />
              <span className="pa-sidebar-brand-name">AI 短剧制作平台</span>
            </div>
            <div className="pa-sidebar-brand-sub">用户端</div>
          </div>

          <nav className="pa-nav">
            <div className="pa-nav-section">
              <div className="pa-nav-section-label">功能菜单</div>
              {NAV_ITEMS.map((item) => (
                <div
                  key={item.key}
                  className={`pa-nav-item${isNavActive(location.pathname, item.key) ? ' active' : ''}`}
                  onClick={() => navigate(item.key)}
                  role="menuitem"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && navigate(item.key)}
                >
                  <span className="pa-nav-item-icon">{item.icon}</span>
                  {item.label}
                </div>
              ))}
            </div>
          </nav>

          <div className="pa-sidebar-footer">
            <div
              className="pa-nav-item"
              onClick={handleLogout}
              role="menuitem"
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && handleLogout()}
            >
              <span className="pa-nav-item-icon"><LogoutOutlined /></span>
              退出登录
            </div>
          </div>
        </aside>
      )}

      <div className="pa-main">
        <header className="pa-topbar">
          <div className="pa-topbar-left">
            <span className="pa-topbar-title">AI 短剧制作平台</span>
            <span className="pa-topbar-badge">{inShell ? 'BOT' : 'USER'}</span>
          </div>
          <div className="pa-topbar-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <button className="pa-user-btn" type="button">
                <div className="pa-user-avatar">{initials}</div>
                {!inShell && (
                  <span className="pa-user-name">{userInfo?.real_name || userInfo?.role_name || '用户'}</span>
                )}
                <DownOutlined className="pa-user-caret" />
              </button>
            </Dropdown>
          </div>
        </header>

        <main className="pa-content">
          <Outlet />
        </main>

        <LegalFooter tone="light" variant="app" />

        {inShell && (
          <nav className="pa-shell-tabbar" aria-label="壳内主导航">
            {SHELL_PRIMARY.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`pa-shell-tab${isNavActive(location.pathname, item.key) ? ' active' : ''}`}
                onClick={() => go(item.key)}
              >
                <span className="pa-shell-tab-icon">{item.icon}</span>
                <span>{item.label}</span>
              </button>
            ))}
            <button
              type="button"
              className={`pa-shell-tab${moreActive || moreOpen ? ' active' : ''}`}
              onClick={() => setMoreOpen(true)}
            >
              <span className="pa-shell-tab-icon"><AppstoreOutlined /></span>
              <span>更多</span>
            </button>
          </nav>
        )}
      </div>

      <Drawer
        title="更多"
        placement="bottom"
        height="auto"
        open={inShell && moreOpen}
        onClose={() => setMoreOpen(false)}
        className="pa-shell-more"
      >
        <div className="pa-shell-more-list">
          {SHELL_MORE.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`pa-shell-more-item${isNavActive(location.pathname, item.key) ? ' active' : ''}`}
              onClick={() => go(item.key)}
            >
              <span className="pa-nav-item-icon">{item.icon}</span>
              {item.label}
            </button>
          ))}
          <button type="button" className="pa-shell-more-item" onClick={handleLogout}>
            <span className="pa-nav-item-icon"><LogoutOutlined /></span>
            退出登录
          </button>
        </div>
      </Drawer>
    </div>
  )
}
