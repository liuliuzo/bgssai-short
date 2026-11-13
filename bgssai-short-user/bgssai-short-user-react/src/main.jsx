import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import App from './App'
import './styles/global.css'
import './styles/rounded-chevron-caret.css'
import { syncBgssaiShell } from './lib/bgssaiShell'

syncBgssaiShell(window.location, window.sessionStorage, document)

dayjs.locale('zh-cn')

// 主题令牌（Ant Design）：主色取自标志蓝 #3060c8，点缀用标志金。浅灰底、白色实底卡片。
const theme = {
  token: {
    colorPrimary: '#3060c8',
    colorInfo: '#3060c8',
    colorLink: '#3060c8',
    colorText: '#0f172a',
    colorTextSecondary: '#5b6577',
    colorTextTertiary: '#8a93a6',
    colorBorder: '#dfe3ea',
    colorBorderSecondary: '#eceef3',
    colorBgLayout: '#f6f7f9',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    borderRadius: 10,
    borderRadiusSM: 8,
    borderRadiusLG: 14,
    controlHeight: 36,
    boxShadow: '0 8px 24px rgba(15, 23, 42, 0.08)',
    boxShadowSecondary: '0 12px 32px rgba(15, 23, 42, 0.12)',
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans SC", "Noto Sans CJK SC", "Source Han Sans SC", Roboto, "Helvetica Neue", Arial, sans-serif',
  },
  components: {
    Button: { fontWeight: 600, primaryShadow: '0 6px 16px -8px rgba(48, 96, 200, 0.7)' },
    Card: { headerFontSize: 15 },
    Modal: { borderRadiusLG: 16 },
    Table: { headerBg: '#f8f9fb', headerColor: '#5b6577', rowHoverBg: '#f5f8ff' },
    Tag: { borderRadiusSM: 6 },
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
