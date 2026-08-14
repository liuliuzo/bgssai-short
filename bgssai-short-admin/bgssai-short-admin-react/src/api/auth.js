import http from '../utils/http'

// Admin authentication.
// POST /bgssai/admin/login  (namespaced so a reverse proxy routes it to the admin backend).
// body:   { login_type: 'password', identifier, password }
// result: { jwt_token, role_code, role_name, user_id, user_name, real_name }
// Only accounts with role_code === 'PLATFORM_ADMIN' are granted access.
const login = (data) => http.post('/admin/login', data)

export default { login }
