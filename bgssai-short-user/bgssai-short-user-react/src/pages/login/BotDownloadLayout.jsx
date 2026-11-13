import { Link } from 'react-router-dom'
import './BotDownloadLayout.css'

function DownloadIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 3v12" />
      <path d="m7 10 5 5 5-5" />
      <path d="M5 21h14" />
    </svg>
  )
}

// 登录 / 下载页共用的顶栏：左边品牌标志 + 本产品名，右边下载入口。
// 以前左边写的是「Bgssai Bot」，进来的人会以为打开错了产品。
export default function BotDownloadLayout({ children }) {
  return (
    <div className="bot-download-layout">
      <nav className="bot-download-nav" aria-label="短剧 Short">
        <span className="bot-download-brand">
          <img src="/brand/bgss-mark.png" alt="" aria-hidden="true" />
          <strong>短剧 Short</strong>
        </span>
        <Link to="/download/bot">
          <DownloadIcon />
          下载 BGSSAI Bot
        </Link>
      </nav>
      <div className="bot-download-content">{children}</div>
    </div>
  )
}
