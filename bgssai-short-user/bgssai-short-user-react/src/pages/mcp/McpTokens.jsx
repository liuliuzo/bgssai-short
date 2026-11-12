import { useEffect, useState } from 'react'
import { Alert, Button, Card, Empty, Input, Modal, Select, Table, Typography, message } from 'antd'
import { CopyOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import mcpTokensApi from '../../api/mcpTokens'

const { Paragraph, Text, Title } = Typography

const TOKEN = 'bgs_mcp_你的令牌'

function agent(key, group, label, hint, snippet) {
  return { key, group, label, hint, snippet }
}

function manual(key, group, label, hint) {
  return agent(key, group, label, hint, (endpoint) => `名称：bgssai-short
MCP URL：${endpoint}
请求头：Authorization: Bearer ${TOKEN}
本站只接受 Streamable HTTP 的 POST。该客户端没有公开的固定配置文件，上面三项填进它的自定义连接器。若它只支持本地命令或 OAuth，这段不能直接连上。`)
}

function httpJson(endpoint, extra) {
  return `{
  "mcpServers": {
    "bgssai-short": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }${extra || ''}
    }
  }
}`
}

const GROUPS = [
  { key: 'foreign', label: '国外' },
  { key: 'domestic', label: '国内' },
  { key: 'bot', label: 'Bgssai Bot' },
]

const AGENTS = [
  agent('claude-code', 'foreign', 'Claude Code',
    '这是 Claude Code 的命令，不是 Claude 网页或 Claude 桌面。',
    (endpoint) => `claude mcp add --transport http bgssai-short ${endpoint} \\
  --header "Authorization: Bearer ${TOKEN}"`),
  agent('codex', 'foreign', 'Codex',
    'Codex 不接受 --header。先把令牌放进环境变量，再用 --bearer-token-env-var。',
    (endpoint) => `# macOS / Linux
export BGSSAI_SHORT_MCP_TOKEN=${TOKEN}
# Windows PowerShell
$env:BGSSAI_SHORT_MCP_TOKEN="${TOKEN}"

codex mcp add bgssai-short --url ${endpoint} --bearer-token-env-var BGSSAI_SHORT_MCP_TOKEN`),
  agent('cursor', 'foreign', 'Cursor',
    '写入 .cursor/mcp.json 或用户级 ~/.cursor/mcp.json，保存后重启 Cursor。',
    (endpoint) => httpJson(endpoint)),
  manual('grok', 'foreign', 'Grok', 'Grok 没有公开的固定 MCP 配置文件。'),
  manual('chatgpt', 'foreign', 'ChatGPT', 'ChatGPT 的连接器多数走 OAuth，自定义项里不一定能手填请求头。'),
  agent('vscode', 'foreign', 'VS Code',
    'GitHub Copilot 用 .vscode/mcp.json。顶层键是 servers，并且要写 type 为 http。',
    (endpoint) => `{
  "servers": {
    "bgssai-short": {
      "type": "http",
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('windsurf', 'foreign', 'Windsurf',
    'Devin Desktop 仍读 ~/.codeium/windsurf/mcp_config.json。远程地址字段是 serverUrl。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "serverUrl": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('gemini', 'foreign', 'Gemini CLI',
    '写在 ~/.gemini/settings.json。httpUrl 才是 Streamable HTTP，url 会被当成 SSE。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "httpUrl": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('cline', 'foreign', 'Cline',
    '扩展设置或 ~/.cline/data/settings/cline_mcp_settings.json。不写 type 会按旧的 SSE 连接，本站会拒绝。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "type": "streamableHttp",
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('continue', 'foreign', 'Continue',
    '写在 config.json。mcpServers 是数组，请求头放在 requestOptions.headers。',
    (endpoint) => `{
  "mcpServers": [
    {
      "name": "bgssai-short",
      "type": "streamable-http",
      "url": "${endpoint}",
      "requestOptions": {
        "headers": { "Authorization": "Bearer ${TOKEN}" }
      }
    }
  ]
}`),
  agent('zed', 'foreign', 'Zed',
    '写在 Zed 设置的 context_servers，不是 mcpServers。',
    (endpoint) => `{
  "context_servers": {
    "bgssai-short": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('warp', 'foreign', 'Warp',
    '写入 ~/.warp/.mcp.json。',
    (endpoint) => httpJson(endpoint)),
  agent('opencode', 'foreign', 'OpenCode',
    '写在 opencode.json 的 mcp.servers。静态令牌要关掉 OAuth。',
    (endpoint) => `{
  "mcp": {
    "servers": {
      "bgssai-short": {
        "type": "remote",
        "url": "${endpoint}",
        "oauth": false,
        "headers": { "Authorization": "Bearer ${TOKEN}" }
      }
    }
  }
}`),
  agent('goose', 'foreign', 'Goose',
    'macOS / Linux 写 ~/.config/goose/config.yaml，Windows 写 %APPDATA%\\Block\\goose\\config\\config.yaml。类型必须是 streamable_http，地址字段是 uri。',
    (endpoint) => `extensions:
  bgssai-short:
    type: streamable_http
    uri: ${endpoint}
    enabled: true
    headers:
      Authorization: "Bearer ${TOKEN}"`),
  agent('workbuddy', 'domestic', 'WorkBuddy',
    '写入 ~/.workbuddy/.mcp.json，类型用 streamable-http。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "type": "streamable-http",
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  manual('doubao', 'domestic', '豆包', '豆包没有公开的固定 MCP 配置文件。'),
  manual('kimi-work', 'domestic', 'Kimi Work', 'Kimi Work 没有公开的固定 MCP 配置文件。'),
  agent('qoderwork', 'domestic', 'Qoder',
    '写入 ~/.qoder/settings.json 的 mcpServers，或项目 .mcp.json。类型用 streamable-http。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "type": "streamable-http",
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  agent('trae', 'domestic', 'TRAE',
    'TRAE 的 HTTP 配置就是 url 加 headers，可写在设置或项目 .trae/mcp.json。',
    (endpoint) => httpJson(endpoint)),
  manual('lingma', 'domestic', '通义灵码', '通义灵码公开说明主要是本地命令和 SSE。本站不是 SSE，纯 SSE 地址会连不上。'),
  agent('codebuddy', 'domestic', 'CodeBuddy',
    '写入 ~/.codebuddy/.mcp.json。HTTP 要显式写 type 为 http。',
    (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "type": "http",
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer ${TOKEN}" }
    }
  }
}`),
  manual('autoclaw', 'domestic', '智谱 AutoClaw', 'AutoClaw 没有公开的固定 MCP 配置文件。'),
  manual('deepseek', 'domestic', 'DeepSeek', 'DeepSeek 没有公开的固定 MCP 配置文件。'),
  manual('yuanbao', 'domestic', '腾讯元宝', '腾讯元宝没有公开的固定 MCP 配置文件。'),
  manual('wenxin', 'domestic', '文心', '文心没有公开的固定 MCP 配置文件。'),
  manual('stepfun', 'domestic', '阶跃桌面伙伴', '阶跃桌面伙伴没有公开的固定 MCP 配置文件。'),
  manual('lobster', 'domestic', 'LobsterAI', 'LobsterAI 没有公开的固定 MCP 配置文件。'),
  agent('bgssai-bot', 'bot', 'Bgssai Bot',
    '在 Bot 里填写这个产品的 MCP 地址和个人访问令牌。也可以用 Chat 登录后由平台下发，那时不用粘贴令牌。',
    (endpoint) => `名称：bgssai-short
MCP URL：${endpoint}
PAT：${TOKEN}`),
]

const fmtTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

export default function McpTokens() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [tokenName, setTokenName] = useState('')
  const [creating, setCreating] = useState(false)
  const [agentKey, setAgentKey] = useState('cursor')
  const endpoint = `${window.location.origin}/api/mcp`
  const activeAgent = AGENTS.find((agent) => agent.key === agentKey) || AGENTS[0]
  const agentOptions = GROUPS.map((group) => ({
    label: group.label,
    options: AGENTS.filter((agent) => agent.group === group.key).map((agent) => ({
      value: agent.key,
      label: agent.label,
    })),
  }))

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await mcpTokensApi.listTokens()
      if (data.success) setRows(data.result || [])
    } catch { /* interceptor */ } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const create = async () => {
    if (!tokenName.trim()) { message.warning('请填写令牌名称'); return }
    setCreating(true)
    try {
      const { data } = await mcpTokensApi.createToken({ token_name: tokenName.trim() })
      if (data.success) {
        setModalOpen(false)
        setTokenName('')
        Modal.success({
          title: 'MCP 令牌已生成（完整值仅此一次展示）',
          content: <Paragraph copyable={{ text: data.result?.token }} style={{ wordBreak: 'break-all', marginTop: 8 }}>
            <Text code>{data.result?.token}</Text>
          </Paragraph>,
          width: 560,
        })
        load()
      }
    } catch { /* interceptor */ } finally {
      setCreating(false)
    }
  }

  const copySnippet = async () => {
    try {
      await navigator.clipboard.writeText(activeAgent.snippet(endpoint))
      message.success('已复制配置片段')
    } catch {
      message.error('复制失败，请手动选中复制')
    }
  }

  const columns = [
    { title: '名称', dataIndex: 'token_name', width: 180, ellipsis: true },
    { title: '令牌', dataIndex: 'token_masked', width: 160, render: (t) => <Text code>{t}</Text> },
    { title: '最近使用', dataIndex: 'last_used_time', width: 160, render: fmtTime },
    { title: '创建时间', dataIndex: 'create_time', width: 160, render: fmtTime },
    {
      title: '操作',
      key: 'a',
      width: 80,
      render: (_, row) => (
        <Button type="link" size="small" danger icon={<DeleteOutlined />} aria-label="吊销令牌"
          onClick={() => Modal.confirm({
            title: '吊销令牌',
            content: `确认吊销「${row.token_name}」？AI 客户端将立即无法调用。`,
            okText: '吊销',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
              const { data } = await mcpTokensApi.revokeToken({ id: row.id })
              if (data.success) { message.success('已吊销'); load() }
            },
          })} />
      ),
    },
  ]

  return (
    <div style={{ padding: 24, maxWidth: 960 }}>
      <div style={{ marginBottom: 20 }}>
        <Title level={3} style={{ marginBottom: 4 }}>MCP 连接器</Title>
        <Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 15 }}>
          把 AI 工具连接到本应用，用个人访问令牌调用开放能力
        </Paragraph>
      </div>

      <Card title="连接 AI 工具" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="连接器分成国外、国内和 Bgssai Bot 三类，使用同一 MCP 端点。先创建令牌，再按工具粘贴下方片段。支持作品与原创剧本、正式分镜和参考图定妆、模型与声音目录、角色声音设置、图像/视频/配音/音乐/合成（需费用确认）、任务状态及真实成片下载；对外发布另需确认。"
        />
        <Paragraph style={{ marginBottom: 12 }}>
          端点：<Text code copyable>{endpoint}</Text>
        </Paragraph>
        <Select
          showSearch
          optionFilterProp="label"
          value={activeAgent.key}
          onChange={setAgentKey}
          options={agentOptions}
          style={{ width: '100%', maxWidth: 420, marginBottom: 16 }}
          aria-label="选择 AI 工具"
        />
        <Paragraph type="secondary" style={{ marginBottom: 12 }}>{activeAgent.hint}</Paragraph>
        <pre style={{
          fontSize: 12,
          background: 'var(--ant-color-fill-quaternary, #f5f5f5)',
          borderRadius: 8,
          padding: 12,
          overflowX: 'auto',
          marginBottom: 12,
          whiteSpace: 'pre-wrap',
        }}>{activeAgent.snippet(endpoint)}</pre>
        <Button icon={<CopyOutlined />} onClick={copySnippet}>复制配置</Button>
      </Card>

      <Card
        title="个人访问令牌"
        extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>创建令牌</Button>}
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="令牌明文只在创建时显示一次。建议按工具命名（Claude Code / Codex / Cursor / Bgssai Bot）。每人最多 5 枚。"
        />
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
          pagination={false} locale={{ emptyText: <Empty description="还没有 MCP 令牌" /> }} />
      </Card>

      <Modal title="创建 MCP 令牌" open={modalOpen} onOk={create} confirmLoading={creating}
        onCancel={() => setModalOpen(false)} okText="创建" cancelText="取消">
        <Input
          placeholder="备注名，例如 Cursor / Claude Code / Bgssai Bot"
          value={tokenName}
          maxLength={64}
          onChange={(e) => setTokenName(e.target.value)}
          onPressEnter={create}
        />
      </Modal>
    </div>
  )
}
