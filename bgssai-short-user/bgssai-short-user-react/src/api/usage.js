import http from '../utils/http'

const summary = (params) => http.get('/user/usage/summary', params)
const records = (params) => http.get('/user/usage/records', params)
const applyRecharge = (body) => http.post('/user/recharge/apply', body)
const listRecharge = () => http.get('/user/recharge/list')

export default { summary, records, applyRecharge, listRecharge }
