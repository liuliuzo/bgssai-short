import dayjs from 'dayjs'

/** Format API timestamps (epoch millis, numeric string or ISO) for display. Empty → '-'. */
export function formatDateTime(value) {
  if (value == null || value === '') return '-'
  const d = dayjs(/^\d{12,13}$/.test(String(value)) ? Number(value) : value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

/** Deploy environment from hostname (dev./test. prefix). env_mode is only the payment gateway mode. */
export function deployEnvLabel(host = typeof window !== 'undefined' ? window.location.hostname : '') {
  if (/^(dev\.|localhost|127\.)/.test(host)) return 'DEV'
  if (/^test\./.test(host)) return 'TEST'
  return '生产'
}
