import { COPYRIGHT_LINE, LEGAL_PAGES, legalFooterLabel, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter() {
  return (
    <footer className="legal-footer">
      <nav className="legal-footer-links" aria-label="法律信息">
        {LEGAL_PAGES.map((page) => (
          <span className="legal-footer-pair" key={page.key}>
            <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">{legalFooterLabel(page, 'zh')}</a>
            <span className="legal-footer-slash" aria-hidden="true"> / </span>
            <a href={legalPageUrl(page, 'en')} target="_blank" rel="noopener noreferrer">{legalFooterLabel(page, 'en')}</a>
          </span>
        ))}
      </nav>
      <p className="legal-footer-copy">{COPYRIGHT_LINE}</p>
    </footer>
  )
}
