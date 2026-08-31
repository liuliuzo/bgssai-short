import http from '../utils/http'

const list = () => http.get('/admin/subscription-plans/list')
const save = (body) => http.post('/admin/subscription-plans/save', body)

export default { list, save }
