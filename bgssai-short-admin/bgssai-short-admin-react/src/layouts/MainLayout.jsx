import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { Dropdown, message } from 'antd'
import { DashboardOutlined, ProfileOutlined, AuditOutlined, TeamOutlined, GiftOutlined, PayCircleOutlined, RobotOutlined, KeyOutlined, FundOutlined, ExperimentOutlined, LogoutOutlined, DownOutlined } from '@ant-design/icons'
import { useUserStore } from '../store/user'
import './main-layout.css'

// Navigation items — two sample sections ship with the scaffold; add product routes here.
const NAV_ITEMS = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/reviews', label: '内容审核', icon: <AuditOutlined /> },
  { key: '/ai-models', label: 'AI 模型', icon: <RobotOutlined /> },
  { key: '/ai-secrets', label: 'AI 密钥', icon: <KeyOutlined /> },
  { key: '/usage-cost', label: '用量与成本', icon: <FundOutlined /> },
  { key: '/creators', label: '创作者', icon: <TeamOutlined /> },
  { key: '/credit-packs', label: '加量包', icon: <GiftOutlined /> },
  { key: '/subscription-plans', label: '订阅套餐', icon: <PayCircleOutlined /> },
  { key: '/pay-merchants', label: '支付商户', icon: <PayCircleOutlined /> },
  { key: '/quality-templates', label: '质量模板', icon: <ExperimentOutlined /> },
  { key: '/login-logs', label: '登录日志', icon: <ProfileOutlined /> },
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
    : (userInfo?.role_name || 'AD').slice(0, 2)

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
          <div className="pa-sidebar-brand-sub">管理控制台</div>
        </div>

        <nav className="pa-nav">
          <div className="pa-nav-section">
            <div className="pa-nav-section-label">管理功能</div>
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
            <span className="pa-topbar-title">AI 短剧制作平台 管理控制台</span>
            <span className="pa-topbar-badge">ADMIN</span>
          </div>
          <div className="pa-topbar-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <button className="pa-user-btn" type="button">
                <div className="pa-user-avatar">{initials}</div>
                <span className="pa-user-name">{userInfo?.real_name || userInfo?.role_name || '管理员'}</span>
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
