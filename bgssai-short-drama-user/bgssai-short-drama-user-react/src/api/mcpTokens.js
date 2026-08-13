import http from '../utils/http'

const listTokens = () => http.post('/mcp-tokens/list', {})

const createToken = (data) => http.post('/mcp-tokens/create', data)

const revokeToken = (data) => http.post('/mcp-tokens/revoke', data)

export default {
  listTokens,
  createToken,
  revokeToken,
}
