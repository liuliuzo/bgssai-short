import http from '../utils/http'

const list = (body) => http.post('/admin/support/sessions/list', body)
const detail = (body) => http.post('/admin/support/sessions/detail', body)
const reply = (body) => http.post('/admin/support/messages/reply', body)
const close = (body) => http.post('/admin/support/sessions/close', body)

export default { list, detail, reply, close }
