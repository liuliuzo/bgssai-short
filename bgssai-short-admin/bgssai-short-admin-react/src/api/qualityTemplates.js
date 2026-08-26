import http from '../utils/http'

const listTemplates = () => http.get('/admin/quality/templates/list')
const createTemplate = (body) => http.post('/admin/quality/templates/create', body)
const updateTemplate = (body) => http.post('/admin/quality/templates/update', body)
const toggleEnabled = (body) => http.post('/admin/quality/templates/toggle-enabled', body)
const thresholdSave = (body) => http.post('/admin/quality/threshold-save', body)

export default {
  listTemplates,
  createTemplate,
  updateTemplate,
  toggleEnabled,
  thresholdSave,
}
