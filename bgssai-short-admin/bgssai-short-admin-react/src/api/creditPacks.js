import http from '../utils/http'

const list = () => http.get('/admin/credit-packs/list')
const save = (body) => http.post('/admin/credit-packs/save', body)

export default { list, save }
