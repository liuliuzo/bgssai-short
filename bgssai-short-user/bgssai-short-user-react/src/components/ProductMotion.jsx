import './ProductMotion.css'

/** Decorative welcome artwork. Authentication controls remain outside the illustration. */
export default function ProductMotion() {
  return (
    <div className="product-motion" aria-hidden="true">
      <img src="/brand/illustrations/welcome.svg" alt="" width="600" height="400" decoding="async" />
    </div>
  )
}
