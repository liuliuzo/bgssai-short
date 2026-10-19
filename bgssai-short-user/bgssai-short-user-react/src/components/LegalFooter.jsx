import { COPYRIGHT_LINE, LEGAL_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ variant = 'app', checked, onChange }) {
  const consentLink = (page) => (
    <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>
      {`《${page.label_zh}》`}
    </a>
  )
  if (variant === 'consent') {
    return (
      <label className="legal-consent">
        <input type="checkbox" checked={!!checked} onChange={(e) => onChange(e.target.checked)} />
        <span>我已阅读并同意{consentLink(LEGAL_PAGES[0])}与{consentLink(LEGAL_PAGES[1])}。</span>
      </label>
    )
  }
  return (
    <footer className="legal-footer">
      <nav aria-label="法律信息 Legal">
        {LEGAL_PAGES.map((page, index) => (
          <span key={page.key}>
            {index > 0 ? ' · ' : null}
            <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">{page.footer_zh}</a>
            {' / '}
            <a href={legalPageUrl(page, 'en')} target="_blank" rel="noopener noreferrer">{page.footer_en}</a>
          </span>
        ))}
      </nav>
      <div>{COPYRIGHT_LINE}</div>
    </footer>
  )
}
