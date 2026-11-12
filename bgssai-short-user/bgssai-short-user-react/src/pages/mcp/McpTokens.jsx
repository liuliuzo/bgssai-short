import { useEffect, useState } from 'react'
import { Alert, Button, Card, Empty, Input, Modal, Select, Table, Typography, message } from 'antd'
import { CopyOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import mcpTokensApi from '../../api/mcpTokens'

const { Paragraph, Text, Title } = Typography

function remoteAgent(key, group, label, hint) {
  return {
    key,
    group,
    label,
    hint,
    snippet: (endpoint) => `MCP URL:  ${endpoint}
Auth:     Authorization: Bearer bgs_mcp_你的令牌
Transport: Streamable HTTP (POST)
Name:     bgssai-short`,
  }
}

function jsonAgent(key, group, label, hint, fileHint) {
  return {
    key,
    group,
    label,
    hint: `${hint} 配置文件：${fileHint}。`,
    snippet: (endpoint) => `{
  "mcpServers": {
    "bgssai-short": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer bgs_mcp_你的令牌" }
    }
  }
}`,
  }
}

const GROUPS = [
  { key: 'foreign', label: '国外' },
  { key: 'domestic', label: '国内' },
  { key: 'bot', label: 'Bgssai Bot' },
]

const AGENTS = [
  {
    key: 'claude',
    group: 'foreign',
    label: 'Claude',
    hint: 'Claude Code / Claude Desktop 通过 Streamable HTTP 连接本站 MCP。',
    snippet: (endpoint) => `claude mcp add --transport http bgssai-short ${endpoint} \\
  --header "Authorization: Bearer bgs_mcp_你的令牌"`,
  },
  {
    key: 'codex',
    group: 'foreign',
    label: 'Codex',
    hint: 'Codex CLI 添加远程 MCP，Bearer 头携带个人访问令牌。',
    snippet: (endpoint) => `codex mcp add bgssai-short --url ${endpoint} \\
  --header "Authorization: Bearer bgs_mcp_你的令牌"`,
  },
  {
    key: 'cursor',
    group: 'foreign',
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
    group: 'foreign',
    label: 'Grok Bot',
    hint: '在 Grok Bot 的 MCP / 远程工具配置中填入端点与 Bearer 令牌。',
    snippet: (endpoint) => `MCP URL:  ${endpoint}
Auth:     Authorization: Bearer bgs_mcp_你的令牌
Transport: Streamable HTTP (POST)`,
  },
  remoteAgent('chatgpt', 'foreign', 'ChatGPT', 'ChatGPT 桌面或网页的 Apps / Connectors 里添加远程 MCP。'),
  jsonAgent('vscode', 'foreign', 'VS Code', 'GitHub Copilot 的 MCP 配置。', '.vscode/mcp.json'),
  jsonAgent('windsurf', 'foreign', 'Windsurf', 'Windsurf / Devin Desktop 的 MCP 配置。', '~/.codeium/windsurf/mcp_config.json'),
  jsonAgent('gemini', 'foreign', 'Gemini', 'Gemini CLI / Antigravity 的 MCP 配置。', '~/.gemini/settings.json'),
  jsonAgent('cline', 'foreign', 'Cline', 'Cline 扩展的 MCP 配置。', '~/.cline/mcp.json'),
  jsonAgent('continue', 'foreign', 'Continue', 'Continue 的 MCP 配置。', 'config.json'),
  jsonAgent('zed', 'foreign', 'Zed', 'Zed 的上下文服务器配置。', '~/.config/zed/settings.json'),
  jsonAgent('warp', 'foreign', 'Warp', 'Warp 桌面端的 MCP 配置。', '~/.warp/.mcp.json'),
  jsonAgent('opencode', 'foreign', 'OpenCode', 'OpenCode 的 MCP 配置。', 'opencode.json'),
  jsonAgent('goose', 'foreign', 'Goose', 'Goose 的扩展配置。', '~/.config/goose/config.yaml'),
  jsonAgent('workbuddy', 'domestic', 'WorkBuddy', '腾讯 WorkBuddy 桌面端，用户级或项目级 mcp.json。', '~/.workbuddy/mcp.json'),
  remoteAgent('doubao', 'domestic', '豆包 PC', '豆包 PC / 豆包工作的自定义 MCP 连接器里填写端点与 Bearer 令牌。'),
  jsonAgent('kimi-work', 'domestic', 'Kimi Work', '月之暗面 Kimi Work 桌面端的 MCP 配置。', 'Kimi Work MCP 设置'),
  jsonAgent('qoderwork', 'domestic', 'QoderWork', '阿里 QoderWork 桌面端的 MCP 配置。', 'QoderWork MCP 设置'),
  jsonAgent('trae', 'domestic', 'TRAE', '字节 TRAE / TRAE Work 的 MCP 配置。', 'TRAE MCP 设置'),
  jsonAgent('lingma', 'domestic', '通义灵码', '通义灵码或 Qwen Code 的 MCP 配置。', '~/.qwen/settings.json'),
  jsonAgent('codebuddy', 'domestic', 'CodeBuddy', '腾讯云代码助手 CodeBuddy 的 MCP 配置。', 'CodeBuddy MCP 设置'),
  remoteAgent('autoclaw', 'domestic', '智谱 AutoClaw', '智谱 AutoClaw 桌面端的自定义工具或 MCP 配置里填写端点。'),
  remoteAgent('deepseek', 'domestic', 'DeepSeek', 'DeepSeek 桌面或 deepseek-harness 的 MCP 配置里填写端点。'),
  remoteAgent('yuanbao', 'domestic', '腾讯元宝', '腾讯元宝桌面端若提供 MCP / 自定义工具，填写同一端点。'),
  remoteAgent('wenxin', 'domestic', '文心', '文心一言或文心快码若提供 MCP / 自定义工具，填写同一端点。'),
  remoteAgent('stepfun', 'domestic', '阶跃桌面伙伴', '阶跃 AI 桌面伙伴的 MCP 配置里填写端点。'),
  remoteAgent('lobster', 'domestic', 'LobsterAI', '网易 LobsterAI 的本地 MCP 配置里填写端点。'),
  {
    key: 'bgssai-bot',
    group: 'bot',
    label: 'Bgssai Bot',
    hint: 'Bgssai Bot 可用本页 PAT 连接；也支持「用 Chat 登录」后下发 MCP（不在本页）。',
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
          message="令牌明文只在创建时显示一次。建议按工具命名（Claude / Codex / Cursor / Grok Bot / Bgssai Bot）。每人最多 5 枚。"
        />
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
          pagination={false} locale={{ emptyText: <Empty description="还没有 MCP 令牌" /> }} />
      </Card>

      <Modal title="创建 MCP 令牌" open={modalOpen} onOk={create} confirmLoading={creating}
        onCancel={() => setModalOpen(false)} okText="创建" cancelText="取消">
        <Input
          placeholder="备注名，例如 Cursor / Claude / Bgssai Bot"
          value={tokenName}
          maxLength={64}
          onChange={(e) => setTokenName(e.target.value)}
          onPressEnter={create}
        />
      </Modal>
    </div>
  )
}
