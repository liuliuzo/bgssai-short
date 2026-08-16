import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { Dropdown, message } from 'antd'
import { DashboardOutlined, ProfileOutlined, ApiOutlined, AccountBookOutlined, LogoutOutlined, DownOutlined } from '@ant-design/icons'
import { useUserStore } from '../store/user'
import './main-layout.css'

// Navigation items — two sample sections ship with the scaffold; add product routes here.
const NAV_ITEMS = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/login-logs', label: '我的登录日志', icon: <ProfileOutlined /> },
  { key: '/mcp-tokens', label: 'MCP 接入', icon: <ApiOutlined /> },
  { key: '/subscription', label: '套餐与加量', icon: <AccountBookOutlined /> },
]

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const userInfo = useUserStore((s) => s.userInfo)
  const clearUserInfo = useUserStore((s) => s.clearUserInfo)

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

  return (
    <div className="pa-layout lg-bg">
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
                className={`pa-nav-item${location.pathname === item.key ? ' active' : ''}`}
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

      <div className="pa-main">
        <header className="pa-topbar">
          <div className="pa-topbar-left">
            <span className="pa-topbar-title">AI 短剧制作平台</span>
            <span className="pa-topbar-badge">USER</span>
          </div>
          <div className="pa-topbar-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <button className="pa-user-btn" type="button">
                <div className="pa-user-avatar">{initials}</div>
                <span className="pa-user-name">{userInfo?.real_name || userInfo?.role_name || '用户'}</span>
                <DownOutlined className="pa-user-caret" />
              </button>
            </Dropdown>
          </div>
        </header>

        <main className="pa-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
