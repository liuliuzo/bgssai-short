import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Modal, message } from 'antd'
import productionApi from '../../api/production'
import featuresApi from '../../api/features'
import { friendlyFailReason } from '../../lib/displayLabels'
import {
  VIDEO_PATH_STEPS,
  buildVideoPathInput,
  resolveVideoPath,
  shotsForFrames,
  shotsForVideo,
} from './productionGuide'
import './video-path.css'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function formatYuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

function confirmBox(title, content) {
  return new Promise((resolve) => {
    Modal.confirm({
      title,
      content,
      okText: '确认',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

async function waitTask(workId, taskId) {
  if (!taskId) return null
  const deadline = Date.now() + 16 * 60 * 1000
  let last = null
  while (Date.now() < deadline) {
    const { data } = await productionApi.listTasks({ work_id: workId, page_num: 1, page_size: 100 })
    const row = (data?.result || []).find((item) => item.task_id === taskId)
    if (row) last = row
    if (row && (row.task_status === 'SUCCEEDED' || row.task_status === 'FAILED' || row.task_status === 'CANCELED')) {
      return row
    }
    await sleep(2000)
  }
  return last
}

function defaultModel(list) {
  const models = Array.isArray(list) ? list : []
  return models.find((item) => item.default_flag === 1) || models[0] || null
}

export default function ProductionGuide({ work, episode, onOpenTab, onChanged }) {
  const [snap, setSnap] = useState(null)
  const [loadError, setLoadError] = useState(false)
  const [busy, setBusy] = useState(false)
  const [models, setModels] = useState({ text: null, image: null, video: null })

  const refresh = useCallback(async () => {
    if (!work?.work_id || !episode?.episode_id) return null
    const [outlineRes, scriptRes, panelRes, shotRes, taskRes] = await Promise.all([
      productionApi.getOutline(work.work_id),
      productionApi.getScript(episode.episode_id),
      productionApi.consistencyPanel(work.work_id),
      productionApi.listShots({ episode_id: episode.episode_id, page_num: 1, page_size: 100 }),
      productionApi.listTasks({ work_id: work.work_id, page_num: 1, page_size: 100 }),
    ])
    const next = {
      outline: outlineRes.data?.result,
      script: scriptRes.data?.result,
      panel: panelRes.data?.result,
      shots: Array.isArray(shotRes.data?.result) ? shotRes.data.result : [],
      tasks: Array.isArray(taskRes.data?.result) ? taskRes.data.result : [],
      episode,
    }
    setSnap(next)
    setLoadError(false)
    return next
  }, [work?.work_id, episode])

  useEffect(() => {
    let cancelled = false
    refresh().catch(() => {
      if (!cancelled) setLoadError(true)
    })
    return () => { cancelled = true }
  }, [refresh])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [textRes, imageRes, videoRes] = await Promise.all([
        productionApi.listAiModels('TEXT'),
        productionApi.listAiModels('IMAGE'),
        productionApi.listAiModels('VIDEO'),
      ])
      if (cancelled) return
      setModels({
        text: defaultModel(textRes.data?.result?.model_list),
        image: defaultModel(imageRes.data?.result?.model_list),
        video: defaultModel(videoRes.data?.result?.model_list),
      })
    })().catch(() => {})
    return () => { cancelled = true }
  }, [])

  const step = useMemo(
    () => (snap ? resolveVideoPath(buildVideoPathInput(snap)) : null),
    [snap],
  )

  useEffect(() => {
    if (!step?.waiting) return undefined
    const timer = setInterval(() => {
      refresh().catch(() => {})
    }, 3000)
    return () => clearInterval(timer)
  }, [step?.waiting, step?.key, refresh])

  const finish = async (successText) => {
    if (successText) message.success(successText)
    await onChanged?.()
    await refresh()
  }

  const run = async (kind) => {
    if (!step || busy || !episode?.episode_id) return
    const actionKind = kind || step.kind
    if (actionKind === 'wait') return
    if (step.tab) onOpenTab?.(step.tab)
    if (actionKind === 'open') return
    setBusy(true)
    try {
      if (actionKind === 'outline') {
        const { data } = await productionApi.generateOutline(work.work_id, models.text?.model_code)
        if (!data?.success) return
        const done = await waitTask(work.work_id, data.result?.task_id)
        if (done?.task_status === 'SUCCEEDED') await finish('大纲已生成')
        else message.error(friendlyFailReason(done?.fail_reason) || '大纲没有生成成功')
        return
      }
      if (actionKind === 'script') {
        const { data } = await productionApi.generateScript(episode.episode_id, undefined, models.text?.model_code)
        if (!data?.success) return
        const done = await waitTask(work.work_id, data.result?.task_id)
        if (done?.task_status === 'SUCCEEDED') await finish('剧本已生成')
        else message.error(friendlyFailReason(done?.fail_reason) || '剧本没有生成成功')
        return
      }
      if (actionKind === 'ready' || actionKind === 'ready-force') {
        const { data } = await productionApi.readyScript(episode.episode_id, actionKind === 'ready-force' ? 1 : 0)
        if (data?.success) await finish('这一集已定稿')
        return
      }
      if (actionKind === 'lock') {
        const look = step.look
        if (!look?.candidateId) return
        const call = look.kind === 'character'
          ? productionApi.lockCharacter(look.id, look.candidateId)
          : productionApi.lockScene(look.id, look.candidateId)
        const { data } = await call
        if (data?.success) await finish(`已锁定「${look.name}」`)
        return
      }
      if (actionKind === 'portrait') {
        const look = step.look
        if (!look?.id) return
        const unit = Number(models.image?.unit_price_cent ?? models.image?.image_fen_per_unit ?? 0)
        const price = Number.isFinite(unit) && unit > 0 ? `预计 ${(unit * 2 / 100).toFixed(2)} 元。` : '按实际出图计费。'
        const ok = await confirmBox(
          `给「${look.name}」出定妆`,
          `生成 2 张，成功后锁定第一张。${price}后面的卡会一张一张做。`,
        )
        if (!ok) return
        const call = look.kind === 'character'
          ? productionApi.portraitCharacter(look.id, models.image?.model_code, 2, 0)
          : productionApi.portraitScene(look.id, models.image?.model_code, 2, 0)
        const { data } = await call
        if (!data?.success || !data.result?.task_id) return
        const done = await waitTask(work.work_id, data.result.task_id)
        if (done?.task_status !== 'SUCCEEDED') {
          message.error(friendlyFailReason(done?.fail_reason) || '定妆没有生成成功')
          await refresh()
          return
        }
        const panelRes = await productionApi.consistencyPanel(work.work_id)
        const panel = panelRes.data?.result || {}
        const cards = look.kind === 'character' ? (panel.character_list || []) : [...(panel.scene_list || []), ...(panel.prop_list || [])]
        const card = cards.find((row) => (row.character_id || row.scene_card_id) === look.id)
        const assetId = card?.candidate_list?.[0]?.asset_id
        if (!assetId) {
          message.warning('定妆图还没回到这张卡上，请在角色场景里点选定')
          await finish()
          return
        }
        const lock = look.kind === 'character'
          ? productionApi.lockCharacter(look.id, assetId)
          : productionApi.lockScene(look.id, assetId)
        const locked = await lock
        if (locked.data?.success) await finish(`已锁定「${look.name}」`)
        else await refresh()
        return
      }
      if (actionKind === 'split') {
        const { data } = await productionApi.splitShots(episode.episode_id)
        if (!data?.success) return
        await featuresApi.previzGenerateText({ episode_id: episode.episode_id })
        await finish('已拆成镜头')
        return
      }
      if (actionKind === 'previz') {
        const { data } = await featuresApi.previzGenerateText({ episode_id: episode.episode_id })
        if (data?.success) await finish('已写好人话分镜')
        return
      }
      if (actionKind === 'frame' || actionKind === 'video') {
        const pool = snap?.shots || []
        const picked = actionKind === 'frame' ? shotsForFrames(pool) : shotsForVideo(pool)
        if (!picked.length) {
          message.warning(actionKind === 'frame' ? '还没有可以出图的镜头' : '还没有可以生成视频的镜头')
          return
        }
        const taskType = actionKind === 'frame' ? 'IMAGE' : 'VIDEO'
        const model = actionKind === 'frame' ? models.image : models.video
        const body = {
          episode_id: episode.episode_id,
          shot_ids: picked.map((row) => row.shot_id),
          task_type: taskType,
          auto_next_flag: 0,
          model_code: model?.model_code,
        }
        const { data: estRes } = await productionApi.estimateTasks(body)
        if (!estRes?.success) return
        const est = estRes.result || {}
        const ok = await confirmBox(
          actionKind === 'frame' ? '确认首帧费用' : '确认视频费用',
          `预计 ${formatYuan(est.estimated_cost_cent)} 元，余额 ${formatYuan(est.available_cent)} 元，共 ${est.task_count || picked.length} 个镜头。`,
        )
        if (!ok) return
        const { data } = await productionApi.submitTasks(body)
        if (data?.success) {
          await finish(actionKind === 'frame' ? '首帧已提交' : '视频已提交，生成通常要几分钟')
        }
      }
    } finally {
      setBusy(false)
    }
  }

  if (!episode?.episode_id) return null

  return (
    <section className="video-path" aria-label="出视频步骤">
      <ol className="video-path-steps">
        {VIDEO_PATH_STEPS.map((item) => {
          const done = step?.doneKeys?.includes(item.key)
          const current = step?.key === item.key
          const className = current ? 'is-current' : (done ? 'is-done' : '')
          return <li key={item.key} className={className}>{item.label}</li>
        })}
      </ol>
      {loadError ? (
        <div>
          <p className="video-path-title">步骤暂时读不到</p>
          <p className="video-path-detail">刷新后再看下一步。生成视频仍按大纲、剧本、定稿、定妆、拆镜、首帧这个顺序。</p>
          <Button onClick={() => refresh().catch(() => setLoadError(true))}>重试</Button>
        </div>
      ) : null}
      {!loadError && !step ? <p className="video-path-detail">正在看这一集做到哪一步。</p> : null}
      {!loadError && step ? (
        <div>
          <p className="video-path-title">{step.title}</p>
          <p className="video-path-detail">{step.detail}</p>
          <div className="video-path-actions">
            {step.action ? (
              <Button type="primary" loading={busy} disabled={step.waiting} onClick={() => run(step.kind)}>
                {step.action}
              </Button>
            ) : null}
            {step.secondary ? (
              <Button disabled={busy || step.waiting} onClick={() => run(step.secondary.kind)}>
                {step.secondary.action}
              </Button>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  )
}
