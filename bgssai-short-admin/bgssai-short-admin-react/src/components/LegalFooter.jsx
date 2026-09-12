import { legalPageUrl, LEGAL_PAGES } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ tone = 'light', variant = 'app' }) {
  return (
    <footer className={`legal-footer legal-footer--${tone} legal-footer--${variant}`}>
      <nav className="legal-footer-links" aria-label="法律信息 Legal">
        {LEGAL_PAGES.map((page) => (
          <span key={page.slug} className="legal-footer-pair">
            <a
              href={legalPageUrl(page, 'zh')}
              target="_blank"
              rel="noopener noreferrer"
            >
              <span lang="zh-CN">{page.label_zh}</span>
            </a>
            <span aria-hidden="true"> / </span>
            <a
              href={legalPageUrl(page, 'en')}
              target="_blank"
              rel="noopener noreferrer"
            >
              <span lang="en">{page.label_en}</span>
            </a>
          </span>
        ))}
      </nav>
      <p className="legal-footer-copy">© 2026 昆山兵贵神速智能科技有限公司</p>
    </footer>
  )
}
