import http from '../utils/http'

const list = (params) => http.get('/admin/secrets/list', params)
const update = (body) => http.post('/admin/secrets/update', body)
const test = (body) => http.post('/admin/secrets/test', body)

export default { list, update, test }
