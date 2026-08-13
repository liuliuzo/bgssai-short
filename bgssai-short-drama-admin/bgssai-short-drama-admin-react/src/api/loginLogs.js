import http from '../utils/http'

// Login-log list (paged).
// GET /bgssai/admin/login-logs?page_num=&page_size=
// Requires a valid JWT (header Jwttoken, injected by the axios interceptor).
// Returns the PageResponse envelope: { code, message, success, result, page_num, page_size, total_size }.
const list = (params) => http.get('/admin/login-logs', params)

export default { list }
