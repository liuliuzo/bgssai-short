import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Progress, Skeleton, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import productionApi from '../../api/production'
import {
  RECENT_WORK_LIMIT,
  SAMPLE_FLOW_LABEL,
  SAMPLE_WORK_TITLE,
  attentionItems,
  ownedSamplePath,
  continuePath,
  continueLabel,
  coverUrl,
  episodeProgress,
  nextStepText,
} from './homeWorkbench'
import './home.css'

const { Text, Link } = Typography

async function detailsFor(works) {
  const pairs = await Promise.all(works.map(async (work) => {
    try {
      const { data } = await productionApi.getWork(work.work_id)
      return [work.work_id, data?.result && typeof data.result === 'object' ? data.result : null]
    } catch {
      return [work.work_id, null]
    }
  }))
  return Object.fromEntries(pairs)
}

async function failedTasksFor(works) {
  const pairs = await Promise.all(works.map(async (work) => {
    try {
      const { data } = await productionApi.listTasks({
        work_id: work.work_id,
        task_status: 'FAILED',
        page_num: 1,
        page_size: 5,
      })
      return [work.work_id, Array.isArray(data?.result) ? data.result : []]
    } catch {
      return [work.work_id, []]
    }
  }))
  return Object.fromEntries(pairs)
}

export default function Home() {
  const navigate = useNavigate()
  const [works, setWorks] = useState([])
  const [detailsById, setDetailsById] = useState({})
  const [failedByWorkId, setFailedByWorkId] = useState({})
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(false)
    try {
      const { data } = await productionApi.listWorks({ page_num: 1, page_size: RECENT_WORK_LIMIT })
      const list = Array.isArray(data?.result) ? data.result : []
      const [details, failed] = await Promise.all([detailsFor(list), failedTasksFor(list)])
      setWorks(list)
      setDetailsById(details)
      setFailedByWorkId(failed)
    } catch {
      setWorks([])
      setDetailsById({})
      setFailedByWorkId({})
      setLoadError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const episodesById = useMemo(() => {
    const map = {}
    for (const [workId, detail] of Object.entries(detailsById)) {
      map[workId] = Array.isArray(detail?.episode_list) ? detail.episode_list : []
    }
    return map
  }, [detailsById])
  const attention = useMemo(
    () => attentionItems({ works, episodesById, failedByWorkId }),
    [works, episodesById, failedByWorkId],
  )
  const guidance = nextStepText({ loadError, works, attention })

  const openSample = async () => {
    const known = ownedSamplePath(works)
    if (known) {
      navigate(known)
      return
    }
    try {
      const { data } = await productionApi.listWorks({
        keyword: SAMPLE_WORK_TITLE,
        page_num: 1,
        page_size: 20,
      })
      const found = ownedSamplePath(Array.isArray(data?.result) ? data.result : [])
      if (found) {
        navigate(found)
        return
      }
    } catch {
      // The list call already reported the failure. Do not open another account's sample.
    }
    message.info('当前账号没有示例作品《悬疑法医手记》')
  }

  return (
    <section className="home-workbench">
      <p className="home-next">
        <span>{guidance}</span>
        <Link onClick={openSample}>{SAMPLE_FLOW_LABEL}</Link>
      </p>

      {loading ? <Skeleton active paragraph={{ rows: 4 }} /> : null}

      {!loading && loadError ? (
        <Alert
          type="warning"
          showIcon
          message="作品列表暂时读不到"
          action={<Button size="small" onClick={load}>重试</Button>}
        />
      ) : null}

      {!loading && !loadError && works.length === 0 ? (
        <div>
          <Button type="primary" onClick={() => navigate('/works?create=1')}>新建作品</Button>
        </div>
      ) : null}

      {!loading && !loadError && works.length > 0 ? (
        <div className="home-work-grid">
          {works.map((work) => {
            const detail = detailsById[work.work_id]
            const episodes = episodesById[work.work_id] || []
            const progress = episodeProgress(work, detail ? episodes : null)
            const cover = coverUrl(detail) || coverUrl(work)
            return (
              <Card key={work.work_id} size="small">
                <div className="home-work-body">
                  {cover ? <img className="home-cover" alt="" src={cover} /> : null}
                  <div className="home-work-main">
                    <Text strong ellipsis={{ tooltip: work.work_title }}>{work.work_title || '未命名作品'}</Text>
                    <div className="home-work-meta">
                      <Text type="secondary">
                        {[work.work_status_name, progress.label].filter(Boolean).join(' · ')}
                      </Text>
                    </div>
                    {progress.percent == null ? null : (
                      <Progress percent={progress.percent} size="small" showInfo={false} />
                    )}
                    <Button
                      type="primary"
                      size="small"
                      onClick={() => navigate(continuePath(work, episodes))}
                    >
                      {continueLabel(work, episodes)}
                    </Button>
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      ) : null}

      {!loading && attention.length > 0 ? (
        <div>
          <p className="home-section-label">需要处理</p>
          <div className="home-attention-grid">
            {attention.map((item) => (
              <Card key={item.key} size="small">
                <Text>{item.title}</Text>
                {item.detail ? (
                  <div className="home-work-meta"><Text type="secondary">{item.detail}</Text></div>
                ) : null}
                <div>
                  <Button type="link" size="small" style={{ paddingInline: 0 }} onClick={() => navigate(item.to)}>
                    {item.action}
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        </div>
      ) : null}
    </section>
  )
}
