import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  CONSENT_FIELD_ERROR,
  emailLoginFieldError,
  passwordLoginFieldError,
  smsLoginFieldError,
} from './loginFieldError.js'

describe('login field errors', () => {
  it('puts an empty password-tab account under the account field only', () => {
    assert.deepEqual(passwordLoginFieldError('  ', 'secret'), { id: 'login-identifier', text: '请输入账号' })
    assert.deepEqual(passwordLoginFieldError('ada', ''), { id: 'login-password', text: '请输入密码' })
    assert.equal(passwordLoginFieldError('ada', 'secret'), null)
  })

  it('keeps phone, email, and consent on their own fields', () => {
    assert.deepEqual(smsLoginFieldError('请输入手机号', ''), { id: 'login-phone', text: '请输入手机号' })
    assert.deepEqual(smsLoginFieldError(null, '12'), { id: 'login-sms-code', text: '请输入 6 位验证码' })
    assert.equal(smsLoginFieldError(null, '', { requireCode: false }), null)
    assert.deepEqual(emailLoginFieldError('', ''), { id: 'login-email', text: '请输入邮箱' })
    assert.deepEqual(emailLoginFieldError('ada@example.com', '1'), { id: 'login-email-code', text: '请输入 6 位验证码' })
    assert.equal(CONSENT_FIELD_ERROR.id, 'login-consent')
    assert.equal(CONSENT_FIELD_ERROR.text.includes('用户协议'), true)
  })
})
