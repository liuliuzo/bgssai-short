import {
  COPYRIGHT_LINE,
  LEGAL_PAGES,
  LOGIN_CONSENT_PAGES,
  legalPageUrl,
} from '../lib/legalLinks'
import './legal-footer.css'

function ZhLink({ page }) {
  return (
    <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">
      <span lang="zh-CN">{page.label_zh}</span>
    </a>
  )
}

function Pair({ page }) {
  return (
    <span className="legal-footer-pair">
      <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">
        <span lang="zh-CN">{page.label_zh}</span>
      </a>
      <span aria-hidden="true"> / </span>
      <a href={legalPageUrl(page, 'en')} target="_blank" rel="noopener noreferrer">
        <span lang="en">{page.label_en}</span>
      </a>
    </span>
  )
}

/**
 * variant:
 * - consent: login agreement line (用户协议 + 隐私政策) — matches blog LegalFanout consent
 * - login: copyright-only under OAuth — matches blog login © row (no ICP on short)
 * - app: five bilingual pairs with · separators + copyright — non-login footers
 */
export default function LegalFooter({ tone = 'light', variant = 'app' }) {
  if (variant === 'consent') {
    const [terms, privacy] = LOGIN_CONSENT_PAGES
    return (
      <p className="legal-consent">
        登录或注册即表示已阅读并同意 <ZhLink page={terms} /> 与 <ZhLink page={privacy} />。
      </p>
    )
  }

  if (variant === 'login') {
    return (
      <footer className={`legal-footer legal-footer--${tone} legal-footer--login`}>
        <p className="legal-footer-copy legal-footer-copy--login">{COPYRIGHT_LINE}</p>
      </footer>
    )
  }

  return (
    <footer className={`legal-footer legal-footer--${tone} legal-footer--${variant}`}>
      <nav className="legal-footer-links" aria-label="法律信息 Legal">
        {LEGAL_PAGES.map((page, index) => (
          <span key={page.slug} className="legal-footer-item">
            {index > 0 ? (
              <span className="legal-sep" aria-hidden="true">
                {' · '}
              </span>
            ) : null}
            <Pair page={page} />
          </span>
        ))}
      </nav>
      <p className="legal-footer-copy">{COPYRIGHT_LINE}</p>
    </footer>
  )
}
