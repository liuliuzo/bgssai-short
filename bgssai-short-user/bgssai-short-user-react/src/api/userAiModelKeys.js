import http from '../utils/http'

const list = (params) => http.get('/user/ai-model-keys/list', params)
const create = (body) => http.post('/user/ai-model-keys/create', body)
const update = (body) => http.post('/user/ai-model-keys/update', body)
const toggle = (body) => http.post('/user/ai-model-keys/toggle', body)
const setDefault = (body) => http.post('/user/ai-model-keys/set-default', body)
const remove = (body) => http.post('/user/ai-model-keys/delete', body)
const test = (body) => http.post('/user/ai-model-keys/test', body)
const updateMode = (body) => http.post('/user/ai-model-keys/mode', body)

export default {
  list,
  create,
  update,
  toggle,
  setDefault,
  remove,
  test,
  updateMode,
}
