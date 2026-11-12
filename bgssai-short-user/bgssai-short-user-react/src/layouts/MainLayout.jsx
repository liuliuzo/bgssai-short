import { useState, useEffect } from 'react'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { Dropdown, Drawer, message } from 'antd'
import { DashboardOutlined, ProfileOutlined, ApiOutlined, AccountBookOutlined, WalletOutlined, VideoCameraOutlined, ThunderboltOutlined, LogoutOutlined, DownOutlined , DownloadOutlined, AppstoreOutlined, SettingOutlined } from '@ant-design/icons'
import { useUserStore } from '../store/user'
import { useBgssaiShell } from '../hooks/useBgssaiShell'
import SupportWidget from '../components/SupportWidget'
import './main-layout.css'

// Navigation items — two sample sections ship with the scaffold; add product routes here.
const NAV_ITEMS = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/works', label: '我的作品', icon: <VideoCameraOutlined /> },
  { key: '/tasks', label: '任务中心', icon: <ThunderboltOutlined /> },
  { key: '/usage', label: '用量与账单', icon: <WalletOutlined /> },
  { key: '/subscription', label: '套餐与加量', icon: <AccountBookOutlined /> },
  { key: '/account', label: '账号设置', icon: <SettingOutlined /> },
  { key: '/mcp-tokens', label: 'MCP 连接器', icon: <ApiOutlined /> },
  { key: '/download/bot', label: '下载 Bgssai Bot', icon: <DownloadOutlined /> },
  { key: '/login-logs', label: '我的登录日志', icon: <ProfileOutlined /> },
]

const SHELL_PRIMARY = [
  { key: '/home', label: '首页', icon: <DashboardOutlined /> },
  { key: '/works', label: '作品', icon: <VideoCameraOutlined /> },
  { key: '/tasks', label: '任务', icon: <ThunderboltOutlined /> },
  { key: '/subscription', label: '套餐', icon: <AccountBookOutlined /> },
]

const SHELL_MORE = NAV_ITEMS.filter((item) => !SHELL_PRIMARY.some((primary) => primary.key === item.key))
const MAKE_KEYS = ['/home', '/works', '/tasks']
const MAKE_NAV = NAV_ITEMS.filter((item) => MAKE_KEYS.includes(item.key))
const ACCOUNT_NAV = NAV_ITEMS.filter((item) => !MAKE_KEYS.includes(item.key))

// 手机浏览器（不在 Bot 旁栏里）也用旁栏那套底部标签栏 +「更多」：以前手机上 220px 侧栏常驻，
// 内容区只剩一窄条，标题被挤成一字一行。
const NARROW_QUERY = '(max-width: 767px)'

function useNarrowViewport() {
  const read = () => (typeof window !== 'undefined' && window.matchMedia ? window.matchMedia(NARROW_QUERY).matches : false)
  const [narrow, setNarrow] = useState(read)
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return undefined
    const mql = window.matchMedia(NARROW_QUERY)
    const onChange = () => setNarrow(mql.matches)
    onChange()
    if (mql.addEventListener) mql.addEventListener('change', onChange)
    else mql.addListener(onChange)
    return () => {
      if (mql.removeEventListener) mql.removeEventListener('change', onChange)
      else mql.removeListener(onChange)
    }
  }, [])
  return narrow
}

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
  const narrow = useNarrowViewport()
  const compact = inShell || narrow
  const [moreOpen, setMoreOpen] = useState(false)

  const handleLogout = () => {
    clearUserInfo()
    navigate('/login', { replace: true })
    message.success('已退出登录')
  }

  const userMenuItems = [
    {
      key: 'account',
      icon: <SettingOutlined />,
      label: '账号设置',
      onClick: () => navigate('/account'),
    },
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
  // 顶栏写当前页面名（以前固定写「AI 短剧制作平台」加一个「用户」标签，是给开发看的端名）。
  const currentNav = NAV_ITEMS.find((item) => isNavActive(location.pathname, item.key))
  const pageTitle = currentNav ? currentNav.label : 'AI 短剧制作平台'

  return (
    <div className={`pa-layout lg-bg${inShell ? ' pa-layout--shell' : ''}${compact ? ' pa-layout--compact' : ''}`}>
      {!compact && (
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

          </div>

          <nav className="pa-nav">
            {[{ label: '制作', items: MAKE_NAV }, { label: '账户', items: ACCOUNT_NAV }].map((section) => (
              <div className="pa-nav-section" key={section.label}>
                <div className="pa-nav-section-label">{section.label}</div>
                {section.items.map((item) => (
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
            ))}
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
            <span className="pa-topbar-title">{pageTitle}</span>
          </div>
          <div className="pa-topbar-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <button className="pa-user-btn" type="button">
                <div className="pa-user-avatar">{initials}</div>
                {!compact && (
                  <span className="pa-user-name">{userInfo?.real_name || userInfo?.role_name || '用户'}</span>
                )}
                <DownOutlined className="pa-user-caret" />
              </button>
            </Dropdown>
          </div>
        </header>

        <main className="pa-content">
          <div key={location.pathname} className="page-enter">
            <Outlet />
          </div>
        </main>

        {compact && (
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

      <SupportWidget />

      <Drawer
        title="更多"
        placement="bottom"
        height="auto"
        open={compact && moreOpen}
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
