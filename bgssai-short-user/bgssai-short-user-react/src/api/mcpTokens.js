import { instance } from '../utils/http/axios'

// 规范路径 /api/mcp/tokens（不走 /bgssai 前缀）。旧路径 /bgssai/mcp-tokens/* 仍可用。
const noPrefix = { baseURL: '' }

const listTokens = () => instance.post('/api/mcp/tokens/list', {}, noPrefix)

const createToken = (data) => instance.post('/api/mcp/tokens/create', data, noPrefix)

const revokeToken = (data) => instance.post('/api/mcp/tokens/revoke', data, noPrefix)

export default {
  listTokens,
  createToken,
  revokeToken,
}
