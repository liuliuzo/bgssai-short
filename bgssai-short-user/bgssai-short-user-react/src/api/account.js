import http from '../utils/http'

const loginMethods = () => http.get('/user/account/login-methods')
const bindOAuth = (body) => http.post('/user/account/login-methods/bind-oauth', body)
const bindContact = (body) => http.post('/user/account/login-methods/bind-contact', body)
const unbind = (body) => http.post('/user/account/login-methods/unbind', body)
const deletionPreview = () => http.get('/user/account/deletion/preview')
const deleteAccount = (body) => http.post('/user/account/deletion', body)

export default {
  loginMethods,
  bindOAuth,
  bindContact,
  unbind,
  deletionPreview,
  deleteAccount,
}
