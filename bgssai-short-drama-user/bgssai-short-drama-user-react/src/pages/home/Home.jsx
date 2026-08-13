import { Card, Descriptions, Typography } from 'antd'
import { useUserStore } from '../../store/user'

const { Title, Paragraph } = Typography

// Sample landing page. Replace with the product home.
export default function Home() {
  const userInfo = useUserStore((s) => s.userInfo)

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>首页</Title>
      <Paragraph type="secondary">
        这是 AI 短剧制作平台用户端的示例首页。登录、鉴权、统一响应封装、分页列表等基础链路已就绪，
        按产品需求在此基础上新增页面与接口即可。
      </Paragraph>

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
