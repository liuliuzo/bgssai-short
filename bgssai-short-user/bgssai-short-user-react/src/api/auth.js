import http from '../utils/http'

// User authentication.
// POST /bgssai/user/login  (namespaced so a reverse proxy routes it to the user backend).
// body:   { login_type: 'password', identifier, password }
// result: { jwt_token, role_code, role_name, user_id, user_name, real_name }
// Accounts log in with role_code === 'USER'.
const login = (data) => http.post('/user/login', data)
const sendSms = (data) => http.post('/user/sms/send', data)
const loginSms = (data) => http.post('/user/login/sms', data)
const sendEmail = (data) => http.post('/user/email/send', data)
const loginEmail = (data) => http.post('/user/login/email', data)

const oauthAuthorizeUrl = (data) => http.post('/user/oauth/authorize-url', data)
const oauthCallback = (data) => http.post('/user/oauth/callback', data)

export default { login, sendSms, loginSms, sendEmail, loginEmail, oauthAuthorizeUrl, oauthCallback }
