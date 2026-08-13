import { instance } from './axios'

const post = (url, data) => instance.post(url, data)
const get = (url, data) => instance.get(url, { params: data })
const put = (url, data) => instance.put(url, data)
const del = (url, data) => instance.delete(url, { params: data })

export default { post, get, put, del }
