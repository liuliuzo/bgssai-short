import { ConfigProvider } from 'antd'
import './BotDownloadSurface.css'

// Keep the download page theme local so the application keeps its own design.
export default function BotDownloadSurface({ children }) {
  return (
    <ConfigProvider theme={{ inherit: false, token: {
      colorPrimary: '#1f4b3a',
      colorLink: '#1f4b3a',
      colorText: '#14231c',
      colorTextSecondary: '#5d675f',
      fontFamily: '"Source Serif 4", "Noto Serif SC", Georgia, serif',
      borderRadius: 4,
    } }}>
      <div className="bot-download-surface">
        <div className="bot-download-surface__content">{children}</div>
      </div>
    </ConfigProvider>
  )
}
