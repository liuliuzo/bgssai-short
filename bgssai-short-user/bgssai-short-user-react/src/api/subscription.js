import http from '../utils/http'

const listPlans = () => http.get('/user/subscription/plans')
const me = () => http.get('/user/subscription/me')
const createPlanOrder = (planCode, paymentChannel) =>
  http.post('/user/subscription/order/create', { plan_code: planCode, payment_channel: paymentChannel })
const listPacks = () => http.get('/user/credit-packs')
const createPackOrder = (packCode, paymentChannel) =>
  http.post('/user/credit-packs/orders', { pack_code: packCode, payment_channel: paymentChannel })
const orderStatus = (orderNo) =>
  http.post('/user/subscription/order/status', { order_no: orderNo })
const listOrders = () => http.get('/user/subscription/orders')

export default { listPlans, me, createPlanOrder, listPacks, createPackOrder, orderStatus, listOrders }
