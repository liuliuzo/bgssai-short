import { COPYRIGHT_LINE, LEGAL_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ variant = 'app', checked, onChange }) {
  const link = (page, consent = false) => (
    <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer" onClick={consent ? (e) => e.stopPropagation() : undefined}>
      {consent ? `《${page.label_zh}》` : page.label_zh}
    </a>
  )
  if (variant === 'consent') {
    return (
      <label className="legal-consent">
        <input type="checkbox" checked={!!checked} onChange={(e) => onChange(e.target.checked)} />
        <span>我已阅读并同意{link(LEGAL_PAGES[0], true)}与{link(LEGAL_PAGES[1], true)}。</span>
      </label>
    )
  }
  return <footer className="legal-footer">{COPYRIGHT_LINE}{LEGAL_PAGES.map(page => <span key={page.key}>{' · '}{link(page)}</span>)}</footer>
}
