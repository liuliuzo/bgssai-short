/** One field-level message. Callers must not also toast the same text. */

export const CONSENT_FIELD_ERROR = {
  id: 'login-consent',
  text: '请先阅读并同意用户协议与隐私政策',
}

export function passwordLoginFieldError(identifier, password) {
  if (!String(identifier ?? '').trim()) {
    return { id: 'login-identifier', text: '请输入账号' }
  }
  if (!password) {
    return { id: 'login-password', text: '请输入密码' }
  }
  return null
}

export function smsLoginFieldError(phoneError, smsCode, options = {}) {
  if (phoneError) return { id: 'login-phone', text: phoneError }
  if (options.requireCode === false) return null
  if (!/^\d{6}$/.test(String(smsCode ?? '').trim())) {
    return { id: 'login-sms-code', text: '请输入 6 位验证码' }
  }
  return null
}

export function emailLoginFieldError(email, emailCode, options = {}) {
  const trimmed = String(email ?? '').trim()
  if (!trimmed.includes('@')) {
    return { id: 'login-email', text: trimmed ? '请输入正确的邮箱' : '请输入邮箱' }
  }
  if (options.requireCode === false) return null
  if (!/^\d{6}$/.test(String(emailCode ?? '').trim())) {
    return { id: 'login-email-code', text: '请输入 6 位验证码' }
  }
  return null
}
