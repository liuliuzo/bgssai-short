import http from '../utils/http'

const list = (params) => http.get('/user/ai-model-keys/list', params)
const create = (body) => http.post('/user/ai-model-keys/create', body)
const update = (body) => http.post('/user/ai-model-keys/update', body)
const toggle = (body) => http.post('/user/ai-model-keys/toggle', body)
const setDefault = (body) => http.post('/user/ai-model-keys/set-default', body)
const remove = (body) => http.post('/user/ai-model-keys/delete', body)
const test = (body) => http.post('/user/ai-model-keys/test', body)

const createCredential = (body) => http.post('/user/ai-model-keys/credentials/create', body)
const updateCredential = (body) => http.post('/user/ai-model-keys/credentials/update', body)
const deleteCredential = (body) => http.post('/user/ai-model-keys/credentials/delete', body)
const activateCredential = (body) => http.post('/user/ai-model-keys/credentials/activate', body)
const testCredential = (body) => http.post('/user/ai-model-keys/credentials/test', body)

export default {
  createCredential,
  updateCredential,
  deleteCredential,
  testCredential,
  activateCredential,
  list,
  create,
  update,
  toggle,
  setDefault,
  remove,
  test,
}
