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
      <Paragraph type="secondary">
        从建剧、写剧本、定妆、拆镜到成片和提审。平台不再提供 Mock 兜底：某个能力没接通会直接报错，
        不会返回假结果。想先看完整流程长什么样，可以打开示例作品《悬疑法医手记》。
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => navigate('/works')}>我的作品</Button>
        <Button onClick={() => navigate('/tasks')}>任务中心</Button>
        <Button onClick={() => navigate('/subscription')}>套餐与加量</Button>
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
