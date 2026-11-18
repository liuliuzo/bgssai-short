import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Modal, message } from 'antd'
import productionApi from '../../api/production'
import featuresApi from '../../api/features'
import { friendlyFailReason } from '../../lib/displayLabels'
import {
  VIDEO_PATH_STEPS,
  buildStepItems,
  buildVideoPathInput,
  resolveVideoPath,
  scriptReadyDecision,
  shotsForFrames,
  shotsForVideo,
} from './productionGuide'
import { autoTaskOutcomeMemory, shouldAnnounceTaskOutcome } from './taskOutcome'
import './video-path.css'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function formatYuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

function confirmBox(title, content, { okText = '确认', cancelText = '取消' } = {}) {
  return new Promise((resolve) => {
    Modal.confirm({
      title,
      content,
      okText,
      cancelText,
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

export default function ProductionGuide({
  work,
  episode,
  refreshKey = 0,
  cameraSaves,
  waitForShotDrafts,
  activeTab,
  viewingStageKey,
  onSelectStage,
  onOpenTab,
  onStepChange,
  onStepLoadError,
  onChanged,
}) {
  const [snap, setSnap] = useState(null)
  const [loadError, setLoadError] = useState(false)
  const [busy, setBusy] = useState(false)
  const [models, setModels] = useState({ text: null, image: null, video: null })
  const refreshSeqRef = useRef(0)
  const onStepChangeRef = useRef(onStepChange)
  const onStepLoadErrorRef = useRef(onStepLoadError)
  onStepChangeRef.current = onStepChange
  onStepLoadErrorRef.current = onStepLoadError

  const refresh = useCallback(async () => {
    if (!work?.work_id || !episode?.episode_id) return null
    const seq = refreshSeqRef.current + 1
    refreshSeqRef.current = seq
    const targetEpisodeId = episode.episode_id
    const [outlineRes, scriptRes, panelRes, shotRes, taskRes] = await Promise.all([
      productionApi.getOutline(work.work_id),
      productionApi.getScript(targetEpisodeId),
      productionApi.consistencyPanel(work.work_id),
      productionApi.listShots({ episode_id: targetEpisodeId, page_num: 1, page_size: 100 }),
      productionApi.listTasks({ work_id: work.work_id, page_num: 1, page_size: 100 }),
    ])
    if (seq !== refreshSeqRef.current) return null
    const next = {
      outline: outlineRes.data?.result,
      script: scriptRes.data?.result,
      panel: panelRes.data?.result,
      shots: Array.isArray(shotRes.data?.result) ? shotRes.data.result : [],
      tasks: Array.isArray(taskRes.data?.result) ? taskRes.data.result : [],
      episode,
    }
    const nextStep = resolveVideoPath(buildVideoPathInput(next))
    setSnap(next)
    setLoadError(false)
    onStepChangeRef.current?.(nextStep, targetEpisodeId)
    return { snap: next, step: nextStep }
  }, [work?.work_id, episode])

  useEffect(() => {
    let cancelled = false
    const targetEpisodeId = episode?.episode_id
    refresh().catch(() => {
      if (!cancelled) {
        setLoadError(true)
        onStepLoadErrorRef.current?.(targetEpisodeId)
      }
    })
    return () => { cancelled = true }
  }, [refresh, refreshKey, episode?.episode_id])

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
  const items = useMemo(
    () => buildStepItems(step, viewingStageKey),
    [step, viewingStageKey],
  )

  // Reloads step data only. It does not call run(), so a waiting outline cannot toast again.
  useEffect(() => {
    if (!step?.waiting) return undefined
    const timer = setInterval(() => {
      refresh().catch(() => {})
    }, 3000)
    return () => clearInterval(timer)
  }, [step?.waiting, step?.key, refresh])

  const finish = async (successText, prevKey) => {
    if (successText) message.success(successText)
    await onChanged?.()
    const refreshed = await refresh()
    const nextStep = refreshed?.step
    if (nextStep && prevKey && nextStep.key !== prevKey) {
      onSelectStage?.({ key: nextStep.key, tab: nextStep.tab })
    }
  }

  const outcomeText = (taskId, successText) => {
    if (taskId == null || taskId === '') return successText
    return shouldAnnounceTaskOutcome(autoTaskOutcomeMemory, taskId) ? successText : undefined
  }

  const run = async (kind) => {
    if (!step || busy || !episode?.episode_id) return
    const actionKind = kind || step.kind
    if (actionKind === 'wait') return
    const prevKey = step.key
    if (onSelectStage) {
      if (actionKind === 'open' || viewingStageKey !== step.key || activeTab !== step.tab) {
        onSelectStage({ key: step.key, tab: step.tab })
      }
    } else if (step.tab) {
      onOpenTab?.(step.tab)
    }
    if (actionKind === 'open') return
    setBusy(true)
    try {
      if (actionKind === 'outline') {
        const { data } = await productionApi.generateOutline(work.work_id, models.text?.model_code)
        if (!data?.success) return
        const done = await waitTask(work.work_id, data.result?.task_id)
        if (done?.task_status === 'SUCCEEDED') await finish(outcomeText(data.result?.task_id, '大纲已生成'), prevKey)
        else message.error(friendlyFailReason(done?.fail_reason) || '大纲没有生成成功')
        return
      }
      if (actionKind === 'script') {
        const { data } = await productionApi.generateScript(episode.episode_id, undefined, models.text?.model_code)
        if (!data?.success) return
        const done = await waitTask(work.work_id, data.result?.task_id)
        if (done?.task_status === 'SUCCEEDED') await finish(outcomeText(data.result?.task_id, '剧本已生成'), prevKey)
        else message.error(friendlyFailReason(done?.fail_reason) || '剧本没有生成成功')
        return
      }
      if (actionKind === 'ready') {
        const decision = scriptReadyDecision(snap?.script)
        if (decision.needsConfirm) {
          const ok = await confirmBox(decision.confirmTitle, decision.confirmContent, {
            okText: decision.okText,
            cancelText: decision.cancelText,
          })
          if (!ok) return
        }
        const { data } = await productionApi.readyScript(episode.episode_id, decision.forceReadyFlag)
        if (data?.success) await finish('这一集已定稿', prevKey)
        return
      }
      if (actionKind === 'lock') {
        const look = step.look
        if (!look?.candidateId) return
        const call = look.kind === 'character'
          ? productionApi.lockCharacter(look.id, look.candidateId)
          : productionApi.lockScene(look.id, look.candidateId)
        const { data } = await call
        if (data?.success) await finish(`已锁定「${look.name}」`, prevKey)
        return
      }
      if (actionKind === 'portrait') {
        const look = step.look
        if (!look?.id) return
        const unit = Number(models.image?.unit_price_cent ?? models.image?.image_fen_per_unit ?? 0)
        const price = Number.isFinite(unit) && unit > 0 ? `预计 ${(unit * 2 / 100).toFixed(2)} 元。` : '按实际出图计费。'
        const ok = await confirmBox(
          `生成「${look.name}」定妆`,
          `生成 2 张并锁定首张。${price}`,
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
          message.warning(step.key === 'scenes' ? '参考图还没回到当前场景上，请在场景区点击选定' : '定妆图还没回到当前定妆项上，请在定妆区点击选定')
          await finish(undefined, prevKey)
          return
        }
        const lock = look.kind === 'character'
          ? productionApi.lockCharacter(look.id, assetId)
          : productionApi.lockScene(look.id, assetId)
        const locked = await lock
        if (locked.data?.success) await finish(`已锁定「${look.name}」`, prevKey)
        else await refresh()
        return
      }
      if (actionKind === 'split') {
        const { data } = await productionApi.splitShots(episode.episode_id)
        if (!data?.success) return
        await featuresApi.previzGenerateText({ episode_id: episode.episode_id })
        await finish('已拆成镜头', prevKey)
        return
      }
      if (actionKind === 'previz') {
        const { data } = await featuresApi.previzGenerateText({ episode_id: episode.episode_id })
        if (data?.success) await finish('已补齐画面描述', prevKey)
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
        try {
          await waitForShotDrafts(body.shot_ids)
        } catch (error) {
          message.error(error.message || '画面描述保存失败')
          return
        }
        if (taskType === 'VIDEO' && !await cameraSaves.waitFor(body.shot_ids)) return
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
          await finish(actionKind === 'frame' ? '首帧已提交' : '视频已提交', prevKey)
        }
      }
    } finally {
      setBusy(false)
    }
  }

  if (!episode?.episode_id) return null

  const badgeClass = step
    ? (step.allDone ? 'video-path-badge is-done' : (step.waiting ? 'video-path-badge is-running' : 'video-path-badge is-current'))
    : 'video-path-badge'
  const badgeText = step
    ? (step.allDone
      ? `全流程已完成（${VIDEO_PATH_STEPS.length}/${VIDEO_PATH_STEPS.length}）`
      : step.waiting
        ? `当前环节：第 ${step.stepNo}/${VIDEO_PATH_STEPS.length} 步 · ${step.stepLabel}（生成中）`
        : `当前环节：第 ${step.stepNo}/${VIDEO_PATH_STEPS.length} 步 · ${step.stepLabel}`)
    : '当前环节：读取中'

  return (
    <section className="video-path" aria-label="出视频步骤">
      <div className="video-path-head">
        <span className={badgeClass}>{badgeText}</span>
        {step ? <span className="video-path-count">{`已完成 ${step.doneKeys.length}/${VIDEO_PATH_STEPS.length} 步`}</span> : null}
      </div>
      <ol className="video-path-steps">
        {items.map((item) => {
          const isCurrent = item.state === 'current' || item.state === 'running'
          const tagText = item.isViewing && !isCurrent
            ? `${item.statusLabel} · 查看中`
            : item.statusLabel
          const btnClass = [
            'video-path-step-btn',
            `is-${item.state}`,
            item.isViewing ? 'is-viewing' : '',
          ].filter(Boolean).join(' ')
          return (
            <li key={item.key}>
              <button
                type="button"
                className={btnClass}
                aria-current={isCurrent ? 'step' : undefined}
                onClick={() => onSelectStage?.(item)}
              >
                <span className="video-path-step-no">{item.stepNo}</span>
                <span className="video-path-step-label">{item.label}</span>
                <span className="video-path-step-tag">{tagText}</span>
              </button>
            </li>
          )
        })}
      </ol>
      {loadError ? (
        <div className="video-path-bar">
          <p className="video-path-title">读取失败</p>
          <div className="video-path-actions">
            <Button onClick={() => refresh().catch(() => setLoadError(true))}>重试</Button>
          </div>
        </div>
      ) : null}
      {!loadError && !step ? <p className="video-path-detail">读取中</p> : null}
      {!loadError && step ? (
        <div className="video-path-bar">
          <p className="video-path-title">{step.title}</p>
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
            {viewingStageKey !== step.key && !step.allDone ? (
              <Button onClick={() => onSelectStage?.({ key: step.key, tab: step.tab })}>
                {`返回当前环节：${step.stepLabel}`}
              </Button>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  )
}
