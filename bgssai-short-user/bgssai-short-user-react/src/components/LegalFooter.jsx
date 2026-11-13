import { Fragment } from 'react'
import { COPYRIGHT_LINE, ICP_FILING_NUMBER, ICP_FILING_URL, LEGAL_PAGES, LOGIN_CONSENT_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

function LanguageRow({ locale }) {
  const english = locale === 'en'
  return (
    <div className={english ? 'legal-footer-lang legal-footer-lang-en' : 'legal-footer-lang'}>
      {LEGAL_PAGES.map((page, index) => (
        <Fragment key={page.key}>
          {index > 0 ? <span className="legal-footer-sep" aria-hidden="true">·</span> : null}
          <a href={legalPageUrl(page, locale)} target="_blank" rel="noopener noreferrer">
            {english ? page.footer_en : page.footer_zh}
          </a>
        </Fragment>
      ))}
    </div>
  )
}

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
      <nav className="legal-footer-links" aria-label="法律信息">
        <LanguageRow locale="zh" />
        <LanguageRow locale="en" />
      </nav>
      <div className="legal-footer-meta">
        <p className="legal-footer-copy">{COPYRIGHT_LINE}</p>
        <p className="legal-footer-filing">
          <a href={ICP_FILING_URL} target="_blank" rel="noopener noreferrer">
            {ICP_FILING_NUMBER}
          </a>
        </p>
      </div>
    </footer>
  )
}
