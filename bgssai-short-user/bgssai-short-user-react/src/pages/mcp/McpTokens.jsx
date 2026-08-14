import { useEffect, useState } from 'react'
import { Alert, Button, Card, Empty, Input, Modal, Table, Typography, message } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import mcpTokensApi from '../../api/mcpTokens'

const { Paragraph, Text } = Typography

const fmtTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

export default function McpTokens() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [tokenName, setTokenName] = useState('')
  const [creating, setCreating] = useState(false)
  const endpoint = `${window.location.origin}/api/mcp`

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
      <Card title="MCP 接入" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>创建令牌</Button>}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="把本站能力提供给 Cursor / Claude 等 AI 客户端。令牌明文只在创建时显示一次。当前工具仅登录日志与健康探针。"
        />
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
          pagination={false} locale={{ emptyText: <Empty description="还没有 MCP 令牌" /> }} />
        <pre style={{ fontSize: 12, background: 'var(--ant-color-fill-quaternary, #f5f5f5)', borderRadius: 8, padding: 12, overflowX: 'auto', marginTop: 16 }}>
{`端点：${endpoint}

Cursor（.cursor/mcp.json）：
{
  "mcpServers": {
    "bgssai-short": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer bgs_mcp_你的令牌" }
    }
  }
}`}
        </pre>
      </Card>
      <Modal title="创建 MCP 令牌" open={modalOpen} onOk={create} confirmLoading={creating}
        onCancel={() => setModalOpen(false)} okText="创建" cancelText="取消">
        <Input placeholder="备注名，例如 Cursor" value={tokenName} maxLength={64}
          onChange={(e) => setTokenName(e.target.value)} />
      </Modal>
    </div>
  )
}
