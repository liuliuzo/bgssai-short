import BotDownloadSurface from './BotDownloadSurface'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Space, Table, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import { instance } from '../../utils/http/axios'
import { useUserStore, isAllowedRole } from '../../store/user'

const { Title, Paragraph, Text } = Typography

function formatSize(bytes) {
  if (bytes == null) return '-'
  const mb = bytes / 1024 / 1024
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`
}

export default function BotDownloadPage() {
  const navigate = useNavigate()
  const userInfo = useUserStore((s) => s.userInfo)
  const loggedIn = !!(userInfo && isAllowedRole(userInfo))
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    instance
      .get('/api/download/bot', { baseURL: '' })
      .then((res) => {
        if (alive) setItems(res.data?.result || [])
      })
      .catch(() => {
        if (alive) setItems([])
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  const columns = [
    {
      title: '安装包',
      dataIndex: 'file_name',
      render: (name) => <Text strong>{name}</Text>,
    },
    { title: '平台', dataIndex: 'platform', width: 120, render: (value) => value || '-' },
    { title: '大小', dataIndex: 'size_bytes', width: 120, render: formatSize },
    {
      title: '更新时间',
      dataIndex: 'updated_at',
      width: 200,
      render: (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      dataIndex: 'url',
      width: 140,
      render: (url) => (
        <Button type="primary" icon={<DownloadOutlined />} href={url} download>
          下载
        </Button>
      ),
    },
  ]

  return (
    <BotDownloadSurface>
      {!loggedIn ? (
        <Space style={{ marginBottom: 20 }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/login')}>
            前往登录页
          </Button>
        </Space>
      ) : null}

      <Title level={loggedIn ? 4 : 3} style={{ marginTop: 0 }}>下载 BGSSAI Bot</Title>
      <Paragraph type="secondary">
        安装 BGSSAI Bot 桌面客户端后，可通过自然语言对话授权智能体协助完成本应用内的日常操作，支持 Windows 与 Linux 环境。
      </Paragraph>

      {items.length === 0 && !loading ? (
        <Alert
          type="info"
          showIcon
          message="当前暂无可下载的安装包"
          description="最新版本正在准备中，发布后即可在此直接下载对应系统的安装程序。"
          style={{ marginBottom: 24 }}
        />
      ) : (
        <Card>
          <Table
            scroll={{ x: 760 }}
            rowKey="file_name"
            loading={loading}
            columns={columns}
            dataSource={items}
            pagination={false}
          />
        </Card>
      )}

      {!loggedIn ? (
        <Paragraph style={{ marginTop: 24 }}>
          <Link to="/login">登录本应用</Link>
        </Paragraph>
      ) : null}
    </BotDownloadSurface>
  )
}
