import { useEffect, useState } from 'react'
import { Alert, Button, Card, Empty, Input, Modal, Tabs, Table, Typography, message } from 'antd'
import { CopyOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import mcpTokensApi from '../../api/mcpTokens'

const { Paragraph, Text, Title } = Typography

const AGENTS = [
  {
    key: 'claude',
    label: 'Claude',
    hint: 'Claude Code / Claude Desktop 通过 Streamable HTTP 连接本站 MCP。',
    snippet: (endpoint) => `claude mcp add --transport http bgssai-short ${endpoint} \\
  --header "Authorization: Bearer bgs_mcp_你的令牌"`,
  },
  {
    key: 'codex',
    label: 'Codex',
    hint: 'OpenAI Codex CLI 添加远程 MCP，Bearer 头携带个人访问令牌。',
    snippet: (endpoint) => `codex mcp add bgssai-short --url ${endpoint} \\
  --header "Authorization: Bearer bgs_mcp_你的令牌"`,
  },
  {
    key: 'cursor',
    label: 'Cursor',
    hint: '写入项目或用户级 .cursor/mcp.json，重启 Cursor 后生效。',
    snippet: (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer bgs_mcp_你的令牌" }
    }
  }
}`,
  },
  {
    key: 'grok-bot',
    label: 'Grok Bot',
    hint: '在 Grok Bot 的 MCP / 远程工具配置中填入端点与 Bearer 令牌。',
    snippet: (endpoint) => `MCP URL:  ${endpoint}
Auth:     Authorization: Bearer bgs_mcp_你的令牌
Transport: Streamable HTTP (POST)`,
  },
  {
    key: 'bgssai-bot',
    label: 'bgssai-bot',
    hint: 'bgssai-bot 可用本页 PAT 连接；也支持「用 Chat 登录」后下发 MCP（不在本页）。',
    snippet: (endpoint) => `MCP URL:  ${endpoint}
Auth:     Authorization: Bearer bgs_mcp_你的令牌
Transport: Streamable HTTP (POST)
# 或：用 Chat 登录后由平台下发 MCP，无需手工粘贴令牌`,
  },
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
  const activeAgent = AGENTS.find((a) => a.key === agentKey) || AGENTS[0]

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
        <Button type="link" size="small" danger icon={<DeleteOutlined />}
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
        <Title level={3} style={{ marginBottom: 4 }}>MCP Connectors</Title>
        <Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 15 }}>
          Connect your AI tools to bgssai
        </Paragraph>
      </div>

      <Card title="连接 AI 工具" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="为 Claude、Codex、Cursor、Grok Bot、bgssai-bot 配置同一 MCP 端点。先创建令牌，再按工具粘贴下方片段。当前工具仅登录日志与健康探针。"
        />
        <Paragraph style={{ marginBottom: 8 }}>
          端点：<Text code copyable>{endpoint}</Text>
        </Paragraph>
        <Tabs
          activeKey={agentKey}
          onChange={setAgentKey}
          items={AGENTS.map((agent) => ({
            key: agent.key,
            label: agent.label,
            children: (
              <div>
                <Paragraph type="secondary" style={{ marginBottom: 12 }}>{agent.hint}</Paragraph>
                <pre style={{
                  fontSize: 12,
                  background: 'var(--ant-color-fill-quaternary, #f5f5f5)',
                  borderRadius: 8,
                  padding: 12,
                  overflowX: 'auto',
                  marginBottom: 12,
                  whiteSpace: 'pre-wrap',
                }}>{agent.snippet(endpoint)}</pre>
                <Button icon={<CopyOutlined />} onClick={copySnippet}>复制配置</Button>
              </div>
            ),
          }))}
        />
      </Card>

      <Card
        title="个人访问令牌"
        extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>创建令牌</Button>}
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="令牌明文只在创建时显示一次。建议按工具命名（Claude / Codex / Cursor / Grok Bot / bgssai-bot）。每人最多 5 枚。"
        />
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
          pagination={false} locale={{ emptyText: <Empty description="还没有 MCP 令牌" /> }} />
      </Card>

      <Modal title="创建 MCP 令牌" open={modalOpen} onOk={create} confirmLoading={creating}
        onCancel={() => setModalOpen(false)} okText="创建" cancelText="取消">
        <Input
          placeholder="备注名，例如 Cursor / Claude / bgssai-bot"
          value={tokenName}
          maxLength={64}
          onChange={(e) => setTokenName(e.target.value)}
          onPressEnter={create}
        />
      </Modal>
    </div>
  )
}
