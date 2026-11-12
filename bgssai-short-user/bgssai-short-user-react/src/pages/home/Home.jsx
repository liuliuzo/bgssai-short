import { Card, Descriptions, Typography, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '../../store/user'
import ProductMotion from '../../components/ProductMotion'
import './home.css'

const { Title, Paragraph } = Typography

const FLOW = ['建剧', '写剧本', '定妆', '拆镜', '成片', '提审']

export default function Home() {
  const userInfo = useUserStore((s) => s.userInfo)
  const navigate = useNavigate()

  return (
    <div className="short-home">
      <section className="short-home-hero">
        <div className="short-home-copy">
          <Title level={4} className="short-home-title">首页</Title>
          <Paragraph type="secondary" className="short-home-lead">
            从建剧、写剧本、定妆、拆镜到成片和提审，按步骤完成一部短剧。
            想先看完整流程，可以打开示例作品体验制作链路。
          </Paragraph>
          <ol className="short-home-flow" aria-hidden="true">
            {FLOW.map((step) => <li key={step}>{step}</li>)}
          </ol>
          <div className="short-home-actions">
            <Button type="primary" onClick={() => navigate('/works')}>我的作品</Button>
            <Button onClick={() => navigate('/tasks')}>任务中心</Button>
            <Button onClick={() => navigate('/subscription')}>套餐与加量</Button>
          </div>
        </div>
        <div className="short-home-visual" aria-hidden="true">
          <ProductMotion />
        </div>
      </section>
      <Card className="short-home-identity" title="当前登录身份">
        <Descriptions column={1} size="small">
          <Descriptions.Item label="用户名">{userInfo?.user_name || '-'}</Descriptions.Item>
          <Descriptions.Item label="姓名">{userInfo?.real_name || '-'}</Descriptions.Item>
          <Descriptions.Item label="角色">{userInfo?.role_name || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
