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
    <div style={loggedIn ? undefined : { maxWidth: 880, margin: '0 auto', padding: '32px 20px 60px' }}>
      {!loggedIn ? (
        <Space style={{ marginBottom: 20 }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/login')}>
            前往登录页
          </Button>
        </Space>
      ) : null}

      <Title level={loggedIn ? 4 : 3} style={{ marginTop: 0 }}>下载 BGSSAI BOT</Title>
      <Paragraph type="secondary">
        安装后可将本应用托管给 Bot 直接操作。本页可在未登录时访问；安装包由本站提供。
      </Paragraph>

      {items.length === 0 && !loading ? (
        <Alert
          type="info"
          showIcon
          message="安装包即将上线"
          description="当前尚未发布可下载的安装包，请稍后再来，或关注产品更新通知。"
          style={{ marginBottom: 24 }}
        />
      ) : (
        <Card>
          <Table
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
    </div>
  )
}
