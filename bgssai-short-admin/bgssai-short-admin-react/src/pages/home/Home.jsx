import { useEffect, useState } from 'react'
import { Card, Descriptions, Typography, Button, Space, Statistic } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '../../store/user'
import reviewsApi from '../../api/reviews'
import creatorsApi from '../../api/creators'
import usageApi from '../../api/usage'

const { Title, Paragraph } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function Home() {
  const userInfo = useUserStore((s) => s.userInfo)
  const navigate = useNavigate()
  const [stats, setStats] = useState({ pending: null, creators: null, tasks: null, cost: null })

  useEffect(() => {
    let alive = true
    Promise.all([
      reviewsApi.list({ page_num: 1, page_size: 1, review_status: 'PENDING' }),
      creatorsApi.list({ page_num: 1, page_size: 1 }),
      usageApi.summary({}),
    ]).then(([reviewRes, creatorRes, usageRes]) => {
      if (!alive) return
      setStats({
        pending: Number(reviewRes.data?.total_size) || 0,
        creators: Number(creatorRes.data?.total_size) || 0,
        tasks: usageRes.data?.result?.task_count ?? 0,
        cost: usageRes.data?.result?.total_cost_cent ?? 0,
      })
    }).catch(() => {})
    return () => { alive = false }
  }, [])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>首页</Title>
      <Paragraph type="secondary">待审内容、创作者和近 31 天用量。</Paragraph>
      <Space style={{ marginBottom: 16 }} wrap>
        <Card><Statistic title="待审核" value={stats.pending ?? '-'} /></Card>
        <Card><Statistic title="创作者" value={stats.creators ?? '-'} /></Card>
        <Card><Statistic title="近 31 天任务" value={stats.tasks ?? '-'} /></Card>
        <Card><Statistic title="近 31 天成本(元)" value={stats.cost == null ? '-' : yuan(stats.cost)} /></Card>
      </Space>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => navigate('/reviews')}>内容审核</Button>
        <Button onClick={() => navigate('/creators')}>创作者</Button>
        <Button onClick={() => navigate('/credit-packs')}>加量包</Button>
        <Button onClick={() => navigate('/pay-merchants')}>支付商户</Button>
      </Space>
      <Card title="当前登录身份" style={{ maxWidth: 640 }}>
        <Descriptions column={1} size="small">
          <Descriptions.Item label="用户名">{userInfo?.user_name || '-'}</Descriptions.Item>
          <Descriptions.Item label="姓名">{userInfo?.real_name || '-'}</Descriptions.Item>
          <Descriptions.Item label="角色">{userInfo?.role_name || '管理员'}</Descriptions.Item>
          <Descriptions.Item label="用户 ID">{userInfo?.user_id || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
