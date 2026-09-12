import { legalPageUrl, LEGAL_PAGES } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter({ tone = 'light', variant = 'app' }) {
  return (
    <footer className={`legal-footer legal-footer--${tone} legal-footer--${variant}`}>
      <nav className="legal-footer-links" aria-label="法律信息 Legal">
        {LEGAL_PAGES.map((page) => (
          <a
            key={page.slug}
            href={legalPageUrl(page)}
            target="_blank"
            rel="noopener noreferrer"
          >
            <span lang="zh-CN">{page.label_zh}</span>
            <span aria-hidden="true"> / </span>
            <span lang="en">{page.label_en}</span>
          </a>
        ))}
      </nav>
      <p className="legal-footer-copy">© 2026 昆山兵贵神速智能科技有限公司</p>
    </footer>
  )
}
