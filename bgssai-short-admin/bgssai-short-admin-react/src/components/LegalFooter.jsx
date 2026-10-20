import { COPYRIGHT_LINE, ICP_FILING_NUMBER, ICP_FILING_URL, LEGAL_PAGES, legalPageUrl } from '../lib/legalLinks'
import './legal-footer.css'

export default function LegalFooter() {
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
      <div className="legal-footer-filing">
        <a href={ICP_FILING_URL} target="_blank" rel="noopener noreferrer">
          {ICP_FILING_NUMBER}
        </a>
      </div>
    </footer>
  )
}
