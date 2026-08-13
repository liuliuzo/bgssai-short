import http from '../utils/http'

// User authentication.
// POST /bgssai/user/login  (namespaced so a reverse proxy routes it to the user backend).
// body:   { login_type: 'password', identifier, password }
// result: { jwt_token, role_code, role_name, user_id, user_name, real_name }
// Accounts log in with role_code === 'USER'.
const login = (data) => http.post('/user/login', data)

const oauthAuthorizeUrl = (data) => http.post('/user/oauth/authorize-url', data)
const oauthCallback = (data) => http.post('/user/oauth/callback', data)

export default { login, oauthAuthorizeUrl, oauthCallback }
