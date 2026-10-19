import dayjs from 'dayjs'

function formatEpochShanghai(ms) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).formatToParts(new Date(ms))
  const get = (type) => parts.find((part) => part.type === type)?.value || '00'
  const hour = get('hour') === '24' ? '00' : get('hour')
  return `${get('year')}-${get('month')}-${get('day')} ${hour}:${get('minute')}:${get('second')}`
}

/** Epoch millis render in Asia/Shanghai. Existing wall-clock strings are left unchanged. */
export function formatDateTime(value) {
  if (value == null || value === '') return '-'
  const text = String(value)
  if (/^\d{12,13}$/.test(text)) return formatEpochShanghai(Number(text))
  if (/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}/.test(text)) return text.replace('T', ' ').slice(0, 19)
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : text
}

/** Deploy environment from hostname (dev./test. prefix). env_mode is only the payment gateway mode. */
export function deployEnvLabel(host = typeof window !== 'undefined' ? window.location.hostname : '') {
  if (/^(dev\.|localhost|127\.)/.test(host)) return 'DEV'
  if (/^test\./.test(host)) return 'TEST'
  return '生产'
}
