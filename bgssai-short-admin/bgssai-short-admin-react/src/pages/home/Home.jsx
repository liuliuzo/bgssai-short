import { Card, Descriptions, Typography, Button, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '../../store/user'

const { Title, Paragraph } = Typography

export default function Home() {
  const userInfo = useUserStore((s) => s.userInfo)
  const navigate = useNavigate()

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>首页</Title>
      <Paragraph type="secondary">审核队列、创作者冻结、加量包与支付商户配置。</Paragraph>
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
          <Descriptions.Item label="角色">
            {userInfo?.role_name || '-'}（{userInfo?.role_code || '-'}）
          </Descriptions.Item>
          <Descriptions.Item label="用户 ID">{userInfo?.user_id || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
