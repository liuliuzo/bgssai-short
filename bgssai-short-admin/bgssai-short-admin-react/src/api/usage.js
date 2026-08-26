import http from '../utils/http'

const summary = (params) => http.get('/admin/usage/summary', params)
const byCreator = (params) => http.get('/admin/usage/by-creator', params)

export default { summary, byCreator }
