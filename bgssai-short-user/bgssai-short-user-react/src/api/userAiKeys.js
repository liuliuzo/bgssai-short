import http from '../utils/http'

const list = () => http.get('/user/ai-keys')
const upsert = (body) => http.post('/user/ai-keys', body)
const remove = (providerCode) => http.del(`/user/ai-keys/${encodeURIComponent(providerCode)}`)

export default {
  list,
  upsert,
  remove,
}
