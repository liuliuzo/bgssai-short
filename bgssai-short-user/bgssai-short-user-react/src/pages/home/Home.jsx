import { Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import './home.css'

const STEPS = ['写一句梗概', '生成大纲', '定妆和分镜', '合成成片']

export default function Home() {
  const navigate = useNavigate()

  return (
    <section className="home-start">
      <p className="home-kicker">短剧制作</p>
      <h1>从一句话开始一部短剧</h1>
      <p className="home-lead">只要剧名和一句梗概。题材、集数和时长都有默认值，想改再展开。</p>
      <div className="home-actions">
        <Button type="primary" size="large" onClick={() => navigate('/works?create=1')}>
          新建短剧
        </Button>
        <Button size="large" onClick={() => navigate('/works')}>
          继续已有作品
        </Button>
      </div>
      <ol className="home-steps">
        {STEPS.map((label, index) => (
          <li key={label} style={{ animationDelay: `${index * 70}ms` }}>
            <span>{index + 1}</span>
            {label}
          </li>
        ))}
      </ol>
    </section>
  )
}
