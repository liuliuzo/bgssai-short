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

// 主题令牌：扁平蓝色设计体系（品牌色 #2563eb），管理端与用户端保持一致。
const theme = {
  token: {
    colorPrimary: '#2563eb',
    colorInfo: '#2563eb',
    colorText: '#0f172a',
    colorTextSecondary: '#64748b',
    colorTextTertiary: '#94a3b8',
    colorBorder: '#e2e8f0',
    colorBorderSecondary: '#eef0f4',
    colorBgLayout: '#f8fafc',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    borderRadius: 8,
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.08)',
    boxShadowSecondary: '0 8px 24px rgba(0, 0, 0, 0.10)',
    fontFamily:
      '-apple-system, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "PingFang SC", "Microsoft YaHei", Arial, sans-serif',
  },
  components: {
    Button: { borderRadius: 8, borderRadiusSM: 6, borderRadiusLG: 10, primaryShadow: 'none' },
    Input: { borderRadius: 8, borderRadiusSM: 6, borderRadiusLG: 10 },
    Card: { borderRadiusLG: 12 },
    Modal: { borderRadiusLG: 12 },
    Table: { borderRadiusLG: 12, headerBg: '#f8fafc' },
  },
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={theme}>
      <AntdApp>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
)
