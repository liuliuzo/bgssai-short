import http from '../utils/http'

const list = (params) => http.get('/admin/creators/list', params)
const toggle = (body) => http.post('/admin/creators/toggle-enabled', body)
const adjustQuota = (body) => http.post('/admin/creators/adjust-quota', body)

export default { list, toggle, adjustQuota }
