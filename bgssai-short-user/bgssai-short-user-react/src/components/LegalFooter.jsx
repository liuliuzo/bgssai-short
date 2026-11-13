import { COPYRIGHT_LINE, ICP_FILING_NUMBER, ICP_FILING_URL, LEGAL_PAGES, LOGIN_CONSENT_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ variant = 'app', checked, onChange, describedBy, invalid }) {
  const consentLink = (page) => (
    <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>
      {`《${page.label_zh}》`}
    </a>
  )
  if (variant === 'consent') {
    return (
      <label className="legal-consent">
        <input
          id="login-consent"
          type="checkbox"
          checked={!!checked}
          aria-invalid={invalid ? true : undefined}
          aria-describedby={describedBy}
          onChange={(e) => onChange(e.target.checked)}
        />
        <span>我已阅读并同意{consentLink(LOGIN_CONSENT_PAGES[0])}与{consentLink(LOGIN_CONSENT_PAGES[1])}。</span>
      </label>
    )
  }
  return (
    <footer className="legal-footer">
      <p className="legal-footer-line">
        <span>{COPYRIGHT_LINE}</span>
        {LEGAL_PAGES.map((page) => (
          <span key={page.key}>
            <span className="legal-footer-dot" aria-hidden="true"> · </span>
            <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">{page.label_zh}</a>
          </span>
        ))}
        <span>
          <span className="legal-footer-dot" aria-hidden="true"> · </span>
          <a className="legal-footer-filing-link" href={ICP_FILING_URL} target="_blank" rel="noopener noreferrer">{ICP_FILING_NUMBER}</a>
        </span>
      </p>
    </footer>
  )
}
