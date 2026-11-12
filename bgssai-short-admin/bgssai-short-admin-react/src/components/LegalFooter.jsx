import { COPYRIGHT_LINE, ICP_FILING_NUMBER, ICP_FILING_URL, LEGAL_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter() {
  return (
    <footer className="legal-footer">
      <nav className="legal-footer-links" aria-label="法律信息 Legal">
        {LEGAL_PAGES.map((page) => (
          <span className="legal-footer-pair" key={page.key}>
            <a href={legalPageUrl(page, 'zh')} target="_blank" rel="noopener noreferrer">{page.footer_zh}</a>
            <span className="legal-footer-slash" aria-hidden="true"> / </span>
            <a href={legalPageUrl(page, 'en')} target="_blank" rel="noopener noreferrer">{page.footer_en}</a>
          </span>
        ))}
      </nav>
      <p className="legal-footer-copy">{COPYRIGHT_LINE}</p>
      <p className="legal-footer-filing">
        <a href={ICP_FILING_URL} target="_blank" rel="noopener noreferrer">
          {ICP_FILING_NUMBER}
        </a>
      </p>
    </footer>
  )
}
