import { isUserKeyModelRow, modelPricingCompactLabel, modelPricingLabel } from '../../modelPricing'
import { creatorToolLabel } from '../../publicModel'
import { isPlaceholderMediaUrl, isRealPhotoUrl } from '../../lib/mediaUrl'
import UsageReceipts from '../../components/UsageReceipts'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Card, Checkbox, Collapse, Dropdown, Form, Input, Modal, Radio, Segmented, Select, Skeleton, Space, Table, Tag, Tooltip, Typography, message } from 'antd'
import ImageToVideoPanel from './ImageToVideoPanel'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import productionApi from '../../api/production'
import featuresApi from '../../api/features'
import VoiceStudioPage from './VoiceStudioPage'
import StoryMapPage from './StoryMapPage'
import QualityReportPage from './QualityReportPage'
import EpisodePreviewPage from './EpisodePreviewPage'
import ContinuityCenterPage from './ContinuityCenterPage'
import EditExportPage from './EditExportPage'
import SourceImportPage from './SourceImportPage'
import StoryboardCanvas from './StoryboardCanvas'
import ProductionGuide from './ProductionGuide.jsx'
import {
  AUX_STUDIO_TOOLS,
  CORE_STUDIO_TABS,
  VIDEO_PATH_STEPS,
  resolveScriptRhythmScore,
  resolveViewingStageKey,
  scriptReadyDecision,
  shotNeedsSpoken,
} from './productionGuide'
import {
  autoTaskOutcomeMemory,
  hasAnnouncedTaskOutcome,
  shouldAnnounceTaskOutcome,
  withoutOutlineTaskId,
} from './taskOutcome'
import { shotCardHeading, shotLatestTaskBadge, shotOffersVideoRegenerate } from './shotCardMedia'
import { runOfficialShotVideo } from './officialVideo'
import {
  hasPlayableShotVideo,
  hasRealFirstFrame,
  resolveRepairAction,
  scriptStudioGuide,
  summarizeConsistencyCards,
} from './productionFlow'
import { formatTubePublishHint, reviewActionState } from './reviewActions'
import { ClickThumb, ListShotVideo, ShotMediaColumn } from './shotListMedia'
import LookSlot from '../look/LookSlot'
import LibraryLookPicker, { ReferenceNotice } from '../look/LibraryLookPicker'
import { LOOK_ANGLES, cardGenerationCopy } from '../look/lookGroups'
import {
  appendPreset,
  assetBinderPlaceholder,
  assetLibrarySummary,
  bindOptionLabel,
  bindSelectProps,
  buildAssetBindPayload,
  frameLinkOptions,
  knownSceneName,
  modelSelectProps,
  outlineLongTextProps,
  presetIsActive,
  reconcileEmptyScene,
  shotCardContainment,
  shotDialogueLine,
  shotPresetGroups,
  shotVideoDialogProps,
  storyboardPrimaryPlan,
  canvasSwitchState,
  taskRowsFromResult,
} from './studioListUx'
import DramaAssistantDrawer from '../../components/DramaAssistantDrawer'
import {
  SHOT_TYPES,
  evaluateShotReadiness,
  validateTubePublishPreflight,
} from '../../utils/CinematographyHelper'
import { composePublishHints, publishStatusAlertType, tubeTokenMissingMessage } from '../../utils/baselineCards'
import {
  labelOf, SCRIPT_STATUS_LABEL, TARGET_TYPE_LABEL,
  localizePromptShotSize, friendlyFailReason, visibleFailReason,
} from '../../lib/displayLabels'
import {
  composeSurfaceFromEntry,
  episodeComposeTag,
  episodeComposeTagColor,
  episodeSelectLabel,
  portraitFilmPlayerStyle,
} from '../../utils/composeStatusSurface'

const { Title, Paragraph, Text } = Typography
const { TextArea } = Input

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function formatYuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

function confirmEstimate(est, title) {
  const breakdown = (est.shot_cost_list || []).slice(0, 8).map((s) =>
    `镜${s.shot_no} ${s.task_type} ${formatYuan(s.estimated_cost_cent)}元`).join('；')
  const modelText = isUserKeyModelRow(est)
    ? (est.display_name || est.model_name || est.model_code || '-')
    : (creatorToolLabel(est.display_name || est.model_code, est.task_type) || '-')
  return new Promise((resolve) => {
    Modal.confirm({
      title: title || '确认费用后提交',
      content: (
        <div>
          <p>预计费用 {formatYuan(est.estimated_cost_cent)} 元</p>
          <p>余额可用 {formatYuan(est.available_cent)} 元</p>
          <p>共 {est.task_count || 0} 项（{modelText}）</p>
          {breakdown ? <p>{breakdown}{(est.shot_cost_list || []).length > 8 ? '…' : ''}</p> : null}
        </div>
      ),
      okText: '确认提交',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function confirmOfficialVideo(preview, { regenerate } = {}) {
  const shots = (preview && preview.shot_list) || []
  const lines = shots.filter((shot) => Number(shot.blocked) !== 1)
  const blocked = shots.filter((shot) => Number(shot.blocked) === 1)
  const zeroCost = Number(preview?.estimated_cost_cent || 0) === 0
  return new Promise((resolve) => {
    Modal.confirm({
      title: regenerate ? '重新生成这一镜视频' : '确认生成视频',
      content: (
        <div>
          <p>
            {zeroCost
              ? (regenerate
                ? '将使用自有密钥重新生成。当前成片会保留，直到新视频成功后才替换。'
                : '确认后将使用自有密钥生成视频，不扣减平台额度。')
              : (regenerate
                ? '将开始一次新的生成，并按这一次计费。不会接着上次未完成的回执继续。当前成片会保留，直到新视频成功后才替换。'
                : '确认后开始一次新的生成，并按这一次计费。')}
          </p>
          {lines.map((shot) => (
            <p key={shot.shot_id}>镜{shot.shot_no}：{shot.route_name}。{shot.size_note}</p>
          ))}
          <p>
            {zeroCost
              ? '自有密钥调用（不扣减平台额度）。'
              : `预计费用 ${formatYuan(preview.estimated_cost_cent)} 元，余额可用 ${formatYuan(preview.available_cent)} 元。`}
          </p>
          {blocked.map((shot) => (
            <p key={`blocked-${shot.shot_id}`}>镜{shot.shot_no}：{shot.block_reason}</p>
          ))}
        </div>
      ),
      okText: regenerate ? '确认重新生成' : '确认提交',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function confirmScriptReady(scriptOrScore) {
  const decision = scriptReadyDecision(scriptOrScore)
  if (!decision.needsConfirm) return Promise.resolve(decision.forceReadyFlag)
  return new Promise((resolve) => {
    Modal.confirm({
      title: decision.confirmTitle,
      content: decision.confirmContent,
      okText: decision.okText,
      cancelText: decision.cancelText,
      onOk: () => resolve(decision.forceReadyFlag),
      onCancel: () => resolve(null),
    })
  })
}

async function waitTask(workId, taskId) {
  if (!taskId) return null
  const rows = await waitTasks(workId, [taskId])
  return rows[0] || null
}

/** Poll until the task ends. Video and multi-image looks can run about 15 minutes. */
async function waitTasks(workId, taskIds) {
  const ids = [...new Set((taskIds || []).filter(Boolean))]
  if (!ids.length) return []
  const deadline = Date.now() + 16 * 60 * 1000
  let lastMatched = []
  while (Date.now() < deadline) {
    const { data } = await productionApi.listTasks({ work_id: workId, page_num: 1, page_size: 100 })
    const rows = data?.result || []
    const matched = ids.map((id) => rows.find((item) => item.task_id === id)).filter(Boolean)
    lastMatched = matched
    const done = matched.length === ids.length
      && matched.every((row) => row.task_status === 'SUCCEEDED'
        || row.task_status === 'FAILED'
        || row.task_status === 'CANCELED')
    if (done) return matched
    await sleep(2000)
  }
  return lastMatched
}

function videoWatchCopy(watch) {
  const progress = watch.progress || {}
  const queued = Number(progress.queued) || 0
  const running = Number(progress.running) || 0
  const succeeded = Number(progress.succeeded) || 0
  const failed = Number(progress.failed) || 0
  if (watch.error) {
    return { type: 'warning', message: '进度暂时读不到', description: '可到任务中心查看，生成还在服务器上继续。' }
  }
  if (watch.lingering) {
    return { type: 'info', message: '视频仍在生成', description: '已等待较久，还没有结束。请到任务中心查看，这里不把它当成失败。' }
  }
  if (watch.done && failed > 0) {
    const reason = progress.failed_task_list && progress.failed_task_list[0] && progress.failed_task_list[0].fail_reason
    return { type: 'error', message: '有视频没有生成成功', description: friendlyFailReason(reason) || `失败 ${failed} 个，可在任务中心恢复` }
  }
  if (watch.done) {
    return { type: 'success', message: '视频已生成', description: `成功 ${succeeded} 个` }
  }
  return {
    type: 'info',
    message: '视频生成中',
    description: `排队 ${queued} · 执行中 ${running} · 已完成 ${succeeded}`,
  }
}

function reportTaskOutcome(task, successText) {
  if (task?.task_id != null && task.task_id !== '' && !shouldAnnounceTaskOutcome(autoTaskOutcomeMemory, task.task_id)) {
    return task.task_status === 'SUCCEEDED'
  }
  if (!task) {
    message.warning('任务仍在执行或已超时，请到任务中心查看最新状态')
    return false
  }
  if (task.task_status === 'FAILED' || task.task_status === 'CANCELED') {
    message.error(friendlyFailReason(task.fail_reason) || task.fail_reason_code || '任务失败，请稍后重试')
    return false
  }
  if (task.task_status === 'SUCCEEDED') {
    if (successText) message.success(successText)
    return true
  }
  message.warning('任务尚未结束，请到任务中心查看进度')
  return false
}

async function waitBatch(workId, batchId, taskIds) {
  if (batchId) {
    const deadline = Date.now() + 16 * 60 * 1000
    while (Date.now() < deadline) {
      const { data } = await productionApi.batchProgress(batchId)
      const result = data?.result
      if (result && (result.queued + result.running) === 0) {
        return result
      }
      await sleep(1500)
    }
  }
  return waitTasks(workId, taskIds)
}

function ScriptPane({ work, episodeId, focusedStage, onReload, onFlowChange, onJumpTab, initialOutlineTaskId, onOutlineTaskConsumed }) {
  const workId = work?.work_id
  const [outline, setOutline] = useState([])
  const [script, setScript] = useState(null)
  const [textModels, setTextModels] = useState([])
  const [textModelCode, setTextModelCode] = useState()
  const [loading, setLoading] = useState(true)
  const [settled, setSettled] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [scriptBusy, setScriptBusy] = useState('')
  const [openScenes, setOpenScenes] = useState([])
  const [outlineExpanded, setOutlineExpanded] = useState(focusedStage === 'outline')
  const loadedKey = useRef('')
  const navOutlineTaskRef = useRef(null)
  if (initialOutlineTaskId) {
    navOutlineTaskRef.current = { workId, taskId: initialOutlineTaskId }
  } else if (navOutlineTaskRef.current && navOutlineTaskRef.current.workId !== workId) {
    navOutlineTaskRef.current = null
  }

  const load = useCallback(async () => {
    if (!workId) {
      setLoading(false)
      setSettled(true)
      return
    }
    const key = `${workId}:${episodeId || ''}`
    if (loadedKey.current !== key) setSettled(false)
    setLoading(true)
    try {
      const outlineRes = await productionApi.getOutline(workId)
      setOutline(outlineRes.data?.result?.outline || [])
      if (episodeId) {
        const scriptRes = await productionApi.getScript(episodeId)
        setScript(scriptRes.data?.result || null)
      } else {
        setScript(null)
      }
      loadedKey.current = key
      onFlowChange?.()
    } finally {
      setLoading(false)
      setSettled(true)
    }
  }, [workId, episodeId, onFlowChange])

  const loadRef = useRef(load)
  const onReloadRef = useRef(onReload)
  const onOutlineTaskConsumedRef = useRef(onOutlineTaskConsumed)
  loadRef.current = load
  onReloadRef.current = onReload
  onOutlineTaskConsumedRef.current = onOutlineTaskConsumed

  useEffect(() => { load() }, [load])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const { data } = await productionApi.listAiModels('TEXT')
      if (cancelled) return
      const models = data?.result?.model_list || []
      setTextModels(models)
      setTextModelCode(models.find((m) => m.default_flag === 1)?.model_code || models[0]?.model_code)
    })()
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!workId) return undefined
    const nav = navOutlineTaskRef.current
    const taskIdFromNav = nav && nav.workId === workId ? nav.taskId : null
    let cancelled = false
    ;(async () => {
      let taskId = taskIdFromNav
      if (taskId && hasAnnouncedTaskOutcome(autoTaskOutcomeMemory, taskId)) {
        if (taskIdFromNav != null && String(taskIdFromNav) === String(taskId)) {
          onOutlineTaskConsumedRef.current?.(taskId)
        }
        return
      }
      if (!taskId) {
        const outlineRes = await productionApi.getOutline(workId)
        if (cancelled) return
        const existing = outlineRes.data?.result?.outline || []
        if (existing.length) return
        const { data } = await productionApi.listTasks({
          work_id: workId,
          task_type: 'TEXT',
          page_num: 1,
          page_size: 50,
        })
        if (cancelled) return
        const pending = (data?.result || []).find((row) =>
          row.target_type === 'OUTLINE'
          && (row.task_status === 'QUEUED' || row.task_status === 'RUNNING'))
        taskId = pending?.task_id
      }
      if (!taskId || cancelled) return
      if (hasAnnouncedTaskOutcome(autoTaskOutcomeMemory, taskId)) return
      setGenerating(true)
      try {
        const done = await waitTask(workId, taskId)
        if (cancelled) return
        reportTaskOutcome(done, '大纲已生成')
        if (taskIdFromNav != null && String(taskIdFromNav) === String(taskId)) {
          onOutlineTaskConsumedRef.current?.(taskId)
        }
        await onReloadRef.current?.()
        await loadRef.current?.()
      } finally {
        if (!cancelled) setGenerating(false)
      }
    })()
    return () => { cancelled = true }
    // `load` and `onReload` change after each studio reload. Depending on them
    // re-waits a succeeded outline and toasts「大纲已生成」again.
  }, [workId])

  useEffect(() => {
    if (!workId || !episodeId || script?.script_status !== 'GENERATING') return undefined
    let cancelled = false
    ;(async () => {
      const { data } = await productionApi.listTasks({
        work_id: workId,
        task_type: 'TEXT',
        page_num: 1,
        page_size: 50,
      })
      if (cancelled) return
      const pending = (data?.result || []).find((row) =>
        row.target_type === 'SCRIPT'
        && (!row.episode_id || row.episode_id === episodeId)
        && (row.task_status === 'QUEUED' || row.task_status === 'RUNNING'))
      if (!pending?.task_id || cancelled) return
      if (hasAnnouncedTaskOutcome(autoTaskOutcomeMemory, pending.task_id)) return
      setScriptBusy('generate')
      try {
        const done = await waitTask(workId, pending.task_id)
        if (cancelled) return
        reportTaskOutcome(done, '剧本已生成')
        await loadRef.current?.()
        await onReloadRef.current?.()
      } finally {
        if (!cancelled) setScriptBusy('')
      }
    })()
    return () => { cancelled = true }
  }, [workId, episodeId, script?.script_status])

  const runOutline = async () => {
    setGenerating(true)
    try {
      const { data } = await productionApi.generateOutline(work.work_id, textModelCode)
      if (!data?.success) return
      message.success('大纲任务已提交')
      const done = await waitTask(work.work_id, data.result.task_id)
      reportTaskOutcome(done, '大纲已生成')
      await onReload()
      await load()
    } finally {
      setGenerating(false)
    }
  }

  const saveOutline = async () => {
    await productionApi.saveOutline({ work_id: work.work_id, episode_list: outline })
    message.success('大纲已保存')
    await onReload()
  }

  const runScript = async () => {
    if (!episodeId || scriptBusy) return
    setScriptBusy('generate')
    try {
      const { data } = await productionApi.generateScript(episodeId, undefined, textModelCode)
      if (!data?.success) {
        if (data?.message) message.warning(data.message)
        return
      }
      message.success('剧本任务已提交')
      const done = await waitTask(work.work_id, data.result.task_id)
      reportTaskOutcome(done, '剧本已生成')
      await load()
      await onReload()
    } finally {
      setScriptBusy('')
    }
  }

  const saveScript = async () => {
    if (!episodeId || scriptBusy) return
    setScriptBusy('save')
    try {
      await productionApi.saveScript({
        episode_id: episodeId,
        script_version: script?.script_version,
        scene_list: script?.scene_list || [],
      })
      message.success('剧本已保存')
      await load()
    } finally {
      setScriptBusy('')
    }
  }

  const ready = async () => {
    if (!episodeId || scriptBusy) return
    const forceFlag = await confirmScriptReady(script)
    if (forceFlag === null) return
    setScriptBusy('ready')
    try {
      const { data } = await productionApi.readyScript(episodeId, forceFlag)
      if (data?.success) {
        message.success('剧本已定稿')
        await load()
        await onReload()
      } else if (data?.message) {
        message.warning({ content: data.message, key: data.message })
      }
    } finally {
      setScriptBusy('')
    }
  }

  const scenes = script?.scene_list || []
  const rhythm = script?.rhythm_check || {}
  const rhythmScore = resolveScriptRhythmScore(script)
  const isScriptReady = script?.script_status === 'READY'
  const lowRhythmScore = rhythmScore !== null && rhythmScore > 0 && rhythmScore < 60
  const episodeRow = work.episode_list?.find((e) => e.episode_id === episodeId)
  const guide = scriptStudioGuide({
    scriptStatus: script?.script_status,
    composeStatus: episodeRow?.compose_status,
  })
  const filmed = guide.mode === 'filmed'
  const doneReason = '这一集已有成片，不必再生成或定稿'

  const seenEpisode = useRef(episodeId)
  const scriptAtSwitch = useRef(null)
  useEffect(() => {
    const list = script?.scene_list || []
    if (seenEpisode.current !== episodeId) {
      seenEpisode.current = episodeId
      scriptAtSwitch.current = script
      setOpenScenes([])
      return
    }
    if (script && script === scriptAtSwitch.current) return
    if (!list.length) return
    const first = String(list[0].scene_no ?? 0)
    setOpenScenes((current) => (current.length ? current : [first]))
  }, [episodeId, script])

  const revealScene = (scene) => {
    const key = String(scene.scene_no)
    setOpenScenes((current) => (current.includes(key) ? current : current.concat(key)))
    window.setTimeout(() => {
      document.getElementById(`script-scene-${key}`)?.scrollIntoView({ block: 'start' })
    }, 60)
  }

  useEffect(() => {
    if (focusedStage === 'outline') setOutlineExpanded(true)
    else if (focusedStage === 'script' || focusedStage === 'ready') setOutlineExpanded(false)
    if (!focusedStage) return undefined
    const targetId = focusedStage === 'outline' ? 'script-outline-card' : 'script-episode-card'
    const timer = window.setTimeout(() => {
      document.getElementById(targetId)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }, 60)
    return () => window.clearTimeout(timer)
  }, [focusedStage])

  const showFullOutline = !settled || generating || outline.length === 0 || outlineExpanded

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      {showFullOutline ? (
        <Card
          id="script-outline-card"
          loading={loading || generating}
          title="全集大纲"
          extra={(
            <Space wrap>
              <Button onClick={() => onJumpTab?.('source')}>导入原著</Button>
              {settled && outline.length > 0 ? (
                <Button onClick={() => setOutlineExpanded(false)}>收起大纲</Button>
              ) : null}
              <Select
                placeholder="文本模型"
                value={textModelCode}
                onChange={setTextModelCode}
                style={{ width: 240 }}
                popupMatchSelectWidth={false}
                disabled={generating}
                options={textModels.map((m) => ({ value: m.model_code, label: modelPricingCompactLabel(m), title: modelPricingLabel(m) }))}
              />
              <Button onClick={runOutline} loading={generating}>生成大纲</Button>
              <Button type="primary" onClick={saveOutline} disabled={generating}>保存大纲</Button>
            </Space>
          )}
        >
          <Table
            rowKey="episode_no"
            size="small"
            pagination={false}
            tableLayout="fixed"
            dataSource={outline}
            columns={[
              { title: '集', dataIndex: 'episode_no', width: 64 },
              {
                title: '标题',
                dataIndex: 'episode_title',
                width: 180,
                render: (v, row, i) => <Input value={v} onChange={(e) => {
                  const next = outline.slice()
                  next[i] = { ...row, episode_title: e.target.value }
                  setOutline(next)
                }} />,
              },
              {
                title: '梗概',
                dataIndex: 'episode_synopsis',
                render: (v, row, i) => (
                  <Input.TextArea
                    {...outlineLongTextProps()}
                    value={v}
                    onChange={(e) => {
                      const next = outline.slice()
                      next[i] = { ...row, episode_synopsis: e.target.value }
                      setOutline(next)
                    }}
                  />
                ),
              },
              {
                title: '钩子',
                dataIndex: 'ending_hook',
                render: (v, row, i) => (
                  <Input.TextArea
                    {...outlineLongTextProps()}
                    value={v}
                    onChange={(e) => {
                      const next = outline.slice()
                      next[i] = { ...row, ending_hook: e.target.value }
                      setOutline(next)
                    }}
                  />
                ),
              },
            ]}
          />
        </Card>
      ) : (
        <Card id="script-outline-card" size="small">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <Text strong>{`全集大纲 · 共 ${outline.length} 集（已完成）`}</Text>
            <Space wrap>
              <Button size="small" onClick={() => onJumpTab?.('source')}>导入原著</Button>
              <Button size="small" onClick={() => setOutlineExpanded(true)}>展开编辑大纲</Button>
            </Space>
          </div>
        </Card>
      )}
      <Card id="script-episode-card" title={`第${work.episode_list?.find((e) => e.episode_id === episodeId)?.episode_no || ''}集剧本`} extra={<Space wrap>
        <Select
          placeholder="文本模型"
          value={textModelCode}
          onChange={setTextModelCode}
          style={{ width: 240 }}
          popupMatchSelectWidth={false}
          disabled={!!scriptBusy}
          options={textModels.map((m) => ({ value: m.model_code, label: modelPricingCompactLabel(m), title: modelPricingLabel(m) }))}
        />
        <Tooltip title={filmed ? doneReason : ''}>
          <span>
            <Button onClick={runScript} loading={scriptBusy === 'generate'} disabled={filmed || (!!scriptBusy && scriptBusy !== 'generate')}>生成剧本</Button>
          </span>
        </Tooltip>
        <Button onClick={saveScript} loading={scriptBusy === 'save'} disabled={!!scriptBusy || !scenes.length}>保存</Button>
        <Tooltip title={filmed ? doneReason : ''}>
          <span>
            <Button type={filmed || isScriptReady ? 'default' : 'primary'} onClick={ready} loading={scriptBusy === 'ready'} disabled={filmed || isScriptReady || !!scriptBusy || !scenes.length}>{isScriptReady ? '已定稿' : '定稿'}</Button>
          </span>
        </Tooltip>
      </Space>}>
        {!settled ? (
          <div style={{ minHeight: 96 }} />
        ) : filmed ? (
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 12 }}
            message="本集已有成片"
            action={<Button size="small" type="primary" onClick={() => onJumpTab?.('compose')}>查看成片</Button>}
          />
        ) : !outline.length ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message="请先生成全集大纲"
            action={<Button size="small" type="primary" loading={generating} onClick={runOutline}>立即生成大纲</Button>}
          />
        ) : !scenes.length ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message={scriptBusy === 'generate' ? '正在生成本集分场剧本' : '大纲已就绪，待生成本集剧本'}
            action={<Button size="small" type="primary" loading={scriptBusy === 'generate'} onClick={runScript}>生成本集剧本</Button>}
          />
        ) : !isScriptReady ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message={lowRhythmScore
              ? `剧本草稿待定稿（节奏评分 ${rhythmScore} 分）`
              : '剧本草稿待定稿'}
          />
        ) : (
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 12 }}
            message="本集剧本已定稿"
            action={(
              <Space>
                <Button size="small" type="primary" onClick={() => onJumpTab?.('characters')}>去角色定妆</Button>
                <Button size="small" onClick={() => onJumpTab?.('storyboard')}>去分镜与视频</Button>
              </Space>
            )}
          />
        )}
        <Paragraph type="secondary">
          状态 {isScriptReady ? '已定稿' : labelOf(SCRIPT_STATUS_LABEL, script?.script_status, script?.script_status || '-')} / 版本 {script?.script_version || 0} / 节奏总分 {rhythmScore ?? '-'}
        </Paragraph>
        {scenes.length ? (
          <Space wrap style={{ marginBottom: 8 }}>
            {scenes.map((scene) => (
              <Button key={scene.scene_no} size="small" onClick={() => revealScene(scene)}>
                场 {scene.scene_no}
              </Button>
            ))}
          </Space>
        ) : settled ? <Text type="secondary">这一集还没有场次。</Text> : null}
        <Collapse
          activeKey={openScenes}
          onChange={(keys) => setOpenScenes(Array.isArray(keys) ? keys : [keys])}
          items={scenes.map((scene, si) => ({
            key: String(scene.scene_no || si),
            label: `场 ${scene.scene_no} ${scene.scene_location || ''} ${scene.scene_summary || ''}`,
            children: (
              <div id={`script-scene-${scene.scene_no || si}`}>
                {scene.scene_location ? <Paragraph style={{ marginBottom: 4 }}>地点：{scene.scene_location}</Paragraph> : null}
                {scene.scene_summary ? <Paragraph style={{ marginBottom: 8 }}>这一场：{scene.scene_summary}</Paragraph> : null}
                {(scene.line_list || []).length ? (scene.line_list || []).map((line, li) => (
                  <div key={`${si}-${li}`} style={{ marginBottom: 8 }}>
                    <Text strong>{line.character_name || line.speaker_name || '旁白'}</Text>
                    <Input.TextArea
                      style={{ marginTop: 4 }}
                      value={line.line_text}
                      onChange={(e) => {
                        const next = scenes.map((s, i) => {
                          if (i !== si) return s
                          const lineList = (s.line_list || []).map((ln, j) => (j === li ? { ...ln, line_text: e.target.value } : ln))
                          return { ...s, line_list: lineList }
                        })
                        setScript({ ...script, scene_list: next })
                      }}
                    />
                  </div>
                )) : <Text type="secondary">这一场还没有对白。</Text>}
              </div>
            ),
          }))}
        />
      </Card>
    </Space>
  )
}

function cardStatusName(status) {
  return { CANDIDATE: '待建卡', DRAFT: '待定妆', GENERATING: '生成中', LOCKED: '已锁定' }[status] || status || '-'
}

function LibrarySkeleton() {
  return <Skeleton active title={false} paragraph={{ rows: 3 }} />
}

function CardReference({ reference, refImageUrl, candidates, selectedAssetId, busy, onUse }) {
  const ref = reference || { used: 0, source: 'NONE' }
  if (ref.source === 'NAME' && (ref.used === 1 || ref.used === true)) {
    return (
      <div style={{ marginTop: 12 }}>
        <ReferenceNotice reference={ref} />
        {ref.material_id ? (
          <Button size="small" style={{ marginTop: 8 }} disabled={busy} onClick={() => onUse(ref.material_id)}>就用这张做定妆照</Button>
        ) : null}
      </div>
    )
  }
  return (
    <div style={{ marginTop: 12 }}>
      <Text>{cardGenerationCopy({ reference: ref, refImageUrl, candidates, selectedAssetId })}</Text>
    </div>
  )
}

function CharacterPane({ work, section = 'characters', onFlowChange, onJumpTab }) {
  const isScenes = section === 'scenes'
  const [chars, setChars] = useState([])
  const [scenes, setScenes] = useState([])
  const [props, setProps] = useState([])
  const [cardsReady, setCardsReady] = useState(false)
  const [lookLibraries, setLookLibraries] = useState([])
  const [lookLibrariesReady, setLookLibrariesReady] = useState(false)
  const [imageModels, setImageModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [form] = Form.useForm()
  const [propForm] = Form.useForm()
  const [portrait, setPortrait] = useState(null)
  const [portraitCount, setPortraitCount] = useState(4)
  const [textOnly, setTextOnly] = useState(false)
  const [angleRedoModal, setAngleRedoModal] = useState(null)
  const [busyIds, setBusyIds] = useState(() => new Set())
  const busyRef = useRef(new Set())
  const ownImageRef = useRef(null)
  const ownImageTarget = useRef(null)
  const selectedModel = imageModels.find((item) => item.model_code === imageModelCode)
  const unitCent = Number(selectedModel?.unit_price_cent ?? selectedModel?.image_fen_per_unit ?? 0)
  const portraitPrice = Number.isFinite(unitCent) && unitCent > 0
    ? `预计 ${(unitCent * portraitCount / 100).toFixed(2)} 元`
    : '按任务实际对账'

  const load = useCallback(async () => {
    try {
      const [panelRes, librariesRes] = await Promise.allSettled([
        productionApi.consistencyPanel(work.work_id),
        productionApi.listLookLibraries(),
      ])
      if (panelRes.status === 'fulfilled') {
        const panel = panelRes.value?.data?.result || {}
        setChars(panel.character_list || [])
        setScenes(panel.scene_list || [])
        setProps(panel.prop_list || [])
      }
      if (librariesRes.status === 'fulfilled') {
        setLookLibraries(librariesRes.value?.data?.result?.library_list || [])
      }
      onFlowChange?.()
    } finally {
      setCardsReady(true)
      setLookLibrariesReady(true)
    }
  }, [work, onFlowChange])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const { data } = await productionApi.listAiModels('IMAGE')
      if (cancelled) return
      const models = data?.result?.model_list || []
      setImageModels(models)
      setImageModelCode((cur) => cur || models.find((m) => m.default_flag === 1)?.model_code || models[0]?.model_code)
    })()
    return () => { cancelled = true }
  }, [])

  const markBusy = (id, on) => {
    if (on) busyRef.current.add(id)
    else busyRef.current.delete(id)
    setBusyIds(new Set(busyRef.current))
  }

  const openGenerate = (kind, id, title, reference) => {
    setPortraitCount(4)
    setTextOnly(false)
    setPortrait({ kind, id, title, reference: reference || null })
  }

  const useLook = async (kind, id, materialId) => {
    if (!materialId || busyRef.current.has(id)) return
    markBusy(id, true)
    try {
      const call = kind === 'character'
        ? productionApi.applyCharacterLook(id, materialId)
        : productionApi.applySceneLook(id, materialId)
      const { data } = await call
      if (!data?.success) {
        if (!data?.message) message.error('没有用上这张照片')
        return
      }
      message.success(kind === 'character' ? '已用这张照片作为定妆照' : '已用这张照片作为参考图')
      await load()
    } catch {
      message.error('没有用上这张照片')
    } finally {
      markBusy(id, false)
    }
  }

  const saveCardToLibrary = async (materialKind, id, cardName, prompt, assetId) => {
    if (!assetId || busyRef.current.has(id)) return
    markBusy(id, true)
    const storeName = materialKind === 'PROP' ? '道具库' : materialKind === 'SCENE' ? '场景库' : '定妆库'
    try {
      const caption = [cardName, prompt].filter(Boolean).join(' ')
      const { data } = await productionApi.uploadLookMaterial({
        asset_id: assetId,
        material_kind: materialKind,
        material_name: cardName,
        caption,
      })
      if (!data?.success) {
        if (!data?.message) message.error(`没有存进${storeName}`)
        return
      }
      message.success(`已存入${storeName}`)
      await load()
    } catch {
      message.error(`没有存进${storeName}`)
    } finally {
      markBusy(id, false)
    }
  }

  async function restoreCardAngleMaterial(characterId, materialId) {
    if (!characterId || !materialId || busyRef.current.has(characterId)) return
    markBusy(characterId, true)
    try {
      const { data } = await productionApi.promoteLookMaterial(materialId, characterId)
      if (!data?.success) {
        if (!data?.message) message.error('没有换回成功')
        return
      }
      message.success('已换回')
      await load()
    } catch {
      message.error('没有换回成功')
    } finally {
      markBusy(characterId, false)
    }
  }

  async function completeAngles(row, angle, replace) {
    if (!row || busyRef.current.has(row.character_id)) return
    const views = row.look_view_list || []
    const hasRefPhoto = views.some((v) => isRealPhotoUrl(v.file_url)) || isRealPhotoUrl(row.ref_image_url)
    const defaultSkipRef = !hasRefPhoto || (angle === 'FRONT_FACE' && replace)
    const body = {
      character_id: row.character_id,
      model_code: imageModelCode,
      replace_existing: replace ? 1 : 0,
      skip_reference: defaultSkipRef ? 1 : 0,
      confirm_cost: 0,
    }
    if (angle) body.view_angle = angle
    if (row.appearance_prompt) body.caption = row.appearance_prompt
    markBusy(row.character_id, true)
    let quote
    try {
      const { data } = await productionApi.completeLookAngles(body)
      if (!data?.success) {
        if (!data?.message) message.error('没有排上生成')
        return
      }
      quote = data.result || {}
    } catch {
      message.error('没有排上生成')
      return
    } finally {
      markBusy(row.character_id, false)
    }
    const names = (quote.angle_name_list || []).join('、') || '这一面'
    const cost = Number(quote.estimated_cost_cent) || 0
    const priceText = cost > 0 ? `预计 ${(cost / 100).toFixed(2)} 元。` : '费用按这次出图对账。'
    setAngleRedoModal({
      characterId: row.character_id,
      characterName: row.character_name,
      angle: angle || null,
      replace: !!replace,
      angleLabel: names,
      hasRefPhoto,
      skipRef: defaultSkipRef,
      caption: row.appearance_prompt || '',
      priceText,
    })
  }

  async function confirmAngleRedoModal() {
    if (!angleRedoModal || busyRef.current.has(angleRedoModal.characterId)) return
    const modalState = angleRedoModal
    setAngleRedoModal(null)
    markBusy(modalState.characterId, true)
    try {
      const reqBody = {
        character_id: modalState.characterId,
        model_code: imageModelCode,
        replace_existing: modalState.replace ? 1 : 0,
        skip_reference: modalState.skipRef ? 1 : 0,
        confirm_cost: 1,
      }
      if (modalState.angle) reqBody.view_angle = modalState.angle
      if (modalState.caption && modalState.caption.trim()) {
        reqBody.caption = modalState.caption.trim()
      }
      const { data } = await productionApi.completeLookAngles(reqBody)
      if (!data?.success || !data.result?.task_id) {
        if (!data?.message) message.error('没有排上生成')
        return
      }
      const done = await waitTask(work.work_id, data.result.task_id)
      if (done && done.task_status !== 'SUCCEEDED') {
        message.error(done.fail_reason || '这一面没有生成出来')
      } else if (done) {
        message.success(modalState.replace ? '已重做' : '已生成')
      } else {
        message.info('还在生成，可以到任务中心看结果')
      }
      load()
    } catch {
      message.error('没有排上生成')
    } finally {
      markBusy(modalState.characterId, false)
    }
  }

  const startPortrait = () => {
    if (!portrait || busyRef.current.has(portrait.id)) return
    const target = portrait
    const count = portraitCount
    const model = imageModelCode
    const skip = textOnly
    setPortrait(null)
    markBusy(target.id, true)
    ;(async () => {
      try {
        const call = target.kind === 'character'
          ? productionApi.portraitCharacter(target.id, model, count, skip)
          : productionApi.portraitScene(target.id, model, count, skip)
        const { data } = await call
        if (!data?.success || !data.result?.task_id) {
          if (!data?.message) message.error('生成没有提交成功')
          return
        }
        const done = await waitTask(work.work_id, data.result.task_id)
        if (done && done.task_status !== 'SUCCEEDED') {
          message.error(`生成失败：${friendlyFailReason(done.fail_reason) || done.fail_reason_code || '未知原因'}`)
        }
        load()
      } catch {
        message.error('生成没有提交成功')
      } finally {
        markBusy(target.id, false)
      }
    })()
  }

  async function onOwnImage(event) {
    const file = event.target.files && event.target.files[0]
    event.target.value = ''
    const target = ownImageTarget.current
    if (!file || !target) return
    if (file.size > 6 * 1024 * 1024) {
      message.error('图片不能超过 6MB')
      return
    }
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = () => reject(new Error('read'))
      reader.readAsDataURL(file)
    }).catch(() => '')
    const comma = dataUrl.indexOf(',')
    const imageBase64 = comma >= 0 ? dataUrl.slice(comma + 1) : dataUrl
    if (!imageBase64) {
      message.error('图片读取失败')
      return
    }
    markBusy(target.id, true)
    try {
      const call = target.kind === 'character'
        ? productionApi.uploadCharacterPortrait(target.id, imageBase64, target.angle)
        : productionApi.uploadScenePortrait(target.id, imageBase64)
      const { data } = await call
      if (!data?.success) {
        if (!data?.message) message.error('图片没有上传成功')
        return
      }
      message.success('已上传并保存')
      load()
    } catch {
      message.error('图片没有上传成功')
    } finally {
      markBusy(target.id, false)
    }
  }

  const [lockingAll, setLockingAll] = useState(false)
  const lockAllPendingCandidates = async () => {
    if (lockingAll) return
    setLockingAll(true)
    try {
      let locked = 0
      for (const row of (isScenes ? [] : chars)) {
        if (row.card_status === 'LOCKED') continue
        const first = (row.candidate_list || []).find((item) => isRealPhotoUrl(item.file_url))
        if (first?.asset_id) {
          const { data } = await productionApi.lockCharacter(row.character_id, first.asset_id)
          if (data?.success) locked += 1
        }
      }
      for (const row of (isScenes ? scenes : props)) {
        if (row.card_status === 'LOCKED') continue
        const first = (row.candidate_list || []).find((item) => isRealPhotoUrl(item.file_url))
        if (first?.asset_id) {
          const { data } = await productionApi.lockScene(row.scene_card_id, first.asset_id)
          if (data?.success) locked += 1
        }
      }
      if (locked > 0) {
        message.success(`已一键锁定 ${locked} 项`)
        await load()
      }
    } finally {
      setLockingAll(false)
    }
  }

  const cardSummary = summarizeConsistencyCards(isScenes
    ? { scene_list: scenes }
    : { character_list: chars, prop_list: props })

  const modelSelect = (
    <Select
      placeholder="出图模型"
      value={imageModelCode}
      onChange={setImageModelCode}
      style={{ width: 220 }}
      options={imageModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
    />
  )

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <div>
        <Title level={5} style={{ margin: 0 }}>{isScenes ? '场景参考图' : '角色定妆与道具参考'}</Title>
      </div>
      <input ref={ownImageRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={onOwnImage} />
      {cardsReady && cardSummary.totalCards === 0 ? (
        <Alert
          type="info"
          showIcon
          message={isScenes ? '暂无场景' : '暂无角色或道具'}
          action={(
            <Space>
              <Button size="small" type="primary" onClick={() => onJumpTab?.('script')}>去剧本与定稿</Button>
              <Button size="small" onClick={() => onJumpTab?.('storyboard')}>去分镜与视频</Button>
            </Space>
          )}
        />
      ) : null}
      {cardsReady && cardSummary.totalCards > 0 && cardSummary.unlockedCards.length > 0 ? (
        <Alert
          type="warning"
          showIcon
          message={`待锁定 ${cardSummary.unlockedCards.length} 项（已锁定 ${cardSummary.lockedCount}/${cardSummary.totalCards}）`}
          description={cardSummary.unlockedCards.slice(0, 6).map((c) => `${c.kind}「${c.name}」`).join('、')}
          action={(
            <Space>
              {cardSummary.pendingSelectionCards.length > 0 ? (
                <Button size="small" type="primary" loading={lockingAll} onClick={lockAllPendingCandidates}>
                  一键锁定已有候选图
                </Button>
              ) : null}
              <Button size="small" onClick={() => onJumpTab?.('storyboard')}>去分镜与视频</Button>
            </Space>
          )}
        />
      ) : null}
      {cardsReady && cardSummary.totalCards > 0 && cardSummary.unlockedCards.length === 0 ? (
        <Alert
          type="success"
          showIcon
          message={`全部 ${cardSummary.totalCards} 项已锁定`}
          action={isScenes
            ? <Button size="small" type="primary" onClick={() => onJumpTab?.('storyboard')}>去分镜与视频</Button>
            : <Button size="small" type="primary" onClick={() => onJumpTab?.('scenes')}>去场景参考</Button>}
        />
      ) : null}
      {!isScenes ? (
      <Card title="角色卡" extra={<Space>{modelSelect}<Button onClick={load}>刷新</Button></Space>}>
        <Form form={form} layout="inline" style={{ marginBottom: 12 }} onFinish={async (v) => {
          await productionApi.createCharacter({ work_id: work.work_id, ...v })
          form.resetFields()
          load()
        }}>
          <Form.Item name="character_name" rules={[{ required: true }]}><Input placeholder="角色名" /></Form.Item>
          <Form.Item name="character_role"><Input placeholder="身份" /></Form.Item>
          <Form.Item name="appearance_prompt"><Input placeholder="外观描述" style={{ width: 220 }} /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !chars.length ? <Text type="secondary">暂无角色</Text> : null}
        {cardsReady ? chars.map((row) => {
          const views = row.look_view_list || []
          const viewOf = (angle) => views.find((view) => view.view_angle === angle && isRealPhotoUrl(view.file_url))
          const hasAnglePhoto = views.some((view) => isRealPhotoUrl(view.file_url))
          const candidates = [...(row.candidate_list || [])].sort((a, b) => {
            if (a.asset_id === row.ref_image_asset_id) return -1
            if (b.asset_id === row.ref_image_asset_id) return 1
            return 0
          })
          const hasRealCandidates = candidates.some((item) => isRealPhotoUrl(item.file_url))
          const isLocked = row.card_status === 'LOCKED'
          return (
          <Card key={row.character_id} size="small" style={{ marginBottom: 8 }} title={`${row.character_name}（${cardStatusName(row.card_status)}）· ${row.used_shot_count || 0} 镜`} extra={
            <Space>
              {isLocked ? <Tag color="green">已锁定</Tag> : (hasRealCandidates ? <Tag color="orange">请点下方「选定」锁定</Tag> : null)}
              {row.ref_image_asset_id && isRealPhotoUrl(row.ref_image_url) ? (
                <Button
                  size="small"
                  disabled={busyIds.has(row.character_id)}
                  onClick={() => saveCardToLibrary('CHARACTER', row.character_id, row.character_name, row.appearance_prompt, row.ref_image_asset_id)}
                >
                  存入定妆库
                </Button>
              ) : null}
              <Button size="small" type="primary" loading={busyIds.has(row.character_id)} disabled={busyIds.has(row.character_id)} onClick={() => openGenerate('character', row.character_id, row.character_name, row.look_reference)}>生成定妆</Button>
            </Space>
          }>
            <div style={{ display: 'flex', gap: 12, overflowX: 'auto' }}>
              {LOOK_ANGLES.map(([angle, label]) => {
                const view = viewOf(angle)
                const currentLook = angle === 'FRONT_FACE' && view && view.asset_id && view.asset_id === row.ref_image_asset_id
                return (
                  <LookSlot
                    key={angle}
                    label={label}
                    note={currentLook ? '当前定妆' : ''}
                    action={view ? '更换照片' : '上传照片'}
                    disabled={busyIds.has(row.character_id)}
                    onAction={() => { ownImageTarget.current = { kind: 'character', id: row.character_id, angle }; ownImageRef.current?.click() }}
                  >
                    {view ? <ClickThumb alt={label} src={view.file_url} width={104} height={140} /> : null}
                  </LookSlot>
                )
              })}
            </div>
            <div style={{ marginTop: 8 }}>
              <Space wrap style={{ marginTop: 4 }}>
                {LOOK_ANGLES.map(([angle, label]) => (
                  <Button
                    key={angle}
                    size="small"
                    disabled={busyIds.has(row.character_id)}
                    onClick={() => completeAngles(row, angle, !!viewOf(angle))}
                  >
                    {viewOf(angle) ? `重做${label}` : `生成${label}`}
                  </Button>
                ))}
                {LOOK_ANGLES.filter(([angle]) => !viewOf(angle)).length >= 2 ? (
                  <Button size="small" type="link" disabled={busyIds.has(row.character_id)} onClick={() => completeAngles(row, null, false)}>一次补齐空缺面</Button>
                ) : null}
              </Space>
            </div>
            {views.some((v) => Array.isArray(v.history_list) && v.history_list.some((h) => isRealPhotoUrl(h.file_url))) ? (
              <div style={{ marginTop: 10 }}>
                <Text type="secondary">历史照片：</Text>
                <Space wrap style={{ marginTop: 6 }}>
                  {views.flatMap((v) => (v.history_list || []).filter((h) => isRealPhotoUrl(h.file_url))).map((h) => (
                    <div key={h.material_id} style={{ textAlign: 'center', width: 84 }}>
                      <div style={{ fontSize: 12, color: '#595959', marginBottom: 2 }}>{h.view_name}</div>
                      <ClickThumb alt={`${row.character_name} ${h.view_name}`} src={h.file_url} width={72} height={100} />
                      <Button
                        size="small"
                        type="link"
                        style={{ padding: 0 }}
                        disabled={busyIds.has(row.character_id)}
                        onClick={() => restoreCardAngleMaterial(row.character_id, h.material_id)}
                      >
                        换回这张
                      </Button>
                    </div>
                  ))}
                </Space>
              </div>
            ) : null}
            <LibraryLookPicker
              kind="CHARACTER"
              libraries={lookLibraries}
              librariesLoading={!lookLibrariesReady}
              currentReference={row.look_reference}
              busy={busyIds.has(row.character_id)}
              onUse={(materialId) => useLook('character', row.character_id, materialId)}
            />
            {isRealPhotoUrl(row.ref_image_url) && !hasAnglePhoto ? (
              <div style={{ marginTop: 12 }}>
                <Text type="secondary">当前定妆照</Text>
                <div><ClickThumb alt="当前定妆照" src={row.ref_image_url} width={72} height={128} /></div>
              </div>
            ) : null}
            <CardReference
              reference={row.look_reference}
              refImageUrl={row.ref_image_url}
              candidates={candidates}
              selectedAssetId={row.ref_image_asset_id}
              busy={busyIds.has(row.character_id)}
              onUse={(materialId) => useLook('character', row.character_id, materialId)}
            />
            {hasRealCandidates ? (
              <div style={{ marginTop: 12 }}>
                <div style={{ marginBottom: 6 }}><Text type="secondary">定妆候选：</Text></div>
                <Space wrap>
                  {candidates.filter((item) => isRealPhotoUrl(item.file_url)).map((c) => (
                    <div key={c.asset_id} style={{ textAlign: 'center' }}>
                      <ClickThumb alt="定妆候选" src={c.file_url} width={72} height={128} />
                      <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                        const { data } = await productionApi.lockCharacter(row.character_id, c.asset_id)
                        if (!data?.success) return
                        message.success('已锁定定妆')
                        load()
                      }}>{row.ref_image_asset_id === c.asset_id ? '已选定' : '选定'}</Button>
                    </div>
                  ))}
                </Space>
              </div>
            ) : null}
          </Card>
          )
        }) : null}
      </Card>
      ) : null}
      {isScenes ? (
      <Card title="场景卡" extra={<Space>{modelSelect}<Button onClick={load}>刷新</Button></Space>}>
        <Form layout="inline" style={{ marginBottom: 12 }} onFinish={async (v) => {
          await productionApi.createScene({ work_id: work.work_id, card_type: 'SCENE', ...v })
          load()
        }}>
          <Form.Item name="card_name" rules={[{ required: true }]}><Input placeholder="场景名" /></Form.Item>
          <Form.Item name="appearance_prompt"><Input placeholder="布景描述" /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !scenes.length ? <Text type="secondary">暂无场景</Text> : null}
        {cardsReady ? scenes.map((row) => {
          const realSceneCandidates = (row.candidate_list || []).filter((item) => isRealPhotoUrl(item.file_url))
          const isLocked = row.card_status === 'LOCKED'
          return (
          <Card key={row.scene_card_id} size="small" style={{ marginBottom: 8 }} title={`${row.card_name}（${cardStatusName(row.card_status)}）· ${row.used_shot_count || 0} 镜`} extra={
            <Space>
            {isLocked ? <Tag color="green">已锁定</Tag> : (realSceneCandidates.length > 0 ? <Tag color="orange">请点下方「选定」锁定</Tag> : null)}
            {row.ref_image_asset_id && isRealPhotoUrl(row.ref_image_url) ? (
              <Button
                size="small"
                disabled={busyIds.has(row.scene_card_id)}
                onClick={() => saveCardToLibrary('SCENE', row.scene_card_id, row.card_name, row.appearance_prompt, row.ref_image_asset_id)}
              >
                存入场景库
              </Button>
            ) : null}
            <Button size="small" loading={busyIds.has(row.scene_card_id)} disabled={busyIds.has(row.scene_card_id)} onClick={() => openGenerate('scene', row.scene_card_id, row.card_name, row.look_reference)}>生成参考图</Button>
            <Button size="small" disabled={busyIds.has(row.scene_card_id)} onClick={() => { ownImageTarget.current = { kind: 'scene', id: row.scene_card_id }; ownImageRef.current?.click() }}>上传参考图</Button>
            </Space>
          }>
            <LibraryLookPicker
              kind="SCENE"
              libraries={lookLibraries}
              librariesLoading={!lookLibrariesReady}
              currentReference={row.look_reference}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
            <Space wrap style={{ marginTop: 8 }}>
              {isRealPhotoUrl(row.ref_image_url) ? (
                <div style={{ textAlign: 'center' }}>
                  <ClickThumb alt="当前参考图" src={row.ref_image_url} width={96} height={64} />
                  <div style={{ fontSize: 12, color: '#595959' }}>当前</div>
                </div>
              ) : null}
              {realSceneCandidates.map((c) => (
                <div key={c.asset_id} style={{ textAlign: 'center' }}>
                  <ClickThumb alt="场景参考" src={c.file_url} width={96} height={64} />
                  <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                    const { data } = await productionApi.lockScene(row.scene_card_id, c.asset_id)
                    if (!data?.success) return
                    message.success('已锁定参考图')
                    load()
                  }}>{row.ref_image_asset_id === c.asset_id ? '已选定' : '选定'}</Button>
                </div>
              ))}
            </Space>
            <CardReference
              reference={row.look_reference}
              refImageUrl={row.ref_image_url}
              candidates={row.candidate_list}
              selectedAssetId={row.ref_image_asset_id}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
          </Card>
          )
        }) : null}
      </Card>
      ) : null}
      {!isScenes ? (
      <Card title="道具卡">
        <Form form={propForm} layout="inline" style={{ marginBottom: 12 }} onFinish={async (v) => {
          await productionApi.createScene({ work_id: work.work_id, card_type: 'PROP', ...v })
          propForm.resetFields()
          load()
        }}>
          <Form.Item name="card_name" rules={[{ required: true }]}><Input placeholder="道具名" /></Form.Item>
          <Form.Item name="appearance_prompt"><Input placeholder="外观描述" /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !props.length ? <Text type="secondary">暂无道具</Text> : null}
        {cardsReady ? props.map((row) => {
          const realPropCandidates = (row.candidate_list || []).filter((item) => isRealPhotoUrl(item.file_url))
          const isLocked = row.card_status === 'LOCKED'
          return (
          <Card key={row.scene_card_id} size="small" style={{ marginBottom: 8 }} title={`${row.card_name}（${cardStatusName(row.card_status)}）· ${row.used_shot_count || 0} 镜`} extra={
            <Space>
            {isLocked ? <Tag color="green">已锁定</Tag> : (realPropCandidates.length > 0 ? <Tag color="orange">请点下方「选定」锁定</Tag> : null)}
            {row.ref_image_asset_id && isRealPhotoUrl(row.ref_image_url) ? (
              <Button
                size="small"
                disabled={busyIds.has(row.scene_card_id)}
                onClick={() => saveCardToLibrary('PROP', row.scene_card_id, row.card_name, row.appearance_prompt, row.ref_image_asset_id)}
              >
                存入道具库
              </Button>
            ) : null}
            <Button size="small" loading={busyIds.has(row.scene_card_id)} disabled={busyIds.has(row.scene_card_id)} onClick={() => openGenerate('scene', row.scene_card_id, row.card_name, row.look_reference)}>生成参考图</Button>
            <Button size="small" disabled={busyIds.has(row.scene_card_id)} onClick={() => { ownImageTarget.current = { kind: 'scene', id: row.scene_card_id }; ownImageRef.current?.click() }}>上传参考图</Button>
            </Space>
          }>
            <LibraryLookPicker
              kind="PROP"
              libraries={lookLibraries}
              librariesLoading={!lookLibrariesReady}
              currentReference={row.look_reference}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
            <Space wrap style={{ marginTop: 8 }}>
              {isRealPhotoUrl(row.ref_image_url) ? (
                <div style={{ textAlign: 'center' }}>
                  <ClickThumb alt="当前参考图" src={row.ref_image_url} width={72} height={72} />
                  <div style={{ fontSize: 12, color: '#595959' }}>当前</div>
                </div>
              ) : null}
              {realPropCandidates.map((c) => (
                <div key={c.asset_id} style={{ textAlign: 'center' }}>
                  <ClickThumb alt="道具参考" src={c.file_url} width={72} height={72} />
                  <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                    const { data } = await productionApi.lockScene(row.scene_card_id, c.asset_id)
                    if (!data?.success) return
                    message.success('已锁定参考图')
                    load()
                  }}>{row.ref_image_asset_id === c.asset_id ? '已选定' : '选定'}</Button>
                </div>
              ))}
            </Space>
            <CardReference
              reference={row.look_reference}
              refImageUrl={row.ref_image_url}
              candidates={row.candidate_list}
              selectedAssetId={row.ref_image_asset_id}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
          </Card>
          )
        }) : null}
      </Card>
      ) : null}
      <Modal
        title={portrait ? `确认生成「${portrait.title || ''}」` : '确认生成'}
        open={!!portrait}
        okText="确认生成"
        cancelText="取消"
        okButtonProps={{ disabled: portrait ? busyIds.has(portrait.id) : false }}
        onCancel={() => setPortrait(null)}
        onOk={startPortrait}
      >
        <Space direction="vertical">
          <Select
            value={portraitCount}
            onChange={setPortraitCount}
            style={{ width: 160 }}
            options={[{ value: 2, label: '2 张' }, { value: 4, label: '4 张' }, { value: 8, label: '8 张' }]}
          />
          <ReferenceNotice reference={portrait && portrait.reference} textOnly={textOnly} />
          {portrait && portrait.reference && (portrait.reference.used === 1 || portrait.reference.used === true) ? (
            <Checkbox checked={textOnly} onChange={(event) => setTextOnly(event.target.checked)}>这次不用参考图，只按文字生成</Checkbox>
          ) : null}
          <span>{portraitPrice}</span>
        </Space>
      </Modal>
      <Modal
        title={angleRedoModal ? `${angleRedoModal.replace ? '重做' : '生成'}「${angleRedoModal.characterName || ''} · ${angleRedoModal.angleLabel}」` : '生成定妆'}
        open={!!angleRedoModal}
        okText={angleRedoModal?.replace ? '开始重做' : '开始生成'}
        cancelText="取消"
        okButtonProps={{ disabled: angleRedoModal ? busyIds.has(angleRedoModal.characterId) : false }}
        onCancel={() => setAngleRedoModal(null)}
        onOk={confirmAngleRedoModal}
      >
        {angleRedoModal ? (
          <Space direction="vertical" style={{ width: '100%' }} size="small">
            {angleRedoModal.hasRefPhoto ? (
              <div>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>重做方式</div>
                <Radio.Group
                  value={angleRedoModal.skipRef ? 'fresh' : 'ref'}
                  onChange={(e) => setAngleRedoModal((prev) => (prev ? { ...prev, skipRef: e.target.value === 'fresh' } : null))}
                >
                  <Space direction="vertical">
                    <Radio value="fresh">直接重做</Radio>
                    <Radio value="ref">参考已有定妆照</Radio>
                  </Space>
                </Radio.Group>
              </div>
            ) : null}
            <div>
              <div style={{ marginBottom: 6, fontWeight: 500 }}>外观特征（可选）</div>
              <Input
                value={angleRedoModal.caption}
                placeholder="外观特征描述"
                onChange={(e) => setAngleRedoModal((prev) => (prev ? { ...prev, caption: e.target.value } : null))}
              />
            </div>
            <Text type="secondary">
              {angleRedoModal.priceText}
            </Text>
          </Space>
        ) : null}
      </Modal>
    </Space>
  )
}

function modelLabel(row) { return modelPricingLabel(row) }

function StoryboardPane({ work, episodeId, currentEpisode, focusedStage, onReload, onFlowChange, onJumpTab }) {
  const [canvasView, setCanvasView] = useState(false)
  const [rows, setRows] = useState([])
  const [shotsLoaded, setShotsLoaded] = useState(false)
  const [loadedEpisodeId, setLoadedEpisodeId] = useState(null)
  const [splitting, setSplitting] = useState(false)
  const [actionError, setActionError] = useState(null)
  const [selected, setSelected] = useState([])
  const [batchExpanded, setBatchExpanded] = useState(false)
  const [expandedShotTools, setExpandedShotTools] = useState({})
  const [drafts, setDrafts] = useState({})
  const autoFilledEpisodeRef = useRef(null)
  const savingDraftsRef = useRef(new Map())
  const [imageModels, setImageModels] = useState([])
  const [videoModels, setVideoModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [videoModelCode, setVideoModelCode] = useState()
  const [panel, setPanel] = useState({ character_list: [], scene_list: [], prop_list: [] })
  const [panelReady, setPanelReady] = useState(false)
  const [bindChars, setBindChars] = useState([])
  const [bindScene, setBindScene] = useState()
  const [bindProps, setBindProps] = useState([])
  const [videoWatch, setVideoWatch] = useState(null)
  const [videoPreview, setVideoPreview] = useState(null)
  const [clipSeconds, setClipSeconds] = useState({})
  const [playbackKind, setPlaybackKind] = useState({})

  useEffect(() => {
    setSelected([])
    setActionError(null)
    setShotsLoaded(false)
    setLoadedEpisodeId(null)
    setBatchExpanded(false)
    setExpandedShotTools({})
    setDrafts({})
    autoFilledEpisodeRef.current = null
    savingDraftsRef.current = new Map()
  }, [episodeId])

  useEffect(() => {
    if (!focusedStage) return undefined
    const targetId = focusedStage === 'shots' ? 'storyboard-split-bar' : 'storyboard-media-card'
    const timer = window.setTimeout(() => {
      document.getElementById(targetId)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }, 60)
    return () => window.clearTimeout(timer)
  }, [focusedStage, rows.length])

  const load = useCallback(async () => {
    if (!episodeId) return
    try {
      const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 })
      setRows(Array.isArray(data?.result) ? data.result : [])
      setLoadedEpisodeId(episodeId)
      if (work?.work_id) {
        try {
          const panelRes = await productionApi.consistencyPanel(work.work_id)
          setPanel(panelRes.data?.result || { character_list: [], scene_list: [], prop_list: [] })
        } finally {
          setPanelReady(true)
        }
      } else {
        setPanelReady(true)
      }
      onFlowChange?.()
    } finally {
      setShotsLoaded(true)
    }
  }, [episodeId, work, onFlowChange])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    if (!shotsLoaded || loadedEpisodeId !== episodeId || !episodeId || !rows.length) return
    if (autoFilledEpisodeRef.current === episodeId) return
    if (!rows.some((row) => shotNeedsSpoken(row))) return
    autoFilledEpisodeRef.current = episodeId
    let cancelled = false
    featuresApi.previzGenerateText({ episode_id: episodeId })
      .then(() => { if (!cancelled) load() })
      .catch(() => {})
    return () => { cancelled = true }
  }, [shotsLoaded, loadedEpisodeId, episodeId, rows, load])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [imageRes, videoRes] = await Promise.all([
        productionApi.listAiModels('IMAGE'),
        productionApi.listAiModels('VIDEO'),
      ])
      if (cancelled) return
      const images = imageRes.data?.result?.model_list || []
      const videos = videoRes.data?.result?.model_list || []
      setImageModels(images)
      setVideoModels(videos)
      setImageModelCode((cur) => cur || images.find((m) => m.default_flag === 1)?.model_code || images[0]?.model_code)
      setVideoModelCode((cur) => cur || videos.find((m) => m.default_flag === 1)?.model_code || videos[0]?.model_code)
    })()
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!videoWatch?.batchId || videoWatch.done) return undefined
    let cancelled = false
    const started = Date.now()
    const pull = async () => {
      try {
        const { data } = await productionApi.batchProgress(videoWatch.batchId)
        const result = data?.result
        if (cancelled || !result) return
        const queued = Number(result.queued) || 0
        const running = Number(result.running) || 0
        const settled = queued + running === 0 && Number(result.total) > 0
        if (settled) {
          setVideoWatch((cur) => (cur && cur.batchId === videoWatch.batchId
            ? { ...cur, progress: result, done: true, lingering: false, error: '' } : cur))
          const failed = Number(result.failed) || 0
          if (failed > 0) {
            const reason = result.failed_task_list && result.failed_task_list[0] && result.failed_task_list[0].fail_reason
            message.error(friendlyFailReason(reason) || `有 ${failed} 个视频没有生成成功，可在任务中心恢复`)
          } else if (Number(result.succeeded) > 0) {
            message.success('视频已生成')
          }
          load()
          return
        }
        if (Date.now() - started > 20 * 60 * 1000) {
          setVideoWatch((cur) => (cur && cur.batchId === videoWatch.batchId
            ? { ...cur, progress: result, done: true, lingering: true, error: '' } : cur))
          return
        }
        setVideoWatch((cur) => (cur && cur.batchId === videoWatch.batchId
          ? { ...cur, progress: result, error: '' } : cur))
      } catch {
        if (!cancelled) {
          setVideoWatch((cur) => (cur && cur.batchId === videoWatch.batchId
            ? { ...cur, error: '进度暂时读不到' } : cur))
        }
      }
    }
    pull()
    const timer = setInterval(pull, 3000)
    const refreshShots = setInterval(() => { load().catch(() => {}) }, 15000)
    return () => {
      cancelled = true
      clearInterval(timer)
      clearInterval(refreshShots)
    }
  }, [videoWatch && videoWatch.batchId, videoWatch && videoWatch.done, load])

  const patchRow = (shotId, fields) => {
    setRows((cur) => cur.map((r) => (r.shot_id === shotId ? { ...r, ...fields } : r)))
  }

  const sceneNameByCardId = useMemo(() => {
    const map = new Map()
    for (const card of panel.scene_list || []) {
      if (card?.scene_card_id) map.set(card.scene_card_id, card.card_name || '')
    }
    return map
  }, [panel.scene_list])

  const spokenOfRow = (row, draftMap = drafts) => {
    const sceneCardName = sceneNameByCardId.get(row.scene_card_id) || ''
    const sceneKnown = knownSceneName({
      sceneName: sceneCardName,
      shotDesc: row.shot_desc,
      lookPlain: row.look_plain,
    })
    return reconcileEmptyScene(draftMap[row.shot_id] ?? row.previz_text ?? '', sceneKnown)
  }

  const isRowDirty = (row) => {
    if (!row?.shot_id || !Object.prototype.hasOwnProperty.call(drafts, row.shot_id)) return false
    return spokenOfRow(row) !== (row.previz_text ?? '')
  }

  const saveShotDraft = (row, { silent = true } = {}) => {
    if (!row?.shot_id) return Promise.resolve()
    const inFlight = savingDraftsRef.current.get(row.shot_id)
    if (!isRowDirty(row)) {
      if (Object.prototype.hasOwnProperty.call(drafts, row.shot_id)) {
        setDrafts((cur) => {
          if (!Object.prototype.hasOwnProperty.call(cur, row.shot_id)) return cur
          const next = { ...cur }
          delete next[row.shot_id]
          return next
        })
      }
      return inFlight ? inFlight.promise : Promise.resolve()
    }
    const text = spokenOfRow(row)
    if (inFlight && inFlight.text === text) return inFlight.promise
    const basePromise = inFlight ? inFlight.promise.catch(() => {}) : Promise.resolve()
    const entry = { text, promise: null }
    entry.promise = basePromise.then(async () => {
      try {
        const { data } = await featuresApi.previzSave({ shot_id: row.shot_id, previz_text: text })
        const saved = data?.result || {}
        const savedText = saved.previz_text || text
        patchRow(row.shot_id, {
          previz_text: savedText,
          look_plain: saved.look_plain,
          image_prompt: saved.image_prompt,
          prompt_score: saved.prompt_score,
        })
        setDrafts((cur) => {
          if (!Object.prototype.hasOwnProperty.call(cur, row.shot_id)) return cur
          if (spokenOfRow(row, cur) !== text) return cur
          const next = { ...cur }
          delete next[row.shot_id]
          return next
        })
        if (!silent) message.success('已保存')
      } finally {
        if (savingDraftsRef.current.get(row.shot_id) === entry) {
          savingDraftsRef.current.delete(row.shot_id)
        }
      }
    })
    savingDraftsRef.current.set(row.shot_id, entry)
    return entry.promise
  }

  const flushDirtyDrafts = async (targetShotIds) => {
    const idSet = Array.isArray(targetShotIds) && targetShotIds.length ? new Set(targetShotIds) : null
    const tasks = []
    for (const row of rows) {
      if (idSet && !idSet.has(row.shot_id)) continue
      const inFlight = savingDraftsRef.current.get(row.shot_id)
      if (isRowDirty(row)) {
        tasks.push(saveShotDraft(row, { silent: true }))
      } else if (inFlight) {
        tasks.push(inFlight.promise)
      }
    }
    if (tasks.length) await Promise.all(tasks)
  }

  const plan = useMemo(() => storyboardPrimaryPlan({ rows, selected }), [rows, selected])
  const selectedIds = selected.length ? selected : undefined

  const showActionError = (rawMessage) => {
    const msg = rawMessage || '操作未能提交，请检查前置条件后重试'
    setActionError({ message: msg, repair: resolveRepairAction(msg) })
  }

  const handleSplitShots = async () => {
    if (!episodeId || splitting) return
    setSplitting(true)
    setActionError(null)
    try {
      const splitRes = await productionApi.splitShots(episodeId)
      if (!splitRes?.data?.success) {
        const msg = splitRes?.data?.message || '请先完成本集剧本定稿，再拆解分镜'
        showActionError(msg)
        return
      }
      await featuresApi.previzGenerateText({ episode_id: episodeId })
      message.success('已拆解分镜')
      await load()
      await onReload?.()
    } catch (err) {
      showActionError(err?.response?.data?.message || err?.message || '拆解分镜失败，请稍后重试')
    } finally {
      setSplitting(false)
    }
  }

  const handleReadyAndSplitShots = async () => {
    if (!episodeId || splitting) return
    setSplitting(true)
    setActionError(null)
    try {
      const detailRes = await productionApi.getScript(episodeId)
      const scriptDetail = detailRes?.data?.result
      if (!scriptDetail?.scene_list?.length) {
        onJumpTab?.('script')
        message.warning('请先生成本集分场剧本后再定稿')
        return
      }
      const forceFlag = await confirmScriptReady(scriptDetail)
      if (forceFlag === null) return
      const readyRes = await productionApi.readyScript(episodeId, forceFlag)
      if (!readyRes?.data?.success) {
        onJumpTab?.('script')
        const readyMsg = readyRes?.data?.message || '请先生成本集分场剧本后再定稿'
        message.warning({ content: readyMsg, key: readyMsg })
        return
      }
      const splitRes = await productionApi.splitShots(episodeId)
      if (!splitRes?.data?.success) {
        showActionError(splitRes?.data?.message || '拆解分镜失败，请稍后重试')
        return
      }
      await featuresApi.previzGenerateText({ episode_id: episodeId })
      message.success('已定稿并拆解分镜')
      await load()
      await onReload?.()
    } catch (err) {
      onJumpTab?.('script')
      message.warning(err?.response?.data?.message || '请先生成本集分场剧本后再定稿')
    } finally {
      setSplitting(false)
    }
  }

  if (canvasView) {
    return (
      <StoryboardCanvas
        episodeId={episodeId}
        initialRows={rows}
        sceneCards={panel.scene_list || []}
        onSwitchToList={() => { setCanvasView(false); load() }}
      />
    )
  }

  const runOfficial = async (shotIds, { regenerate = false } = {}) => {
    setActionError(null)
    const body = { episode_id: episodeId, shot_ids: shotIds, model_code: videoModelCode }
    const outcome = await runOfficialShotVideo({
      regenerate,
      preview: async () => (await productionApi.previewOfficialVideo(body)).data,
      confirm: (preview, options) => confirmOfficialVideo(preview, options),
      submit: async () => (await productionApi.submitOfficialVideo({ ...body, confirm_cost: 1 })).data,
    })
    if (outcome.reason === 'preview_failed' || outcome.reason === 'submit_failed') {
      const msg = outcome.message || '视频任务提交失败，请检查前置条件'
      message.error(msg)
      showActionError(msg)
      return
    }
    if (outcome.reason === 'in_progress') {
      const msg = outcome.message || '这一镜视频已在生成中'
      message.warning(msg)
      if (/定妆|锁定|角色|场景|道具|占位图/.test(msg)) {
        showActionError(msg)
      }
      return
    }
    if (!outcome.submitted) return
    const batchId = outcome.result && outcome.result.batch_id
    const notes = ((outcome.result && outcome.result.shot_list) || []).map((shot) => shot.size_note).filter(Boolean)
    message.success(regenerate ? '已重新提交视频' : '视频已提交')
    if (notes[0]) message.info(notes[0])
    if (batchId) {
      setVideoWatch({ batchId, progress: null, done: false, lingering: false, error: '' })
    }
    load()
  }

  const submitLevel = async (taskType) => {
    setActionError(null)
    if (taskType === 'VIDEO') {
      const targetShotIds = plan.videoTargetIds
      if (!targetShotIds.length) {
        message.warning('当前没有可生成视频的镜头')
        return
      }
      await flushDirtyDrafts(targetShotIds)
      const hasAnyVideo = targetShotIds.some((id) => hasPlayableShotVideo(rows.find((r) => r.shot_id === id)))
      await runOfficial(targetShotIds, { regenerate: hasAnyVideo })
      return
    }
    const targetShotIds = plan.frameTargetIds
    if (!targetShotIds.length) {
      message.warning('当前没有待出首帧的镜头，可先勾选需要重出的镜头')
      return
    }
    await flushDirtyDrafts(targetShotIds)
    const estimateBody = {
      episode_id: episodeId,
      shot_ids: targetShotIds,
      task_type: taskType,
      auto_next_flag: 0,
      model_code: imageModelCode,
    }
    let estRes
    try {
      estRes = (await productionApi.estimateTasks(estimateBody)).data
    } catch (err) {
      showActionError(err?.response?.data?.message || err?.message || '费用预估失败，请检查角色与场景定妆是否已锁定')
      return
    }
    if (!estRes?.success) {
      showActionError(estRes?.message || '费用预估未通过，请先锁定引用的角色与场景参考图')
      return
    }
    const ok = await confirmEstimate(estRes.result, '确认出图费用')
    if (!ok) return
    const { data } = await productionApi.submitTasks(estimateBody)
    if (!data?.success) {
      showActionError(data?.message || '出图任务提交失败，请稍后重试')
      return
    }
    message.success('首帧已提交')
    await waitBatch(work.work_id, data.result.batch_id, data.result.task_id_list)
    load()
  }

  const imageModelLabel = modelLabel(imageModels.find((item) => item.model_code === imageModelCode) || {})
  const videoModelLabel = modelLabel(videoModels.find((item) => item.model_code === videoModelCode) || {})
  const imageSelect = modelSelectProps(imageModelLabel)
  const videoSelect = modelSelectProps(videoModelLabel)
  const scriptReady = currentEpisode?.script_status === 'READY'
  const canvas = canvasSwitchState(rows.length, { ready: shotsLoaded })
  const showCompactDoneBar = plan.compactDoneMode && !batchExpanded
  const hasAssetSelection = bindChars.length > 0 || Boolean(bindScene) || bindProps.length > 0

  const moreMenuItems = [
    { key: 'select-all', label: `全选本集（${rows.length}）`, disabled: !rows.length },
    { key: 'select-pending-video', label: `勾选待出视频（${plan.pendingVideoIds.length}）`, disabled: !plan.pendingVideoIds.length },
    { type: 'divider' },
    { key: 'frame-link-batch', label: '同场画面自动衔接', disabled: !rows.length },
    { key: 'rewrite-previz', label: '重写画面描述', disabled: !rows.length },
    { key: 'resplit', label: '重新拆镜', danger: true },
  ]

  const handleMoreMenu = async ({ key }) => {
    if (key === 'select-all') {
      setSelected(rows.map((r) => r.shot_id))
      return
    }
    if (key === 'select-pending-video') {
      setSelected(plan.pendingVideoIds)
      return
    }
    if (key === 'frame-link-batch') {
      const { data } = await featuresApi.frameLinkBatch({
        episode_id: episodeId,
        frame_link_mode: 'INHERIT_PREV',
      })
      if (data?.success) {
        message.success(`已衔接 ${data.result.linked_count} 处`)
        load()
      }
      return
    }
    if (key === 'rewrite-previz') {
      Modal.confirm({
        title: '确认重写画面描述',
        content: selected.length
          ? `将根据剧本与设定重写已选 ${selected.length} 个镜头的画面描述，并覆盖当前文字修改。`
          : '将根据剧本与设定重写本集全部镜头的画面描述，并覆盖当前文字修改。',
        okText: '确认重写',
        cancelText: '取消',
        onOk: async () => {
          await featuresApi.previzGenerateText({ episode_id: episodeId, shot_id_list: selectedIds })
          setDrafts({})
          message.success('已重写画面描述')
          load()
        },
      })
      return
    }
    if (key === 'resplit') {
      Modal.confirm({
        title: '确认重新拆镜',
        content: '将按当前定稿剧本重新拆分未锁定镜头，已锁定镜头会保留。',
        okText: '确认重新拆镜',
        cancelText: '取消',
        onOk: handleSplitShots,
      })
    }
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      {actionError ? (
        <Alert
          type="error"
          showIcon
          closable
          onClose={() => setActionError(null)}
          message="当前操作未能继续"
          description={actionError.message}
          action={actionError.repair ? (
            actionError.repair.tab ? (
              <Button size="small" type="primary" onClick={() => onJumpTab?.(actionError.repair.tab)}>
                {actionError.repair.label}
              </Button>
            ) : (
              <Link to={actionError.repair.href}>
                <Button size="small" type="primary">{actionError.repair.label}</Button>
              </Link>
            )
          ) : null}
        />
      ) : null}
      {videoWatch ? (
        <Alert
          type={videoWatchCopy(videoWatch).type}
          showIcon
          message={videoWatchCopy(videoWatch).message}
          description={(
            <span>
              {videoWatchCopy(videoWatch).description}
              {' '}
              <Link to="/tasks">打开任务中心</Link>
            </span>
          )}
        />
      ) : null}
      <Card id="storyboard-media-card" size="small">
        <div
          id="storyboard-split-bar"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}
        >
          <Space wrap align="center">
            <Tooltip title={canvas.disabled ? canvas.reason : ''}>
              <span>
                <Segmented
                  size="small"
                  value="list"
                  onChange={(val) => {
                    if (val === 'canvas' && !canvas.disabled) setCanvasView(true)
                  }}
                  options={[
                    { label: '上下列表', value: 'list' },
                    { label: '按场次排', value: 'canvas', disabled: canvas.disabled },
                  ]}
                />
              </span>
            </Tooltip>
            {rows.length > 0 ? <Text type="secondary">{plan.progressText}</Text> : null}
            {plan.blockedCount > 0 ? (
              <Button type="link" size="small" style={{ paddingInline: 0 }} onClick={() => onJumpTab?.('characters')}>
                待定妆 {plan.blockedCount} 镜 · 去锁定
              </Button>
            ) : null}
          </Space>
          {rows.length > 0 ? (
            <Space wrap align="center">
              {showCompactDoneBar ? (
                <>
                  <Button size="small" type="primary" onClick={() => onJumpTab?.('compose')}>去合成成片</Button>
                  <Button size="small" onClick={() => onJumpTab?.('voice')}>配音字幕</Button>
                  <Button size="small" onClick={() => setBatchExpanded(true)}>批量重做</Button>
                </>
              ) : (
                <>
                  {selected.length > 0 ? (
                    <Button size="small" onClick={() => setSelected([])}>取消勾选（{selected.length}）</Button>
                  ) : (plan.allVideosReady && batchExpanded ? (
                    <Button size="small" onClick={() => setBatchExpanded(false)}>收起</Button>
                  ) : null)}
                  <Button size="small" disabled={!plan.frameTargetIds.length} onClick={() => submitLevel('IMAGE')}>
                    {plan.frameLabel}
                  </Button>
                  <Button size="small" type="primary" disabled={!plan.videoTargetIds.length} onClick={() => submitLevel('VIDEO')}>
                    {plan.videoLabel}
                  </Button>
                </>
              )}
              <Dropdown menu={{ items: moreMenuItems, onClick: handleMoreMenu }} trigger={['click']}>
                <Button size="small">更多操作</Button>
              </Dropdown>
            </Space>
          ) : null}
        </div>
      </Card>
      {!shotsLoaded ? <Card loading style={{ minHeight: 140 }} /> : null}
      {shotsLoaded && rows.length === 0 ? (
        <Card>
          <Title level={5} style={{ marginTop: 0 }}>
            {scriptReady ? '本集暂无分镜' : '本集暂无分镜（剧本待定稿）'}
          </Title>
          <Space wrap>
            {scriptReady ? (
              <Button type="primary" loading={splitting} onClick={handleSplitShots}>从剧本拆解分镜</Button>
            ) : (
              <>
                <Button type="primary" loading={splitting} onClick={handleReadyAndSplitShots}>定稿并拆解分镜</Button>
                <Button onClick={() => onJumpTab?.('script')}>去剧本定稿</Button>
              </>
            )}
            <Button onClick={() => onJumpTab?.('characters')}>查看角色定妆</Button>
          </Space>
        </Card>
      ) : null}
      {shotsLoaded ? (
        <Collapse
          defaultActiveKey={[]}
          items={[{
            key: 'advanced-tools',
            label: '更多工具',
            children: (
              <Space direction="vertical" style={{ width: '100%' }} size="middle">
                <Card size="small" title="生成模型">
                  <Space wrap>
                    <Tooltip title={imageSelect.title || '出图模型'}>
                      <span>
                        <Select
                          aria-label="出图模型"
                          placeholder="出图模型"
                          value={imageModelCode}
                          onChange={setImageModelCode}
                          title={imageSelect.title}
                          popupMatchSelectWidth={imageSelect.popupMatchSelectWidth}
                          style={imageSelect.style}
                          dropdownStyle={imageSelect.dropdownStyle}
                          options={imageModels.map((m) => ({
                            value: m.model_code,
                            label: modelPricingCompactLabel(m),
                            title: modelPricingLabel(m),
                          }))}
                        />
                      </span>
                    </Tooltip>
                    <Tooltip title={videoSelect.title || '视频模型'}>
                      <span>
                        <Select
                          aria-label="视频模型"
                          placeholder="视频模型"
                          value={videoModelCode}
                          onChange={setVideoModelCode}
                          title={videoSelect.title}
                          popupMatchSelectWidth={videoSelect.popupMatchSelectWidth}
                          style={videoSelect.style}
                          dropdownStyle={videoSelect.dropdownStyle}
                          options={videoModels.map((m) => ({
                            value: m.model_code,
                            label: modelPricingCompactLabel(m),
                            title: modelPricingLabel(m),
                          }))}
                        />
                      </span>
                    </Tooltip>
                  </Space>
                </Card>
                <Card
                  size="small"
                  title="修改勾选镜头的角色与场景"
                  extra={<Text type="secondary">{selected.length ? `已选 ${selected.length} 镜` : '请先在下方勾选镜头'}</Text>}
                >
                  <Space wrap>
                    <Select
                      mode="multiple"
                      allowClear
                      {...bindSelectProps()}
                      placeholder={assetBinderPlaceholder('角色', (panel.character_list || []).length, panelReady)}
                      style={{ minWidth: 180 }}
                      value={bindChars}
                      onChange={setBindChars}
                      options={(panel.character_list || []).map((c) => ({
                        value: c.character_id,
                        label: bindOptionLabel(c.character_name, cardStatusName(c.card_status)),
                      }))}
                    />
                    <Select
                      allowClear
                      {...bindSelectProps()}
                      placeholder={assetBinderPlaceholder('场景', (panel.scene_list || []).length, panelReady)}
                      style={{ minWidth: 160 }}
                      value={bindScene}
                      onChange={setBindScene}
                      options={(panel.scene_list || []).map((c) => ({
                        value: c.scene_card_id,
                        label: bindOptionLabel(c.card_name, cardStatusName(c.card_status)),
                      }))}
                    />
                    <Select
                      mode="multiple"
                      allowClear
                      {...bindSelectProps()}
                      placeholder={assetBinderPlaceholder('道具', (panel.prop_list || []).length, panelReady)}
                      style={{ minWidth: 160 }}
                      value={bindProps}
                      onChange={setBindProps}
                      options={(panel.prop_list || []).map((c) => ({
                        value: c.scene_card_id,
                        label: bindOptionLabel(c.card_name, cardStatusName(c.card_status)),
                      }))}
                    />
                    <Button
                      type="primary"
                      disabled={!selected.length || !hasAssetSelection}
                      onClick={async () => {
                        for (const shotId of selected) {
                          const body = buildAssetBindPayload({ shotId, bindChars, bindScene, bindProps })
                          await productionApi.bindShotAssets(body)
                        }
                        message.success(`已应用到 ${selected.length} 镜`)
                        setBindChars([])
                        setBindScene(undefined)
                        setBindProps([])
                        load()
                      }}
                    >应用到已选镜头</Button>
                  </Space>
                  <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                    {assetLibrarySummary({
                      ready: panelReady,
                      characters: (panel.character_list || []).map((item) => item.character_name),
                      scenes: (panel.scene_list || []).map((item) => item.card_name),
                      props: (panel.prop_list || []).map((item) => item.card_name),
                    })}
                  </Paragraph>
                </Card>
                <ImageToVideoPanel workId={work?.work_id} episodeId={episodeId} shots={rows} />
              </Space>
            ),
          }]}
        />
      ) : null}
      {rows.map((row) => {
        const readiness = evaluateShotReadiness(row, panel)
        const taskBadge = shotLatestTaskBadge(row)
        const playback = playbackKind[row.shot_id]
        const showTaskBadge = taskBadge && playback !== 'broken'
        const sceneCardName = sceneNameByCardId.get(row.scene_card_id) || ''
        const sceneKnown = knownSceneName({
          sceneName: sceneCardName,
          shotDesc: row.shot_desc,
          lookPlain: row.look_plain,
        })
        const spoken = spokenOfRow(row)
        const dirty = isRowDirty(row)
        const isSelected = selected.includes(row.shot_id)
        const hasFrame = hasRealFirstFrame(row)
        const canRegenVideo = shotOffersVideoRegenerate(row, playback)
        const dialogue = shotDialogueLine(row)
        const toolsExpanded = Boolean(expandedShotTools[row.shot_id])
        return (
        <Card
          key={row.shot_id}
          className="shot-card"
          style={shotCardContainment()}
          size="small"
          title={shotCardHeading(row, { actualSec: clipSeconds[row.shot_id], playback })}
          extra={<Space>
            {showTaskBadge ? (
              <Tag color={taskBadge.color}>{taskBadge.text}</Tag>
            ) : null}
            {!readiness.ready ? <Tag>待定妆</Tag> : null}
            {row.stale_flag === 1 ? <Tag color="orange">定妆已变，建议重做</Tag> : null}
            {row.blocked_flag === 1 ? <Tag color="red">待定妆解锁</Tag> : null}
            <Checkbox
              checked={isSelected}
              onChange={(e) => {
                setSelected((cur) => (
                  e.target.checked
                    ? (cur.includes(row.shot_id) ? cur : cur.concat(row.shot_id))
                    : cur.filter((id) => id !== row.shot_id)
                ))
              }}
            >选择</Checkbox>
            {row.shot_status === 'LOCKED'
              ? <Button size="small" onClick={async () => { await productionApi.unlockShot(row.shot_id); load() }}>解锁</Button>
              : <Button size="small" onClick={async () => { await productionApi.lockShot(row.shot_id); load() }}>锁定</Button>}
          </Space>}
        >
          <Space align="start" size="large" style={{ width: '100%' }}>
            <ShotMediaColumn
              row={row}
              onRegenerate={canRegenVideo ? async () => {
                await flushDirtyDrafts([row.shot_id])
                await runOfficial([row.shot_id], { regenerate: true })
              } : null}
              onEnlarge={setVideoPreview}
              onDuration={(seconds) => setClipSeconds((cur) => (
                cur[row.shot_id] === seconds ? cur : { ...cur, [row.shot_id]: seconds }
              ))}
              onPlayback={(kind) => setPlaybackKind((cur) => (
                cur[row.shot_id] === kind ? cur : { ...cur, [row.shot_id]: kind }
              ))}
            />
            <div style={{ flex: 1, minWidth: 0 }}>
              {Number(row.blocked_flag) === 1 ? (
                <Alert
                  type="warning"
                  showIcon
                  style={{ marginBottom: 8 }}
                  message={row.block_reason || '该镜头引用的角色、场景或道具尚未锁定参考图，暂时无法出图和生成视频'}
                  action={(
                    <Button size="small" type="primary" ghost onClick={() => onJumpTab?.('characters')}>
                      去角色定妆锁定
                    </Button>
                  )}
                />
              ) : null}
              <Space wrap size={[4, 4]} style={{ marginBottom: 8 }}>
                {(row.character_ref_json || []).map((id) => {
                  const card = (panel.character_list || []).find((c) => c.character_id === id)
                  const label = bindOptionLabel(card?.character_name, '')
                  if (!card?.character_name || label === '未命名') return null
                  return <Tag key={id}>{card.character_name}</Tag>
                })}
                {sceneCardName ? (
                  <Tag color="blue">{sceneCardName}</Tag>
                ) : null}
                {(row.prop_ref_json || []).map((id) => {
                  const card = (panel.prop_list || []).find((c) => c.scene_card_id === id)
                  if (!card?.card_name) return null
                  return <Tag key={id} color="gold">{card.card_name}</Tag>
                })}
                {row.link_effective_flag === 1 ? (
                  <Tag color="blue">接着上一镜拍</Tag>
                ) : (row.link_hint ? <Tag color="orange">{row.link_hint}</Tag> : null)}
              </Space>
              {dialogue ? (
                <Paragraph type="secondary" style={{ marginBottom: 6 }}>台词：{dialogue}</Paragraph>
              ) : null}
              <Text type="secondary">画面内容描述</Text>
              <TextArea
                className="shot-spoken"
                rows={3}
                style={{ marginTop: 6 }}
                value={spoken}
                onChange={(e) => setDrafts((cur) => ({ ...cur, [row.shot_id]: e.target.value }))}
                onBlur={() => { saveShotDraft(row, { silent: true }) }}
              />
              {row.reference_note ? (
                <Paragraph type="secondary" style={{ marginTop: 6, marginBottom: 0 }}>{row.reference_note}</Paragraph>
              ) : null}
              <Space className="shot-actions" style={{ marginTop: 8 }} wrap>
                {dirty ? (
                  <Button size="small" onClick={() => saveShotDraft(row, { silent: false })}>保存修改</Button>
                ) : null}
                <Button size="small" disabled={row.shot_status === 'LOCKED'} onClick={async () => {
                  setActionError(null)
                  await flushDirtyDrafts([row.shot_id])
                  const body = {
                    episode_id: episodeId,
                    shot_ids: [row.shot_id],
                    task_type: 'IMAGE',
                    auto_next_flag: 0,
                    model_code: imageModelCode,
                  }
                  const { data: estRes } = await productionApi.estimateTasks(body)
                  if (!estRes?.success) {
                    showActionError(estRes?.message || '这一镜存在未锁定的角色或场景参考图，请先完成定妆锁定')
                    return
                  }
                  const ok = await confirmEstimate(estRes.result, '确认出图费用')
                  if (!ok) return
                  const { data } = await productionApi.submitTasks(body)
                  if (!data?.success) {
                    showActionError(data?.message || '出图任务提交失败，请稍后重试')
                    return
                  }
                  message.success('出图已提交')
                  await waitTask(work.work_id, data.result.task_id_list?.[0])
                  load()
                }}>{hasFrame ? '重出首帧' : '生成首帧'}</Button>
                <Button
                  size="small"
                  type="primary"
                  disabled={!canRegenVideo && row.shot_status === 'LOCKED'}
                  onClick={async () => {
                    await flushDirtyDrafts([row.shot_id])
                    await runOfficial([row.shot_id], { regenerate: canRegenVideo })
                  }}
                >{canRegenVideo ? '重新生成视频' : '生成视频'}</Button>
                <Button
                  size="small"
                  onClick={() => setExpandedShotTools((cur) => ({ ...cur, [row.shot_id]: !cur[row.shot_id] }))}
                >{toolsExpanded ? '收起微调' : '镜头微调'}</Button>
              </Space>
              {toolsExpanded ? (
                <div style={{ marginTop: 10, paddingTop: 10, borderTop: '1px solid #eef2f7' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>和上一镜</Text>
                    <Select
                      size="small"
                      style={{ width: 120 }}
                      value={row.frame_link_mode || 'NONE'}
                      aria-label="和上一镜"
                      onChange={async (mode) => {
                        await featuresApi.frameLinkSave({
                          shot_id: row.shot_id,
                          frame_link_mode: mode,
                          model_code: videoModelCode,
                        })
                        load()
                      }}
                      options={frameLinkOptions()}
                    />
                  </div>
                  <div style={{ marginBottom: 8 }}>
                    {shotPresetGroups().map((group) => (
                      <div key={group.key} style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center', marginBottom: 6 }}>
                        <Text type="secondary" style={{ fontSize: 12, minWidth: 64 }}>{group.label}</Text>
                        {group.items.map((item) => {
                          const active = presetIsActive(spoken, item.marker)
                          return (
                            <Button
                              key={item.key}
                              size="small"
                              type={active ? 'primary' : 'default'}
                              onClick={() => {
                                setDrafts((cur) => {
                                  const old = reconcileEmptyScene(cur[row.shot_id] ?? row.previz_text ?? '', sceneKnown)
                                  return { ...cur, [row.shot_id]: appendPreset(old, item.marker) }
                                })
                              }}
                            >
                              {item.label}
                            </Button>
                          )
                        })}
                      </div>
                    ))}
                  </div>
                  {row.image_prompt ? (
                    <div style={{ marginBottom: 8 }}>
                      <Text type="secondary">画面提示词</Text>
                      <div>{localizePromptShotSize(row.image_prompt)}</div>
                      <Space wrap size={4}>
                        {(row.prompt_segments || []).map((seg) => (
                          <Tag key={seg.source}>{seg.label}</Tag>
                        ))}
                      </Space>
                    </div>
                  ) : null}
                  <Button size="small" onClick={async () => {
                    await flushDirtyDrafts([row.shot_id])
                    const { data } = await featuresApi.previzPromptCheck({ shot_id: row.shot_id })
                    const check = data?.result || {}
                    patchRow(row.shot_id, {
                      look_plain: check.look_plain,
                      prompt_score: check.prompt_score,
                      prompt_issues: check.issue_list,
                    })
                  }}>检查画面描述</Button>
                  {(row.prompt_issues || []).length ? (
                    <div style={{ marginTop: 8 }}>
                      {(row.prompt_issues || []).slice(0, 3).map((it, i) => (
                        <Paragraph key={i} type="secondary" style={{ marginBottom: 2 }}>
                          {it.issue_desc}{it.suggestion ? ` → ${it.suggestion}` : ''}
                        </Paragraph>
                      ))}
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
          </Space>
        </Card>
        )
      })}
      <Modal
        title={videoPreview ? `镜 ${videoPreview.shot_no} 视频` : '视频'}
        open={!!videoPreview}
        onCancel={() => setVideoPreview(null)}
        {...shotVideoDialogProps()}
      >
        {videoPreview && isPlaceholderMediaUrl(videoPreview.video_url) ? (
          <div>
            <img alt="" src={videoPreview.video_url} style={{ maxWidth: '100%', maxHeight: 360 }} />
            <Paragraph>这是占位图，不是能播放的视频。</Paragraph>
          </div>
        ) : videoPreview && videoPreview.video_url ? (
          <ListShotVideo
            src={videoPreview.video_url}
            label="播放或暂停"
            autoPlay
            onPlayback={(kind) => setPlaybackKind((cur) => (
              cur[videoPreview.shot_id] === kind ? cur : { ...cur, [videoPreview.shot_id]: kind }
            ))}
            onDuration={(seconds) => setClipSeconds((cur) => (
              cur[videoPreview.shot_id] === seconds ? cur : { ...cur, [videoPreview.shot_id]: seconds }
            ))}
          />
        ) : videoPreview ? <Paragraph>这一镜还没有视频。</Paragraph> : null}
      </Modal>
    </Space>
  )
}

function TasksPane({ workId, episodeId }) {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [settled, setSettled] = useState(false)
  const [taskType, setTaskType] = useState()
  const [taskStatus, setTaskStatus] = useState()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      // Work-level tasks (OUTLINE etc.) have null episode_id; do not filter by episode
      // or they never appear in 「本剧任务」.
      const { data } = await productionApi.listTasks({
        work_id: workId,
        task_type: taskType || undefined,
        task_status: taskStatus || undefined,
        page_num: 1,
        page_size: 50,
      })
      setRows(taskRowsFromResult(data?.result))
    } finally {
      setLoading(false)
      setSettled(true)
    }
  }, [workId, episodeId, taskType, taskStatus])

  useEffect(() => {
    load()
    const timer = setInterval(load, 2500)
    return () => clearInterval(timer)
  }, [load])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Space wrap>
        <Select allowClear placeholder="类型" style={{ width: 120 }} value={taskType} onChange={setTaskType}
          options={[{ value: 'TEXT', label: '文本' }, { value: 'IMAGE', label: '图片' }, { value: 'VIDEO', label: '视频' }, { value: 'VOICE', label: '配音' }, { value: 'COMPOSE', label: '合成' }]} />
        <Select allowClear placeholder="状态" style={{ width: 120 }} value={taskStatus} onChange={setTaskStatus}
          options={[{ value: 'QUEUED', label: '排队中' }, { value: 'RUNNING', label: '执行中' }, { value: 'SUCCEEDED', label: '已成功' }, { value: 'FAILED', label: '已失败' }, { value: 'CANCELED', label: '已取消' }]} />
        <Button onClick={load}>刷新</Button>
      </Space>
      <Table
        rowKey="task_id"
        size="small"
        loading={loading || !settled}
        locale={{ emptyText: settled ? '这一部还没有任务记录' : ' ' }}
        dataSource={rows}
        pagination={false}
        columns={[
          { title: '类型', dataIndex: 'task_type_name', width: 90, render: (v, row) => v || row.task_type },
          { title: '目标', dataIndex: 'target_type_name', width: 110, render: (v, row) => v || labelOf(TARGET_TYPE_LABEL, row.target_type, row.target_type || '-') },
          { title: '状态', dataIndex: 'task_status_name', width: 110, render: (v, row) => <Tag>{v || row.task_status}</Tag> },
          { title: '进度', dataIndex: 'progress_percent', width: 280, render: (_, row) => row.progress_hint || row.progress_percent },
          { title: '费用(元)', dataIndex: 'cost_cent', width: 90, render: (v) => ((Number(v) || 0) / 100).toFixed(2) },
          { title: '失败', key: 'fail_reason', render: (_, row) => visibleFailReason(row) || '—' },
          {
            title: '操作',
            width: 180,
            render: (_, row) => (
              <Space>
                {row.recoverable_flag === 1 || row.task_status === 'FAILED' || row.task_status === 'CANCELED'
                  ? <Button size="small" onClick={async () => { await productionApi.recoverTask(row.task_id); load() }}>恢复</Button>
                  : null}
                {row.cancellable_flag === 1 || row.task_status === 'QUEUED' || row.task_status === 'RUNNING'
                  ? <Button size="small" onClick={async () => { await productionApi.cancelTask(row.task_id); load() }}>取消</Button>
                  : null}
              </Space>
            ),
          },
        ]}
      />
    </Space>
  )
}

function formatPublishError(data, fallback) {
  const code = data?.code != null ? String(data.code) : ''
  const msg = friendlyFailReason(data?.message || fallback || '发布失败')
  if (code === '4006' || /READY|APPROVED|过期|未就绪|审核/.test(msg)) {
    return { title: '无法发布（闸门未通过）', detail: msg }
  }
  if (code === '502000' || /不可达|不可用|连接失败|Tube 服务/.test(msg)) {
    return { title: 'Tube 服务不可用', detail: msg }
  }
  if (code === '504000' || /超时/.test(msg)) {
    return { title: 'Tube 请求超时', detail: msg }
  }
  if (/6\d{4}/.test(msg) || /^6\d{4}$/.test(code)) {
    return { title: 'Tube 业务拒绝', detail: msg }
  }
  if (/未配置|base-url|ingest-token/.test(msg)) {
    return { title: 'Tube 配置缺失', detail: msg }
  }
  return { title: '发布失败', detail: msg }
}

function showPublishSuccess(result) {
  const mediaId = result?.media_id || ''
  const playUrl = result?.play_url || ''
  const replayed = !!result?.replayed
  if (result?.status === 'UNKNOWN') {
    Modal.warning({
      title: '发布结果未知',
      width: 520,
      content: (
        <div>
          <Paragraph>Tube 没有给出可确认的结果。这不是发布成功，也不是失败。同一成片版本不会另建作品。</Paragraph>
          <Paragraph type="secondary">作品号 {mediaId || '尚未回写'}。可稍后用同一来源再查一次。</Paragraph>
        </div>
      ),
    })
    return
  }
  const pending = result?.status === 'PENDING' || result?.commercial_ready === false
  if (pending && result?.status !== 'READY') {
    const note = result?.publish_log?.error || 'Tube 已受理，素材尚未验证为可播放。这不是发布成功。'
    Modal.info({
      title: 'Tube 已受理，尚未可播放',
      width: 520,
      content: (
        <div>
          <Paragraph>{note}</Paragraph>
          <Paragraph type="secondary">状态 {result?.status || 'PENDING'}。可稍后重试，同一幂等键不会新建目录。</Paragraph>
        </div>
      ),
    })
    return
  }
  Modal.success({
    title: replayed ? '已发布到 Tube（幂等回放，未新建目录）' : '已发布到 Tube',
    width: 520,
    content: (
      <div>
        <Paragraph style={{ marginBottom: 8 }}>
          <Text type="secondary">作品号</Text>
          <br />
          <Text copyable={{ text: mediaId }}>{mediaId || '-'}</Text>
        </Paragraph>
        <Paragraph style={{ marginBottom: 12 }}>
          <Text type="secondary">播放链接</Text>
          <br />
          {playUrl ? (
            <Text copyable={{ text: playUrl }}>
              <a href={playUrl} target="_blank" rel="noreferrer">{playUrl}</a>
            </Text>
          ) : (
            <Text>-</Text>
          )}
        </Paragraph>
        <Space>
          {playUrl ? (
            <Button type="primary" onClick={() => window.open(playUrl, '_blank', 'noopener,noreferrer')}>
              打开播放页
            </Button>
          ) : null}
          {mediaId ? (
            <Button onClick={async () => {
              try {
                await navigator.clipboard.writeText(mediaId)
                message.success('已复制作品号')
              } catch {
                message.warning('复制失败，请手动选择')
              }
            }}>复制作品号</Button>
          ) : null}
          {playUrl ? (
            <Button onClick={async () => {
              try {
                await navigator.clipboard.writeText(playUrl)
                message.success('已复制播放链接')
              } catch {
                message.warning('复制失败，请手动选择')
              }
            }}>复制播放链接</Button>
          ) : null}
        </Space>
      </div>
    ),
  })
}

const LOADING_ENTRY = { phase: 'loading', film: null, precheck: null }

function ComposePane({ work, episodeId, onReload, onFlowChange, onJumpTab, composeEntry, onComposeEntry, mediaPreflight, onMediaPreflight }) {
  const [composing, setComposing] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [batchPublishing, setBatchPublishing] = useState(false)
  const [batchProgress, setBatchProgress] = useState(null)
  const [publishError, setPublishError] = useState(null)
  const [composeCost, setComposeCost] = useState(null)
  const [musicModels, setMusicModels] = useState([])
  const [musicModelCode, setMusicModelCode] = useState()
  const [musicPrompt, setMusicPrompt] = useState('电影感纯音乐，情绪递进，适合作为短剧背景音乐')
  const requestSeq = useRef(0)
  const entry = composeEntry || LOADING_ENTRY
  const film = entry.film
  const precheck = entry.precheck
  const statusSettled = entry.phase === 'settled'
  const surface = composeSurfaceFromEntry(entry)

  const load = useCallback(async () => {
    if (!episodeId) return
    const seq = requestSeq.current + 1
    requestSeq.current = seq
    const requestedId = episodeId
    onComposeEntry(requestedId, (prev) => ({
      phase: 'loading',
      film: prev?.film ?? null,
      precheck: prev?.precheck ?? null,
    }))
    try {
      const filmPromise = productionApi.filmDetail(requestedId).then((f) => {
        if (seq !== requestSeq.current) return null
        const nextFilm = f.data?.result ?? null
        onComposeEntry(requestedId, (prev) => ({
          phase: 'loading',
          film: nextFilm,
          precheck: prev?.precheck ?? null,
        }))
        return nextFilm
      })
      const [p, nextFilm, mp] = await Promise.all([
        productionApi.filmPrecheck(requestedId),
        filmPromise,
        productionApi.publishFilmToMediaPreflight({ episode_id: requestedId, work_id: work?.work_id }),
      ])
      if (seq !== requestSeq.current) return
      const nextPrecheck = p.data?.result ?? null
      onComposeEntry(requestedId, {
        phase: 'settled',
        film: nextFilm,
        precheck: nextPrecheck,
      })
      onMediaPreflight(mp.data?.result || null)
      onFlowChange?.()
    } catch {
      if (seq !== requestSeq.current) return
      onComposeEntry(requestedId, { phase: 'error', film: null, precheck: null })
    }
  }, [episodeId, onComposeEntry, onFlowChange, onMediaPreflight, work?.work_id])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const { data } = await productionApi.listAiModels('MUSIC')
      if (cancelled) return
      const models = data?.result?.model_list || []
      setMusicModels(models)
      setMusicModelCode(models.find((m) => m.default_flag === 1)?.model_code || models[0]?.model_code)
    })()
    return () => { cancelled = true }
  }, [])

  const publishLog = film?.publish_log
    || mediaPreflight?.publish_log
    || (film?.media_id || film?.media_last_error || (film?.media_publish_status && film.media_publish_status !== 'NONE')
      ? {
        media_id: film.media_id,
        play_url: film.media_play_url,
        status: film.media_publish_status,
        published_at: film.media_published_at,
        last_error: film.media_last_error,
      }
      : null)

  const clientPreflight = validateTubePublishPreflight(film, {
    review_status: mediaPreflight?.review_status || 'APPROVED',
  })

  const tokenMissing = tubeTokenMissingMessage(mediaPreflight)
  const publishHints = statusSettled ? composePublishHints(mediaPreflight, film) : []
  const canPublish = statusSettled
    && !!film?.film_id
    && clientPreflight.ready
    && mediaPreflight?.config_ready !== false
    && !tokenMissing
    && !publishing

  const runPublish = async () => {
    if (!film?.film_id) return
    setPublishing(true)
    setPublishError(null)
    try {
      const { data } = await productionApi.publishFilmToMedia({
        episode_id: episodeId,
        film_id: film.film_id,
      })
      if (data?.success) {
        showPublishSuccess(data.result || {})
        await load()
        await onReload()
      } else {
        setPublishError(formatPublishError(data))
      }
    } catch (err) {
      const data = err?.response?.data
      setPublishError(formatPublishError(data, err?.message || '网络异常'))
    } finally {
      setPublishing(false)
    }
  }

  const runBatchPublish = async () => {
    if (!work?.work_id) return
    const count = mediaPreflight?.batch_ready_count || 0
    const ok = await new Promise((resolve) => {
      Modal.confirm({
        title: '批量发布到 Tube',
        content: `将发布本作品全部已就绪且已通过上架审核的成片（约 ${count} 集）。确认继续？`,
        okText: '开始批量发布',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
    if (!ok) return
    setBatchPublishing(true)
    setBatchProgress({ progress_percent: 0, total: count, succeeded: 0, failed: 0 })
    setPublishError(null)
    try {
      const { data } = await productionApi.publishFilmToMediaBatch({ work_id: work.work_id })
      if (data?.success) {
        const r = data.result || {}
        setBatchProgress(r)
        message.success(`批量完成：可播放 ${r.succeeded || 0} / 已受理未就绪 ${r.pending || 0} / 失败 ${r.failed || 0} / 共 ${r.total || 0}`)
        await load()
        await onReload()
      } else {
        setPublishError(formatPublishError(data))
      }
    } catch (err) {
      const data = err?.response?.data
      setPublishError(formatPublishError(data, err?.message || '网络异常'))
    } finally {
      setBatchPublishing(false)
    }
  }

  return (
    <>
      <Card title="合成与下载">
        <Paragraph style={{ marginBottom: 4 }}>
          <Text strong>{surface.headline}</Text>
        </Paragraph>
        <Paragraph type="secondary" style={{ fontSize: 13 }}>{surface.paneText}</Paragraph>
        {statusSettled && film?.expired_flag === 1 ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="成片已过期，请重新合成"
          />
        ) : null}
        {statusSettled && (precheck?.blocker_list || []).length ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="合成前置未满足"
            description={(precheck.blocker_list || []).map((b) => `${b.blocker_desc}（${b.count || 0}）`).join('；')}
            action={(
              <Space>
                <Button size="small" type="primary" ghost onClick={() => onJumpTab?.('storyboard')}>去分镜补齐镜头视频</Button>
                <Button size="small" onClick={() => onJumpTab?.('voice')}>去配音字幕检查</Button>
              </Space>
            )}
          />
        ) : null}
        {composeCost ? (
          <Paragraph type="secondary">
            合成预估 {formatYuan(composeCost.estimated_cost_cent)} 元 · 余额 {formatYuan(composeCost.available_cent)} 元
            · 有配音时会混入成片音轨
          </Paragraph>
        ) : null}
        <Space style={{ marginBottom: 12 }} wrap>
          <Select
            placeholder="音乐模型"
            value={musicModelCode}
            onChange={setMusicModelCode}
            style={{ width: 220 }}
            options={musicModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
          />
          <Input value={musicPrompt} onChange={(e) => setMusicPrompt(e.target.value)} style={{ width: 360 }} />
          <Button onClick={async () => {
            const { data } = await featuresApi.filmBgmGenerate({
              work_id: work.work_id,
              episode_id: episodeId,
              model_code: musicModelCode,
              prompt: musicPrompt,
              is_instrumental: 1,
            })
            if (data?.success) {
              message.success('背景音乐任务已提交')
              await waitTask(work.work_id, data.result.task_id)
              await load()
            }
          }}>生成背景音乐</Button>
          <Select
            allowClear
            placeholder="选择背景音乐"
            value={film?.bgm_asset_id}
            style={{ width: 260 }}
            options={(film?.bgm_candidate_list || []).map((item, index) => ({
              value: item.asset_id,
              label: `候选 ${index + 1} / ${item.duration_sec || '-'}秒`,
            }))}
            onChange={async (assetId) => {
              await featuresApi.filmSettingsSave({
                work_id: work.work_id,
                episode_id: episodeId,
                compose_setting: { bgm_asset_id: assetId || '' },
              })
              message.success('背景音乐已选择')
              await load()
            }}
          />
          {(() => {
            const candidates = film?.bgm_candidate_list || []
            const selectedBgm = candidates.find((item) => item.asset_id === film?.bgm_asset_id)
            const previewBgm = selectedBgm || candidates[0]
            if (!previewBgm?.file_url) return null
            return (
              <Space size="small">
                <audio
                  controls
                  preload="metadata"
                  src={previewBgm.file_url}
                  style={{ height: 32, verticalAlign: 'middle' }}
                />
                {!selectedBgm ? (
                  <Button size="small" onClick={async () => {
                    await featuresApi.filmSettingsSave({
                      work_id: work.work_id,
                      episode_id: episodeId,
                      compose_setting: { bgm_asset_id: previewBgm.asset_id },
                    })
                    message.success('背景音乐已选用')
                    await load()
                  }}>选用最新候选</Button>
                ) : null}
              </Space>
            )
          })()}
        </Space>
        <Space wrap>
          <Button onClick={load}>刷新</Button>
          <Button onClick={async () => {
            const { data } = await productionApi.composeEstimate(episodeId)
            if (data?.success) setComposeCost(data.result)
          }}>估合成费用</Button>
          <Button type="primary" loading={composing} disabled={composing || !statusSettled || !precheck?.ready} onClick={async () => {
            setComposing(true)
            try {
              const { data: estRes } = await productionApi.composeEstimate(episodeId)
              if (!estRes?.success) {
                if (estRes?.message) message.warning(estRes.message)
                return
              }
              setComposeCost(estRes.result)
              const ok = await confirmEstimate(estRes.result, '确认合成费用')
              if (!ok) return
              const { data } = await productionApi.composeFilm(episodeId)
              if (data?.success) {
                message.success('合成任务已提交')
                const done = await waitTask(work.work_id, data.result.task_id)
                reportTaskOutcome(done, '本集成片已合成完毕')
                await load()
                await onReload()
              } else if (data?.message) {
                message.warning(data.message)
              }
            } finally {
              setComposing(false)
            }
          }}>合成整集成片</Button>
          {surface.showFilm && film?.download_url ? (
            <Button href={film.download_url} target="_blank" rel="noreferrer">下载成片</Button>
          ) : null}
        </Space>
        {statusSettled && !film?.download_url ? (
          <Paragraph style={{ marginTop: 12 }}>
            这一集现在没有可以预览或下载的成片。
          </Paragraph>
        ) : null}
        {surface.showFilm && film?.download_url ? (
          <div style={{ marginTop: 12 }}>
            <Paragraph>
              成片预览{' '}
              <a href={film.download_url} target="_blank" rel="noreferrer">下载</a>
            </Paragraph>
            <video
              key={film.film_asset_id || film.download_url}
              src={film.download_url}
              controls
              playsInline
              style={portraitFilmPlayerStyle()}
            />
          </div>
        ) : null}
        {film?.baseline_note ? (
          <Alert
            type={film.consistent_flag === 1 ? 'success' : 'warning'}
            showIcon
            style={{ marginTop: 12 }}
            message={film.consistent_flag === 1 ? '成片与锁定版本一致' : '成片不能标成与现行时间轴一致'}
            description={film.baseline_note}
          />
        ) : null}
      </Card>
      <Card title="上架发布到 Tube" style={{ marginTop: 12 }}>
        <Space wrap>
          {(() => {
            const review = reviewActionState({
              reviewStatus: work?.episode_list?.find((ep) => ep.episode_id === episodeId)?.review_status,
              composeReady: film?.compose_status === 'READY' || precheck?.ready === true,
            })
            if (review.mode === 'withdraw') {
              return (
                <Button onClick={async () => {
                  await productionApi.withdrawReview(episodeId)
                  message.success('已撤回上架申请')
                  await load()
                  await onReload()
                }}>撤回上架申请</Button>
              )
            }
            return (
              <Tooltip title={review.submit ? '' : review.submitReason}>
                <span>
                  <Button disabled={!review.submit} onClick={async () => {
                    const { data } = await productionApi.submitReview(episodeId)
                    if (data?.success) {
                      message.success('已提交上架申请')
                      await load()
                      await onReload()
                    }
                  }}>申请上架 Tube</Button>
                </span>
              </Tooltip>
            )
          })()}
          <Button
            type="primary"
            loading={publishing}
            disabled={!canPublish || batchPublishing}
            onClick={runPublish}
          >发布到 Tube</Button>
          <Button
            loading={batchPublishing}
            disabled={!statusSettled || batchPublishing || publishing || !!tokenMissing || mediaPreflight?.config_ready === false
              || !(mediaPreflight?.batch_ready_count > 0)}
            onClick={runBatchPublish}
          >
            批量发布已就绪集
            {mediaPreflight?.batch_ready_count != null ? `（${mediaPreflight.batch_ready_count}）` : ''}
          </Button>
          <Button onClick={async () => {
            await load()
            message.success('已刷新发布状态')
          }}>刷新发布状态</Button>
        </Space>
        {publishHints.length ? (
          <div style={{ marginTop: 12 }}>
            {publishHints.map((hint) => (
              <Paragraph key={hint} style={{ marginBottom: 4 }}>{formatTubePublishHint(hint)}</Paragraph>
            ))}
          </div>
        ) : null}
        {batchProgress ? (
          <Alert
            type="info"
            showIcon
            style={{ marginTop: 12 }}
            message={`批量发布进度 ${batchProgress.progress_percent ?? 0}% · 可播放 ${batchProgress.succeeded || 0} / 已受理未就绪 ${batchProgress.pending || 0} / 结果未知 ${batchProgress.unknown || 0} / 失败 ${batchProgress.failed || 0} / 共 ${batchProgress.total || 0}`}
            description={(batchProgress.item_list || []).slice(0, 12).map((it) => (
              <div key={`${it.episode_id}-${it.film_id}`}>
                {it.progress_index}/{it.progress_total} {it.episode_id} · {it.status === 'UNKNOWN' ? `结果未知 ${it.media_id || '作品号未回写'}` : it.ok ? (it.status === 'PENDING' ? `已受理未就绪 ${it.media_id || ''}` : `可播放 ${it.media_id || ''}`) : `失败 ${it.error || ''}`}
              </div>
            ))}
          />
        ) : null}
        {publishError ? (
          <Alert
            type="error"
            showIcon
            closable
            onClose={() => setPublishError(null)}
            style={{ marginTop: 12 }}
            message={publishError.title}
            description={publishError.detail}
          />
        ) : null}
        {publishLog ? (
          <Alert
            type={publishStatusAlertType(publishLog.status)}
            showIcon
            style={{ marginTop: 12 }}
            message={`最近一次发布 · ${publishLog.status || '-'} · ${publishLog.media_id || '无作品号'}`}
            description={(
              <div>
                {publishLog.play_url ? (
                  <Paragraph style={{ marginBottom: 4 }}>
                    播放链接{' '}
                    <a href={publishLog.play_url} target="_blank" rel="noreferrer">{publishLog.play_url}</a>
                    {' '}
                    <Button
                      size="small"
                      type="link"
                      onClick={async () => {
                        try {
                          await navigator.clipboard.writeText(publishLog.play_url)
                          message.success('已复制')
                        } catch {
                          message.warning('复制失败，请手动选择')
                        }
                      }}
                    >复制</Button>
                  </Paragraph>
                ) : null}
                {publishLog.published_at ? (
                  <Paragraph type="secondary" style={{ marginBottom: 4 }}>
                    发布时间 {String(publishLog.published_at)}
                    {publishLog.http_status != null ? ` · HTTP ${publishLog.http_status}` : ''}
                    {publishLog.attempts != null ? ` · 尝试 ${publishLog.attempts} 次` : ''}
                    {publishLog.idempotency_key ? ` · ${publishLog.idempotency_key}` : ''}
                    {publishLog.replayed ? ' · 幂等回放' : ''}
                  </Paragraph>
                ) : null}
                {publishLog.last_error ? (
                  <Paragraph type="danger" style={{ marginBottom: 0 }}>{publishLog.last_error}</Paragraph>
                ) : null}
              </div>
            )}
          />
        ) : null}
      </Card>
    </>
  )
}

export default function WorkStudio() {
  const { workId, tab } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [work, setWork] = useState(null)
  const [episodeId, setEpisodeId] = useState()
  const [assistantOpen, setAssistantOpen] = useState(false)
  const [composeByEpisode, setComposeByEpisode] = useState({})
  const [mediaPreflight, setMediaPreflight] = useState(null)
  const [guideTick, setGuideTick] = useState(0)
  const [stepState, setStepState] = useState({ episodeId: null, step: null, error: false })
  const [flowState, setFlowState] = useState({
    outlineCount: 0,
    scriptSceneCount: 0,
    consistency: null,
    shots: [],
  })
  const flowSeqRef = useRef(0)
  const followSwitchEpisodeRef = useRef(null)
  const reportComposeEntry = useCallback((id, updater) => {
    setComposeByEpisode((prev) => {
      const cur = prev[id]
      const next = typeof updater === 'function' ? updater(cur) : updater
      if (cur && cur.phase === next.phase && cur.film === next.film && cur.precheck === next.precheck) return prev
      return { ...prev, [id]: next }
    })
  }, [])
  const routedOutlineTaskId = location.state?.outlineTaskId
  const routedId = routedOutlineTaskId == null || routedOutlineTaskId === '' ? null : String(routedOutlineTaskId)
  const consumedOutlineTaskRef = useRef(new Set())
  const [heldOutlineTask, setHeldOutlineTask] = useState(() => (
    routedId ? { workId, taskId: routedId } : null
  ))
  if (
    routedId
    && !consumedOutlineTaskRef.current.has(routedId)
    && (heldOutlineTask?.workId !== workId || heldOutlineTask?.taskId !== routedId)
  ) {
    setHeldOutlineTask({ workId, taskId: routedId })
  }
  const outlineTaskId = heldOutlineTask?.workId === workId ? heldOutlineTask.taskId : null
  const consumeOutlineTask = useCallback((taskId) => {
    if (taskId == null || taskId === '') return
    const id = String(taskId)
    consumedOutlineTaskRef.current.add(id)
    setHeldOutlineTask((current) => (
      current && current.workId === workId && String(current.taskId) === id ? null : current
    ))
  }, [workId])

  useEffect(() => {
    if (!location.state?.outlineTaskId) return
    navigate(
      { pathname: location.pathname, search: location.search, hash: location.hash },
      { replace: true, state: withoutOutlineTaskId(location.state) },
    )
  }, [location, navigate])

  const searchParams = useMemo(() => new URLSearchParams(location.search), [location.search])
  const requestedEpisodeId = searchParams.get('episode_id')
  const requestedStage = searchParams.get('stage')
  const appliedEpisodeQuery = useRef(null)

  const load = useCallback(async () => {
    const { data } = await productionApi.getWork(workId)
    const detail = data?.result
    setWork(detail)
    setEpisodeId((prev) => {
      const list = detail?.episode_list || []
      if (
        requestedEpisodeId
        && appliedEpisodeQuery.current !== requestedEpisodeId
        && list.some((ep) => ep.episode_id === requestedEpisodeId)
      ) {
        appliedEpisodeQuery.current = requestedEpisodeId
        return requestedEpisodeId
      }
      if (prev && list.some((ep) => ep.episode_id === prev)) return prev
      return list[0]?.episode_id
    })
  }, [workId, requestedEpisodeId])

  const loadFlow = useCallback(async () => {
    if (!workId) return
    const seq = flowSeqRef.current + 1
    flowSeqRef.current = seq
    const [outlineRes, panelRes, scriptRes, shotsRes] = await Promise.allSettled([
      productionApi.getOutline(workId),
      productionApi.consistencyPanel(workId),
      episodeId ? productionApi.getScript(episodeId) : Promise.resolve(null),
      episodeId ? productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 }) : Promise.resolve(null),
    ])
    if (seq !== flowSeqRef.current) return
    const outlineList = outlineRes.status === 'fulfilled'
      ? (outlineRes.value?.data?.result?.outline || [])
      : []
    const consistency = panelRes.status === 'fulfilled'
      ? (panelRes.value?.data?.result || null)
      : null
    const scriptScenes = scriptRes.status === 'fulfilled'
      ? (scriptRes.value?.data?.result?.scene_list || [])
      : []
    const shots = shotsRes.status === 'fulfilled' && Array.isArray(shotsRes.value?.data?.result)
      ? shotsRes.value.data.result
      : []
    setFlowState({
      outlineCount: outlineList.length,
      scriptSceneCount: scriptScenes.length,
      consistency,
      shots,
    })
  }, [workId, episodeId])

  const bumpGuide = useCallback(() => {
    setGuideTick((n) => n + 1)
  }, [])

  const handleFlowChange = useCallback(async () => {
    bumpGuide()
    await loadFlow()
  }, [bumpGuide, loadFlow])

  const reloadStudio = useCallback(async () => {
    bumpGuide()
    await Promise.all([load(), loadFlow()])
  }, [bumpGuide, load, loadFlow])

  const buildStagePath = useCallback((nextTab, nextStage, { keepEpisodeParam = true } = {}) => {
    const params = new URLSearchParams()
    const epQuery = keepEpisodeParam ? searchParams.get('episode_id') : null
    if (epQuery) params.set('episode_id', epQuery)
    if (nextStage) params.set('stage', nextStage)
    const qs = params.toString()
    return `/works/${workId}/${nextTab}${qs ? `?${qs}` : ''}`
  }, [searchParams, workId])

  const handleSelectStage = useCallback((stageItem) => {
    if (!stageItem?.tab) return
    followSwitchEpisodeRef.current = null
    navigate(buildStagePath(stageItem.tab, stageItem.key))
  }, [navigate, buildStagePath])

  const jumpTab = useCallback((nextTab) => {
    if (!nextTab) return
    followSwitchEpisodeRef.current = null
    navigate(buildStagePath(nextTab, null))
  }, [navigate, buildStagePath])

  const handleStepChange = useCallback((nextStep, targetEpisodeId) => {
    setStepState({ episodeId: targetEpisodeId, step: nextStep, error: false })
  }, [])

  const handleStepLoadError = useCallback((targetEpisodeId) => {
    setStepState((prev) => ({
      episodeId: targetEpisodeId,
      step: prev.episodeId === targetEpisodeId ? prev.step : null,
      error: true,
    }))
  }, [])

  const handleEpisodeSelect = useCallback((nextEpisodeId) => {
    setEpisodeId(nextEpisodeId)
    if (CORE_STUDIO_TABS.has(tab)) {
      followSwitchEpisodeRef.current = nextEpisodeId
    } else {
      followSwitchEpisodeRef.current = null
    }
    if (searchParams.has('episode_id')) {
      const nextParams = new URLSearchParams(searchParams)
      nextParams.delete('episode_id')
      const qs = nextParams.toString()
      navigate(
        { pathname: location.pathname, search: qs ? `?${qs}` : '', hash: location.hash },
        { replace: true },
      )
    }
  }, [tab, searchParams, navigate, location.pathname, location.hash])

  useEffect(() => { load() }, [load])
  useEffect(() => { loadFlow() }, [loadFlow])

  const hasExplicitTab = Boolean(tab)
  const active = tab || null
  const composeTab = active === 'compose'
  const episodes = work?.episode_list || []
  const current = useMemo(() => episodes.find((e) => e.episode_id === episodeId), [episodes, episodeId])
  const chipSurface = composeSurfaceFromEntry(composeByEpisode[episodeId])
  const resolvedStep = stepState.episodeId === episodeId ? stepState.step : null
  const stepLoadError = stepState.episodeId === episodeId ? stepState.error : false
  const activeAux = useMemo(
    () => AUX_STUDIO_TOOLS.find((item) => item.key === active) || null,
    [active],
  )
  const viewingStageKey = useMemo(() => resolveViewingStageKey({
    activeTab: active,
    requestedStage,
    currentStep: resolvedStep,
    outlineCount: flowState.outlineCount,
  }), [active, requestedStage, resolvedStep, flowState.outlineCount])

  useEffect(() => {
    if (!work || hasExplicitTab || !episodeId) return
    if (resolvedStep) {
      navigate(buildStagePath(resolvedStep.tab, resolvedStep.key), { replace: true, state: location.state })
    } else if (stepLoadError) {
      navigate(buildStagePath('script', null), { replace: true, state: location.state })
    }
  }, [work, hasExplicitTab, episodeId, resolvedStep, stepLoadError, navigate, buildStagePath, location.state])

  useEffect(() => {
    if (!followSwitchEpisodeRef.current) return
    if (followSwitchEpisodeRef.current !== episodeId) return
    if (stepState.episodeId !== episodeId) return
    followSwitchEpisodeRef.current = null
    if (stepState.step) {
      navigate(buildStagePath(stepState.step.tab, stepState.step.key, { keepEpisodeParam: false }), { replace: true })
    }
  }, [episodeId, stepState, navigate, buildStagePath])

  const auxMenu = useMemo(() => ({
    selectedKeys: activeAux ? [activeAux.key] : [],
    items: AUX_STUDIO_TOOLS.map((tool) => ({
      key: tool.key,
      label: tool.label,
    })),
    onClick: ({ key }) => jumpTab(key),
  }), [activeAux, jumpTab])

  if (!work) return <Card loading />

  const waitingInitialStage = !hasExplicitTab && episodes.length > 0 && !stepLoadError

  const renderWorkspace = () => {
    if (waitingInitialStage) {
      return <Card loading style={{ minHeight: 180 }} />
    }
    const effectiveTab = active || 'script'
    if (effectiveTab === 'characters') {
      return <CharacterPane work={work} section="characters" onFlowChange={handleFlowChange} onJumpTab={jumpTab} />
    }
    if (effectiveTab === 'scenes') {
      return <CharacterPane work={work} section="scenes" onFlowChange={handleFlowChange} onJumpTab={jumpTab} />
    }
    if (effectiveTab === 'storyboard') {
      return (
        <StoryboardPane
          work={work}
          episodeId={episodeId}
          currentEpisode={current}
          focusedStage={viewingStageKey}
          onReload={reloadStudio}
          onFlowChange={handleFlowChange}
          onJumpTab={jumpTab}
        />
      )
    }
    if (effectiveTab === 'compose') {
      return (
        <ComposePane
          work={work}
          episodeId={episodeId}
          onReload={reloadStudio}
          onFlowChange={handleFlowChange}
          onJumpTab={jumpTab}
          composeEntry={composeByEpisode[episodeId]}
          onComposeEntry={reportComposeEntry}
          mediaPreflight={mediaPreflight}
          onMediaPreflight={setMediaPreflight}
        />
      )
    }
    if (effectiveTab === 'source') return <SourceImportPage work={work} />
    if (effectiveTab === 'voice') return <VoiceStudioPage work={work} episodeId={episodeId} onJumpTab={jumpTab} />
    if (effectiveTab === 'preview') return <EpisodePreviewPage work={work} episodeId={episodeId} />
    if (effectiveTab === 'story-map') return <StoryMapPage work={work} episodeId={episodeId} />
    if (effectiveTab === 'quality') return <QualityReportPage work={work} episodeId={episodeId} />
    if (effectiveTab === 'continuity') return <ContinuityCenterPage work={work} episodeId={episodeId} />
    if (effectiveTab === 'tasks') return <TasksPane workId={work.work_id} episodeId={episodeId} />
    if (effectiveTab === 'edit-export') return <EditExportPage episodeId={episodeId} />
    return (
      <ScriptPane
        work={work}
        episodeId={episodeId}
        focusedStage={viewingStageKey}
        onReload={reloadStudio}
        onFlowChange={handleFlowChange}
        onJumpTab={jumpTab}
        initialOutlineTaskId={outlineTaskId}
        onOutlineTaskConsumed={consumeOutlineTask}
      />
    )
  }

  return (
    <div className="work-studio">
      <UsageReceipts workRef={work.work_id} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={4} style={{ marginTop: 0 }}>
            {work.work_title}{' '}
            {work.work_status_name ? <Text type="secondary">{`全剧：${work.work_status_name}`}</Text> : null}
          </Title>
          <Paragraph>
            <Link to="/works">返回列表</Link>
            {' · '}
            {work.genre_name} · {work.episode_count} 集 · 已花费 {((work.cost_spent_cent || 0) / 100).toFixed(2)} 元
          </Paragraph>
        </div>
        <Space wrap>
          <Dropdown menu={auxMenu} trigger={['click']}>
            <Button type={activeAux ? 'primary' : 'default'}>
              {activeAux ? `辅助工具：${activeAux.label}` : '辅助工具'}
            </Button>
          </Dropdown>
          <Button onClick={() => setAssistantOpen(true)}>
            AI 协同专家
          </Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>当前剧集</span>
        <Select
          style={{ minWidth: 280 }}
          value={episodeId}
          onChange={handleEpisodeSelect}
          options={episodes.map((ep) => ({
            value: ep.episode_id,
            label: episodeSelectLabel(ep, {
              onCompose: composeTab && ep.episode_id === episodeId,
              surface: chipSurface,
            }),
          }))}
        />
        {current ? (
          composeTab ? (
            <Tag color={episodeComposeTagColor(composeTab, chipSurface)}>
              {episodeComposeTag(current, { onCompose: composeTab, surface: chipSurface })}
            </Tag>
          ) : resolvedStep ? (
            <Tag color={resolvedStep.allDone ? 'success' : 'processing'}>
              {resolvedStep.allDone
                ? `本集进度：${VIDEO_PATH_STEPS.length}/${VIDEO_PATH_STEPS.length} 已完成`
                : `本集环节：${resolvedStep.stepNo}/${VIDEO_PATH_STEPS.length} ${resolvedStep.stepLabel}`}
            </Tag>
          ) : null
        ) : null}
      </Space>
      <ProductionGuide
        work={work}
        episode={current}
        refreshKey={guideTick}
        activeTab={active}
        viewingStageKey={viewingStageKey}
        onSelectStage={handleSelectStage}
        onOpenTab={(key) => {
          if (key && key !== active) jumpTab(key)
        }}
        onStepChange={handleStepChange}
        onStepLoadError={handleStepLoadError}
        onChanged={reloadStudio}
      />
      {activeAux ? (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
            flexWrap: 'wrap',
            padding: '8px 12px',
            marginBottom: 12,
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 8,
          }}
        >
          <Text strong>{`当前视图：${activeAux.label}`}</Text>
          <Button
            size="small"
            type="primary"
            onClick={() => {
              if (resolvedStep) {
                handleSelectStage({ key: resolvedStep.key, tab: resolvedStep.tab })
              } else {
                jumpTab('script')
              }
            }}
          >
            {resolvedStep && !resolvedStep.allDone ? `返回当前环节：${resolvedStep.stepLabel}` : '返回当前环节'}
          </Button>
        </div>
      ) : null}
      {renderWorkspace()}
      <DramaAssistantDrawer
        visible={assistantOpen}
        onClose={() => setAssistantOpen(false)}
        workId={work.work_id}
        episodeId={episodeId}
        onApplied={reloadStudio}
      />
    </div>
  )
}
