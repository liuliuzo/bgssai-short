import http from '../utils/http'

// My login-log list (paged).
// GET /bgssai/user/login-logs?page_num=&page_size=
// Requires a valid JWT (header Jwttoken, injected by the axios interceptor); the backend scopes
// the result to the current user. Returns the PageResponse envelope:
// { code, message, success, result, page_num, page_size, total_size }.
const list = (params) => http.get('/user/login-logs', params)
const logout = () => http.post('/user/logout', {})

export default { list, logout }
