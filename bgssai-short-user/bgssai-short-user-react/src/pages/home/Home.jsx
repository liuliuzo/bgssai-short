import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Input, Modal, Progress, Skeleton, Typography, message } from 'antd'
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
  episodeCardSummary,
  episodeProgress,
  findEpisodeItem,
  nextStepText,
  workCardStatus,
} from './homeWorkbench'
import './home.css'

const { Link } = Typography

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
  const [episodesModal, setEpisodesModal] = useState(null)
  const [episodeQuery, setEpisodeQuery] = useState('')
  const [locatedKey, setLocatedKey] = useState(null)

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

  const openEpisodesModal = (workTitle, items) => {
    setEpisodeQuery('')
    setLocatedKey(null)
    setEpisodesModal({ title: workTitle || '未命名作品', items })
  }

  const closeEpisodesModal = () => {
    setEpisodesModal(null)
    setLocatedKey(null)
  }

  const locateEpisode = () => {
    const items = episodesModal?.items || []
    const found = findEpisodeItem(items, episodeQuery)
    if (!found) {
      message.info(`请输入 1–${items.length} 之间的集号`)
      return
    }
    setLocatedKey(found.key)
    const reduceMotion = typeof window !== 'undefined'
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    document.getElementById(`home-episode-all-${found.key}`)
      ?.scrollIntoView({ block: 'center', behavior: reduceMotion ? 'auto' : 'smooth' })
  }

  const renderEpisodeRow = (epItem, { idPrefix = '', located = false, onPick = null } = {}) => (
    <li key={epItem.key}>
      <button
        type="button"
        id={idPrefix ? `${idPrefix}${epItem.key}` : undefined}
        className={`home-episode-row is-${epItem.tone}${located ? ' is-located' : ''}`}
        title={epItem.text}
        onClick={() => {
          if (onPick) onPick()
          navigate(epItem.to)
        }}
      >
        <span className="home-episode-no">{epItem.noLabel}</span>
        <span className="home-episode-stage">{epItem.stageLabel}</span>
      </button>
    </li>
  )

  const modalSummary = episodesModal ? episodeCardSummary(episodesModal.items) : null

  return (
    <section className="home-workbench">
      <p className="home-next">
        <span>{guidance}</span>
        <Link onClick={openSample}>{SAMPLE_FLOW_LABEL}</Link>
      </p>

      {!loading && !loadError ? (
        <div className="home-start">
          <Button type="primary" onClick={() => navigate('/works?create=1')}>新建作品</Button>
        </div>
      ) : null}

      {loading ? <Skeleton active paragraph={{ rows: 4 }} /> : null}

      {!loading && loadError ? (
        <Alert
          type="warning"
          showIcon
          message="作品列表加载失败，请检查网络后重试"
          action={<Button size="small" onClick={load}>重试</Button>}
        />
      ) : null}

      {!loading && !loadError && works.length > 0 ? (
        <div className="home-work-grid">
          {works.map((work) => {
            const detail = detailsById[work.work_id]
            const episodes = episodesById[work.work_id] || []
            const progress = episodeProgress(work, detail ? episodes : null, detail)
            const statusText = workCardStatus(work, detail ? episodes : null, detail)
            const cover = coverUrl(detail) || coverUrl(work)
            const targetPath = continuePath(work, episodes, detail)
            const summary = episodeCardSummary(progress.episodeItems, targetPath)
            return (
              <Card key={work.work_id} size="small">
                <div className="home-work-body">
                  {cover ? <img className="home-cover" alt="" src={cover} /> : null}
                  <div className="home-work-main">
                    <div className="home-work-title">{work.work_title || '未命名作品'}</div>
                    {statusText ? <div className="home-work-status">{statusText}</div> : null}
                    {progress.label ? <div className="home-work-progress-label">{progress.label}</div> : null}
                    {progress.percent == null ? null : (
                      <Progress percent={progress.percent} size="small" showInfo={false} />
                    )}
                    {summary.inlineItems.length ? (
                      <ul
                        className={summary.folded ? 'home-episode-list home-episode-list--summary' : 'home-episode-list'}
                        aria-label="集数列表"
                      >
                        {summary.inlineItems.map((epItem) => renderEpisodeRow(epItem))}
                      </ul>
                    ) : null}
                    {summary.folded ? (
                      <div className="home-episode-summary">
                        <span>{summary.countsLabel}</span>
                        <Button
                          type="link"
                          size="small"
                          onClick={() => openEpisodesModal(work.work_title, progress.episodeItems)}
                        >
                          全部 {summary.total} 集
                        </Button>
                      </div>
                    ) : null}
                    <div className="home-work-actions">
                      <Button
                        type="primary"
                        size="small"
                        onClick={() => navigate(targetPath)}
                      >
                        {continueLabel(work, episodes, detail)}
                      </Button>
                    </div>
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
                <div className="home-attention-name">{item.workTitle || item.title}</div>
                {item.statusText ? <div className="home-attention-status">{item.statusText}</div> : null}
                {item.detail ? (
                  <div className="home-attention-detail">{item.detail}</div>
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

      <Modal
        title={episodesModal ? `《${episodesModal.title}》全部集数` : '全部集数'}
        open={Boolean(episodesModal)}
        onCancel={closeEpisodesModal}
        footer={null}
        width={640}
        destroyOnClose
      >
        {episodesModal ? (
          <>
            <div className="home-episode-jump">
              <Input
                value={episodeQuery}
                onChange={(event) => setEpisodeQuery(event.target.value)}
                onPressEnter={locateEpisode}
                placeholder={`输入集号，1–${episodesModal.items.length}`}
                inputMode="numeric"
                aria-label="按集号定位"
                style={{ width: 200 }}
              />
              <Button onClick={locateEpisode}>定位</Button>
              {modalSummary ? <span className="home-episode-jump-counts">{modalSummary.countsLabel}</span> : null}
            </div>
            <ul className="home-episode-all-list" aria-label="全部集数">
              {episodesModal.items.map((epItem) => renderEpisodeRow(epItem, {
                idPrefix: 'home-episode-all-',
                located: locatedKey === epItem.key,
                onPick: closeEpisodesModal,
              }))}
            </ul>
          </>
        ) : null}
      </Modal>
    </section>
  )
}
