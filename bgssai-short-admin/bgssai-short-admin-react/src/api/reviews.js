import http from '../utils/http'

const list = (params) => http.get('/admin/reviews/list', params)
const detail = (reviewId) => http.get('/admin/reviews/detail', { review_id: reviewId })
const approve = (body) => http.post('/admin/reviews/approve', body)
const reject = (body) => http.post('/admin/reviews/reject', body)

export default { list, detail, approve, reject }
