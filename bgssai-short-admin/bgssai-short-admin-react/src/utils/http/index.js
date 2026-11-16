import { instance } from './axios'

// Optional config (timeout, signal, onUploadProgress, headers) is forwarded
// to axios untouched; existing two-argument calls keep the instance defaults.
const post = (url, data, config) => instance.post(url, data, config)
const get = (url, data, config) => instance.get(url, { params: data, ...config })
const put = (url, data, config) => instance.put(url, data, config)
const del = (url, data, config) => instance.delete(url, { params: data, ...config })

export default { post, get, put, del }
