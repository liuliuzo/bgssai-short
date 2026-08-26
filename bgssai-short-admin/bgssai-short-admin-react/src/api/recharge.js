import http from '../utils/http'

const list = (params) => http.get('/admin/recharge/list', params)
const handle = (body) => http.post('/admin/recharge/handle', body)

export default { list, handle }
