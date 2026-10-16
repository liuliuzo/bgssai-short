import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Space, Table, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import { instance } from '../../utils/http/axios'
import { useUserStore } from '../../store/user'

const { Title, Paragraph, Text } = Typography

function formatSize(bytes) {
  if (bytes == null) return '-'
  const mb = bytes / 1024 / 1024
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`
}

export default function BotDownloadPage() {
  const navigate = useNavigate()
  const userInfo = useUserStore((s) => s.userInfo)
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
    <div style={{ maxWidth: 880, margin: '0 auto', padding: '32px 20px 60px' }}>
      <Space style={{ marginBottom: 20 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(userInfo ? '/home' : '/login')}
        >
          {userInfo ? '返回首页' : '前往登录页'}
        </Button>
      </Space>

      <Title level={3} style={{ marginTop: 0 }}>下载 BGSSAI BOT</Title>
      <Paragraph type="secondary">
        装好 BGSSAI BOT 之后可以把本应用托管给 Bot 直接操作。
        本页不要求登录。安装包由本站提供，不上官网。
      </Paragraph>

      {items.length === 0 && !loading ? (
        <Alert
          type="info"
          showIcon
          message="本站尚未发布安装包"
          description="目录为空时不会列出假文件。发布时把 BGSSAI-Bot-Setup.exe 放到本应用的 Bot 下载目录即可。"
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

      <Paragraph style={{ marginTop: 24 }}>
        <Link to="/login">登录本应用</Link>
      </Paragraph>
    </div>
  )
}
