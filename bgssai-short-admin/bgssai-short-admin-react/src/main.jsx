import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import App from './App'
import './styles/global.css'

dayjs.locale('zh-cn')

// 主题令牌：Liquid Glass（半透明容器 + 品牌色 #2563eb），管理端与用户端保持一致。
const theme = {
  token: {
    colorPrimary: '#2563eb',
    colorInfo: '#2563eb',
    colorText: '#0f172a',
    colorTextSecondary: '#64748b',
    colorTextTertiary: '#94a3b8',
    colorBorder: 'rgba(255, 255, 255, 0.55)',
    colorBorderSecondary: 'rgba(255, 255, 255, 0.32)',
    colorBgLayout: 'transparent',
    colorBgContainer: 'rgba(255, 255, 255, 0.52)',
    colorBgElevated: 'rgba(255, 255, 255, 0.74)',
    borderRadius: 8,
    boxShadow: '0 10px 36px rgba(15, 23, 42, 0.10)',
    boxShadowSecondary: '0 18px 52px rgba(15, 23, 42, 0.14)',
    fontFamily:
      '-apple-system, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "PingFang SC", "Microsoft YaHei", Arial, sans-serif',
  },
  components: {
    Button: { borderRadius: 8, borderRadiusSM: 6, borderRadiusLG: 10, primaryShadow: 'none' },
    Input: { borderRadius: 8, borderRadiusSM: 6, borderRadiusLG: 10 },
    Card: { borderRadiusLG: 12 },
    Modal: { borderRadiusLG: 12 },
    Table: { borderRadiusLG: 12, headerBg: 'rgba(255, 255, 255, 0.34)' },
  },
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={theme} button={{ autoInsertSpace: false }}>
      <AntdApp message={{ top: 88 }}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
)
