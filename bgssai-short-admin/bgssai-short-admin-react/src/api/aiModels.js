import http from '../utils/http'

const list = (params) => http.get('/admin/ai-models/list', params)
const create = (body) => http.post('/admin/ai-models/create', body)
const update = (body) => http.post('/admin/ai-models/update', body)
const setDefault = (body) => http.post('/admin/ai-models/set-default', body)
const toggleEnabled = (body) => http.post('/admin/ai-models/toggle-enabled', body)

export default { list, create, update, setDefault, toggleEnabled }
