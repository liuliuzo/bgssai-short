import { COPYRIGHT_LINE, LEGAL_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ variant = 'app' }) {
  const link = (page, consent = false) => <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">{consent ? `《${page.label_zh}》` : page.label_zh}</a>
  if (variant === 'consent') return <p className="legal-consent">登录或注册即表示已阅读并同意{link(LEGAL_PAGES[0], true)}与{link(LEGAL_PAGES[1], true)}</p>
  return <footer className="legal-footer">{COPYRIGHT_LINE}{LEGAL_PAGES.map(page => <span key={page.key}>{' · '}{link(page)}</span>)}</footer>
}
