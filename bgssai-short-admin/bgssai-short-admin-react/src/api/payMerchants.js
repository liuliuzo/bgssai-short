import http from '../utils/http'

const listAlipay = () => http.get('/admin/pay/alipay-configs')
const saveAlipay = (body) => http.post('/admin/pay/alipay-configs/save', body)
const activeAlipay = (id) => http.post('/admin/pay/alipay-configs/set-collection-active', { id })
const listWechat = () => http.get('/admin/pay/wechat-configs')
const saveWechat = (body) => http.post('/admin/pay/wechat-configs/save', body)
const activeWechat = (id) => http.post('/admin/pay/wechat-configs/set-collection-active', { id })

export default { listAlipay, saveAlipay, activeAlipay, listWechat, saveWechat, activeWechat }
