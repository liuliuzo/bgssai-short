import { modelPricingCompactLabel, modelPricingLabel, estimateShotBatchTokens } from '../../modelPricing'
import { creatorToolLabel, publicModelText } from '../../publicModel'
import { isPlaceholderMediaUrl, isRealPhotoUrl } from '../../lib/mediaUrl'
import UsageReceipts from '../../components/UsageReceipts'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Card, Checkbox, Collapse, Form, Input, Modal, Select, Skeleton, Space, Table, Tabs, Tag, Tooltip, Typography, message } from 'antd'
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
import { shotCardHeading, shotLatestTaskBadge, shotOffersVideoRegenerate } from './shotCardMedia'
import { runOfficialShotVideo } from './officialVideo'
import {
  hasPlayableShotVideo,
  hasRealFirstFrame,
  resolveRepairAction,
  selectPendingVideoShotIds,
  summarizeConsistencyCards,
} from './productionFlow'
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
  frameLinkOptions,
  knownSceneName,
  modelSelectProps,
  outlineLongTextProps,
  presetIsActive,
  reconcileEmptyScene,
  selectionFeeLine,
  shotCardContainment,
  shotPresetGroups,
  shotVideoDialogProps,
  tokenAmountLabel,
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
  return new Promise((resolve) => {
    Modal.confirm({
      title: title || '确认费用后提交',
      content: (
        <div>
          <p>预计费用 {formatYuan(est.estimated_cost_cent)} 元</p>
          <p>余额可用 {formatYuan(est.available_cent)} 元</p>
          <p>共 {est.task_count || 0} 项（{creatorToolLabel(est.display_name || est.model_code, est.task_type) || '-'}）</p>
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
  return new Promise((resolve) => {
    Modal.confirm({
      title: regenerate ? '重新生成这一镜视频' : '确认生成视频',
      content: (
        <div>
          <p>
            {regenerate
              ? '将开始一次新的生成，并按这一次计费。不会接着上次未完成的回执继续。当前成片会保留，直到新视频成功后才替换。'
              : '确认后开始一次新的生成，并按这一次计费。'}
          </p>
          {lines.map((shot) => (
            <p key={shot.shot_id}>镜{shot.shot_no}：{shot.route_name}。{shot.size_note}</p>
          ))}
          <p>预计费用 {formatYuan(preview.estimated_cost_cent)} 元，余额可用 {formatYuan(preview.available_cent)} 元。</p>
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

function ScriptPane({ work, episodeId, onReload, onFlowChange, onJumpTab, initialOutlineTaskId }) {
  const [outline, setOutline] = useState([])
  const [script, setScript] = useState(null)
  const [textModels, setTextModels] = useState([])
  const [textModelCode, setTextModelCode] = useState()
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [scriptBusy, setScriptBusy] = useState('')
  const [openScenes, setOpenScenes] = useState([])

  const load = useCallback(async () => {
    if (!work?.work_id) return
    setLoading(true)
    try {
      const outlineRes = await productionApi.getOutline(work.work_id)
      setOutline(outlineRes.data?.result?.outline || [])
      if (episodeId) {
        const scriptRes = await productionApi.getScript(episodeId)
        setScript(scriptRes.data?.result || null)
      }
      onFlowChange?.()
    } finally {
      setLoading(false)
    }
  }, [work, episodeId, onFlowChange])

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
    if (!work?.work_id) return undefined
    let cancelled = false
    ;(async () => {
      let taskId = initialOutlineTaskId
      if (!taskId) {
        const outlineRes = await productionApi.getOutline(work.work_id)
        const existing = outlineRes.data?.result?.outline || []
        if (existing.length) return
        const { data } = await productionApi.listTasks({
          work_id: work.work_id,
          task_type: 'TEXT',
          page_num: 1,
          page_size: 50,
        })
        const pending = (data?.result || []).find((row) =>
          row.target_type === 'OUTLINE'
          && (row.task_status === 'QUEUED' || row.task_status === 'RUNNING'))
        taskId = pending?.task_id
      }
      if (!taskId || cancelled) return
      setGenerating(true)
      try {
        const done = await waitTask(work.work_id, taskId)
        if (cancelled) return
        reportTaskOutcome(done, '大纲已生成')
        await onReload()
        await load()
      } finally {
        if (!cancelled) setGenerating(false)
      }
    })()
    return () => { cancelled = true }
  }, [work?.work_id, initialOutlineTaskId, onReload, load])

  useEffect(() => {
    if (!work?.work_id || !episodeId || script?.script_status !== 'GENERATING') return undefined
    let cancelled = false
    ;(async () => {
      const { data } = await productionApi.listTasks({
        work_id: work.work_id,
        task_type: 'TEXT',
        page_num: 1,
        page_size: 50,
      })
      const pending = (data?.result || []).find((row) =>
        row.target_type === 'SCRIPT'
        && (!row.episode_id || row.episode_id === episodeId)
        && (row.task_status === 'QUEUED' || row.task_status === 'RUNNING'))
      if (!pending?.task_id || cancelled) return
      setScriptBusy('generate')
      try {
        const done = await waitTask(work.work_id, pending.task_id)
        if (cancelled) return
        reportTaskOutcome(done, '剧本已生成')
        await load()
        await onReload()
      } finally {
        if (!cancelled) setScriptBusy('')
      }
    })()
    return () => { cancelled = true }
  }, [work?.work_id, episodeId, script?.script_status, load, onReload])

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
      message.success('剧本任务已提交，正在编写分场对白')
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

  const ready = async (force) => {
    if (!episodeId || scriptBusy) return
    setScriptBusy('ready')
    try {
      const { data } = await productionApi.readyScript(episodeId, force)
      if (data?.success) {
        message.success('剧本已定稿，已自动提取本集角色与场景')
        await load()
        await onReload()
      } else if (data?.message) {
        message.warning(data.message)
      }
    } finally {
      setScriptBusy('')
    }
  }

  const scenes = script?.scene_list || []
  const rhythm = script?.rhythm_check || {}
  const rhythmScore = rhythm.total_score ?? script?.total_score
  const isScriptReady = script?.script_status === 'READY'
  const lowRhythmScore = typeof rhythmScore === 'number' && rhythmScore < 60

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

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <div>
        <Title level={5} style={{ margin: 0 }}>第 1 步：剧本与定稿</Title>
        <Paragraph style={{ marginBottom: 0, color: '#334155' }}>先确认全集大纲，再生成当前这一集的分场剧本并点击「定稿剧本」。定稿后会自动提取角色和场景，进入下一步。</Paragraph>
      </div>
      <Card loading={loading || generating} title="全集大纲" extra={<Space>
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
      </Space>}>
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
      <Card title={`第${work.episode_list?.find((e) => e.episode_id === episodeId)?.episode_no || ''}集剧本`} extra={<Space wrap>
        <Select
          placeholder="文本模型"
          value={textModelCode}
          onChange={setTextModelCode}
          style={{ width: 240 }}
          popupMatchSelectWidth={false}
          disabled={!!scriptBusy}
          options={textModels.map((m) => ({ value: m.model_code, label: modelPricingCompactLabel(m), title: modelPricingLabel(m) }))}
        />
        <Button onClick={runScript} loading={scriptBusy === 'generate'} disabled={!!scriptBusy && scriptBusy !== 'generate'}>生成剧本</Button>
        <Button onClick={saveScript} loading={scriptBusy === 'save'} disabled={!!scriptBusy || !scenes.length}>保存</Button>
        <Button type="primary" onClick={() => ready(0)} loading={scriptBusy === 'ready'} disabled={!!scriptBusy || !scenes.length}>定稿剧本</Button>
        <Button onClick={() => ready(1)} loading={scriptBusy === 'ready'} disabled={!!scriptBusy || !scenes.length}>强制定稿</Button>
      </Space>}>
        {!outline.length ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message="请先生成全集大纲"
            description="本集剧本需要依据全集大纲生成。请点击上方「生成大纲」或下方按钮一键生成。"
            action={<Button size="small" type="primary" loading={generating} onClick={runOutline}>立即生成大纲</Button>}
          />
        ) : !scenes.length ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message={scriptBusy === 'generate' ? '正在生成本集分场剧本，请稍候' : '大纲已就绪，点击「生成本集剧本」编写分场与台词'}
            description="生成完成后，点击「定稿剧本」即可自动提取出场角色与场景卡。"
            action={<Button size="small" type="primary" loading={scriptBusy === 'generate'} onClick={runScript}>生成本集剧本</Button>}
          />
        ) : !isScriptReady ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message={lowRhythmScore
              ? `本集剧本草稿已生成（当前节奏评分 ${rhythmScore} 分，未满 60 分）`
              : '本集剧本草稿已生成，请点击「定稿剧本」进入下一步'}
            description={lowRhythmScore
              ? '可直接修改下方台词后保存，也可直接点击「强制定稿并继续」自动提取角色并解锁分镜拆解。'
              : '定稿后系统会自动建立本集角色卡与场景卡，并解锁第 3 步分镜拆解。'}
            action={(
              <Space>
                {lowRhythmScore ? (
                  <Button size="small" type="primary" loading={scriptBusy === 'ready'} onClick={() => ready(1)}>强制定稿并继续</Button>
                ) : (
                  <Button size="small" type="primary" loading={scriptBusy === 'ready'} onClick={() => ready(0)}>定稿剧本并提取角色</Button>
                )}
              </Space>
            )}
          />
        ) : (
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 12 }}
            message="本集剧本已定稿"
            description="出场角色与场景已自动提取。下一步请去「2. 角色定妆」选定参考图锁定造型，再去「3. 分镜与视频」拆镜并生成视频。"
            action={(
              <Space>
                <Button size="small" type="primary" onClick={() => onJumpTab?.('characters')}>下一步：去角色定妆</Button>
                <Button size="small" onClick={() => onJumpTab?.('storyboard')}>直接去分镜与视频</Button>
              </Space>
            )}
          />
        )}
        <Paragraph type="secondary">
          状态 {labelOf(SCRIPT_STATUS_LABEL, script?.script_status, script?.script_status || '-')} / 版本 {script?.script_version || 0} / 节奏总分 {rhythm.total_score ?? script?.total_score ?? '-'}
        </Paragraph>
        {scenes.length ? (
          <Space wrap style={{ marginBottom: 8 }}>
            {scenes.map((scene) => (
              <Button key={scene.scene_no} size="small" onClick={() => revealScene(scene)}>
                场 {scene.scene_no}
              </Button>
            ))}
          </Space>
        ) : <Text type="secondary">这一集还没有场次。</Text>}
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

function CharacterPane({ work, onFlowChange, onJumpTab }) {
  const [chars, setChars] = useState([])
  const [scenes, setScenes] = useState([])
  const [props, setProps] = useState([])
  const [cardsReady, setCardsReady] = useState(false)
  const [imageModels, setImageModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [form] = Form.useForm()
  const [propForm] = Form.useForm()
  const [portrait, setPortrait] = useState(null)
  const [portraitCount, setPortraitCount] = useState(4)
  const [textOnly, setTextOnly] = useState(false)
  const returnParam = encodeURIComponent(`/works/${work.work_id}/characters`)
  const characterLibraryHref = `/look-library?return=${returnParam}`
  const sceneLibraryHref = `/look-library?kind=SCENE&return=${returnParam}`
  const propLibraryHref = `/look-library?kind=PROP&return=${returnParam}`
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
      const { data } = await productionApi.consistencyPanel(work.work_id)
      setChars(data?.result?.character_list || [])
      setScenes(data?.result?.scene_list || [])
      setProps(data?.result?.prop_list || [])
      onFlowChange?.()
    } finally {
      setCardsReady(true)
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
    const storeName = materialKind === 'PROP' ? '道具参考' : materialKind === 'SCENE' ? '场景参考' : '人物定妆'
    try {
      const caption = [cardName, prompt].filter(Boolean).join(' ')
      const { data } = await productionApi.uploadLookMaterial({
        asset_id: assetId,
        material_kind: materialKind,
        material_name: cardName,
        caption,
      })
      if (!data?.success) {
        if (!data?.message) message.error('没有存进参考库')
        return
      }
      message.success(`已存入「我的定妆 · ${storeName}」，后续剧集和作品可直接选用`)
      await load()
    } catch {
      message.error('没有存进参考库')
    } finally {
      markBusy(id, false)
    }
  }

  async function completeAngles(row, angle, replace) {
    if (!row || busyRef.current.has(row.character_id)) return
    const body = {
      character_id: row.character_id,
      model_code: imageModelCode,
      replace_existing: replace ? 1 : 0,
      confirm_cost: 0,
    }
    if (angle) body.view_angle = angle
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
    const price = cost > 0 ? `预计 ${(cost / 100).toFixed(2)} 元。` : '费用按这次出图对账。'
    Modal.confirm({
      title: replace ? `按正脸重做${names}` : `按正脸生成${names}`,
      content: `会用「${row.character_name}」的正脸来画，要求还是同一个人、同一套衣服。${price}模型有时仍会画走样，不满意可以再重做。`,
      okText: '开始生成',
      cancelText: '取消',
      onOk: async () => {
        markBusy(row.character_id, true)
        try {
          const { data } = await productionApi.completeLookAngles({ ...body, confirm_cost: 1 })
          if (!data?.success || !data.result?.task_id) {
            if (!data?.message) message.error('没有排上生成')
            return
          }
          const done = await waitTask(work.work_id, data.result.task_id)
          if (done && done.task_status !== 'SUCCEEDED') {
            message.error(done.fail_reason || '这一面没有生成出来')
          } else if (done) {
            message.success('已按正脸画好')
          } else {
            message.info('还在生成，可以到任务中心看结果')
          }
          load()
        } catch {
          message.error('没有排上生成')
        } finally {
          markBusy(row.character_id, false)
        }
      },
    })
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
      message.success(target.kind === 'character'
        ? (target.angle === 'FRONT_FACE'
          ? '正脸已锁定为定妆照，照片也放进了「我的定妆」'
          : '已保存这一面，照片放进了「我的定妆」')
        : '已锁定为参考图，照片也放进了「我的定妆」')
      load()
    } catch {
      message.error('图片没有上传成功')
    } finally {
      markBusy(target.id, false)
    }
  }

  const cardSummary = summarizeConsistencyCards({
    character_list: chars,
    scene_list: scenes,
    prop_list: props,
  })

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
        <Title level={5} style={{ margin: 0 }}>第 2 步：角色定妆与场景参考图</Title>
        <Paragraph style={{ marginBottom: 0, color: '#334155' }}>
          为出场角色、场景或道具确定参考图并变为「已锁定」。注意：点「生成定妆」出候选图后，必须再点图片下方的「选定」才算锁定。
        </Paragraph>
      </div>
      {cardsReady && cardSummary.totalCards === 0 ? (
        <Alert
          type="info"
          showIcon
          message="当前还没有角色或场景资产卡"
          description="先在「1. 剧本与定稿」完成剧本定稿可自动提取出场角色与场景；也可在下方手动新增，或直接去第 3 步拆解分镜。"
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
          message={`还有 ${cardSummary.unlockedCards.length} 张资产卡未锁定（已锁定 ${cardSummary.lockedCount}/${cardSummary.totalCards}）`}
          description={cardSummary.pendingSelectionCards.length > 0
            ? `${cardSummary.pendingSelectionCards.map((c) => `${c.kind}「${c.name}」`).join('、')} 已有候选图，请点击该卡片下方某张图旁边的「选定」完成锁定；未锁定的卡片会导致引用它的分镜无法出图和生成视频。`
            : `待锁定：${cardSummary.unlockedCards.slice(0, 6).map((c) => `${c.kind}「${c.name}」`).join('、')}。可生成参考图后点「选定」，或直接上传照片、从知识库选用。`}
          action={<Button size="small" onClick={() => onJumpTab?.('storyboard')}>先去分镜与视频</Button>}
        />
      ) : null}
      {cardsReady && cardSummary.totalCards > 0 && cardSummary.unlockedCards.length === 0 ? (
        <Alert
          type="success"
          showIcon
          message={`全部 ${cardSummary.totalCards} 张角色、场景与道具卡均已锁定`}
          description="造型与场景参考图已就绪，现在可以进入「3. 分镜与视频」一键拆解分镜并生成视频。"
          action={<Button size="small" type="primary" onClick={() => onJumpTab?.('storyboard')}>下一步：去分镜与视频</Button>}
        />
      ) : null}
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
        <input ref={ownImageRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={onOwnImage} />
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary">先选定一张参考图，再生成定妆。可以上传照片，也可以从定妆知识库里选，不用离开这个作品。</Text>
        </div>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !chars.length ? <Text type="secondary">还没有角色。先在上面写下角色名，再来定妆。</Text> : null}
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
            {(viewOf('FRONT_FACE') || isRealPhotoUrl(row.ref_image_url)) ? (
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">缺的面可以按正脸生成。侧面和背面会跟着这张正脸的人和衣服来画。</Text>
                <Space wrap style={{ marginTop: 4 }}>
                  {LOOK_ANGLES.filter(([angle]) => angle !== 'FRONT_FACE').map(([angle, label]) => (
                    <Button
                      key={angle}
                      size="small"
                      disabled={busyIds.has(row.character_id)}
                      onClick={() => completeAngles(row, angle, !!viewOf(angle))}
                    >
                      {viewOf(angle) ? `按正脸重做${label}` : `按正脸生成${label}`}
                    </Button>
                  ))}
                  {LOOK_ANGLES.filter(([angle]) => angle !== 'FRONT_FACE' && !viewOf(angle)).length >= 2 ? (
                    <Button size="small" type="link" disabled={busyIds.has(row.character_id)} onClick={() => completeAngles(row, null, false)}>一次补齐缺的面</Button>
                  ) : null}
                </Space>
              </div>
            ) : null}
            <LibraryLookPicker
              kind="CHARACTER"
              libraryHref={characterLibraryHref}
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
                <Text type="secondary">这些是已经生成或上传的图。点击某张图下方的「选定」即可锁定定妆。</Text>
                <Space wrap style={{ marginTop: 8 }}>
                  {candidates.filter((item) => isRealPhotoUrl(item.file_url)).map((c) => (
                    <div key={c.asset_id} style={{ textAlign: 'center' }}>
                      <ClickThumb alt="定妆候选" src={c.file_url} width={72} height={128} />
                      <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                        const { data } = await productionApi.lockCharacter(row.character_id, c.asset_id)
                        if (!data?.success) return
                        message.success(`已锁定「${row.character_name}」的定妆照`)
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
      <Card title="场景卡">
        <Form layout="inline" style={{ marginBottom: 12 }} onFinish={async (v) => {
          await productionApi.createScene({ work_id: work.work_id, card_type: 'SCENE', ...v })
          load()
        }}>
          <Form.Item name="card_name" rules={[{ required: true }]}><Input placeholder="场景名" /></Form.Item>
          <Form.Item name="appearance_prompt"><Input placeholder="布景描述" /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !scenes.length ? <Text type="secondary">还没有场景。先在上面写下场景名，再上传或从知识库选一张参考图。</Text> : null}
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
            <Text type="secondary">可以上传一张照片，也可以从场景库里选用已有场景，或把选定的生成图一键存入场景库。</Text>
            <LibraryLookPicker
              kind="SCENE"
              libraryHref={sceneLibraryHref}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
            <Space wrap style={{ marginTop: 8 }}>
              {isRealPhotoUrl(row.ref_image_url) ? <ClickThumb alt="当前参考图" src={row.ref_image_url} width={96} height={64} /> : null}
              {realSceneCandidates.map((c) => (
                <div key={c.asset_id} style={{ textAlign: 'center' }}>
                  <ClickThumb alt="场景参考" src={c.file_url} width={96} height={64} />
                  <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                    const { data } = await productionApi.lockScene(row.scene_card_id, c.asset_id)
                    if (!data?.success) return
                    message.success(`已锁定场景「${row.card_name}」的参考图`)
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
      <Card title="道具卡">
        <Form form={propForm} layout="inline" style={{ marginBottom: 12 }} onFinish={async (v) => {
          await productionApi.createScene({ work_id: work.work_id, card_type: 'PROP', ...v })
          propForm.resetFields()
          load()
        }}>
          <Form.Item name="card_name" rules={[{ required: true }]}><Input placeholder="道具名，如退婚书" /></Form.Item>
          <Form.Item name="appearance_prompt"><Input placeholder="外观描述" /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {!cardsReady ? <LibrarySkeleton /> : null}
        {cardsReady && !props.length ? <Text type="secondary">还没有道具。先在上面写下道具名，再上传或从知识库选一张参考图。</Text> : null}
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
            <Text type="secondary">可以上传一张照片，也可以从道具库里选用已有道具，或把选定的生成图一键存入道具库。</Text>
            <LibraryLookPicker
              kind="PROP"
              libraryHref={propLibraryHref}
              busy={busyIds.has(row.scene_card_id)}
              onUse={(materialId) => useLook('scene', row.scene_card_id, materialId)}
            />
            <Space wrap style={{ marginTop: 8 }}>
              {isRealPhotoUrl(row.ref_image_url) ? <ClickThumb alt="当前参考图" src={row.ref_image_url} width={72} height={72} /> : null}
              {realPropCandidates.map((c) => (
                <div key={c.asset_id} style={{ textAlign: 'center' }}>
                  <ClickThumb alt="道具参考" src={c.file_url} width={72} height={72} />
                  <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                    const { data } = await productionApi.lockScene(row.scene_card_id, c.asset_id)
                    if (!data?.success) return
                    message.success(`已锁定道具「${row.card_name}」的参考图`)
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
          <span>{portraitPrice}。只有生成成功才会计入用量。</span>
        </Space>
      </Modal>
    </Space>
  )
}

function modelLabel(row) { return modelPricingLabel(row) }

function StoryboardPane({ work, episodeId, currentEpisode, onReload, onFlowChange, onJumpTab }) {
  const [canvasView, setCanvasView] = useState(false)
  const [rows, setRows] = useState([])
  const [shotsLoaded, setShotsLoaded] = useState(false)
  const [splitting, setSplitting] = useState(false)
  const [actionError, setActionError] = useState(null)
  const [selected, setSelected] = useState([])
  const [cost, setCost] = useState(null)
  const [drafts, setDrafts] = useState({})
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
  }, [episodeId])

  const load = useCallback(async () => {
    if (!episodeId) return
    try {
      const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 })
      setRows(Array.isArray(data?.result) ? data.result : [])
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

  const pendingVideoIds = useMemo(() => selectPendingVideoShotIds(rows), [rows])
  const videoReadyCount = useMemo(() => rows.filter(hasPlayableShotVideo).length, [rows])
  const firstFrameCount = useMemo(() => rows.filter(hasRealFirstFrame).length, [rows])
  const blockedCount = useMemo(() => rows.filter((r) => Number(r.blocked_flag) === 1).length, [rows])
  const allVideosReady = rows.length > 0 && videoReadyCount === rows.length
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
        const msg = splitRes?.data?.message || '请先在「1. 剧本与定稿」完成本集定稿，再拆解分镜'
        showActionError(msg)
        return
      }
      await featuresApi.previzGenerateText({ episode_id: episodeId })
      message.success('已从剧本拆解分镜，并写好每一镜的人话描述')
      await load()
      await onReload?.()
    } catch (err) {
      showActionError(err?.response?.data?.message || err?.message || '拆解分镜失败，请稍后重试')
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
        onSwitchToList={() => setCanvasView(false)}
      />
    )
  }

  const runOfficial = async (shotIds, { regenerate = false } = {}) => {
    setActionError(null)
    const body = { episode_id: episodeId, shot_ids: shotIds }
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
    message.success(regenerate ? '已重新提交这一镜视频，当前成片会保留到新视频成功' : '视频已提交，生成通常要几分钟')
    if (notes[0]) message.info(notes[0])
    if (batchId) {
      setVideoWatch({ batchId, progress: null, done: false, lingering: false, error: '' })
    }
    load()
  }

  const submitLevel = async (taskType, autoNext) => {
    setActionError(null)
    if (taskType === 'VIDEO') {
      const targetShotIds = selected.length ? selected : (pendingVideoIds.length ? pendingVideoIds : undefined)
      await runOfficial(targetShotIds, { regenerate: false })
      return
    }
    const estimateBody = {
      episode_id: episodeId,
      shot_ids: selectedIds,
      task_type: taskType,
      auto_next_flag: autoNext ? 1 : 0,
      model_code: taskType === 'VIDEO' ? videoModelCode : imageModelCode,
      next_model_code: autoNext ? videoModelCode : undefined,
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
    setCost(estRes.result)
    const ok = await confirmEstimate(estRes.result, taskType === 'VIDEO' ? '确认视频费用' : '确认出图费用')
    if (!ok) return
    const { data } = await productionApi.submitTasks(estimateBody)
    if (!data?.success) {
      showActionError(data?.message || '出图任务提交失败，请稍后重试')
      return
    }
    if (taskType === 'VIDEO') {
      const batchId = data.result && data.result.batch_id
      message.success(batchId ? '视频已提交，生成通常要几分钟' : '视频已提交，请到任务中心查看')
      if (batchId) {
        setVideoWatch({ batchId, progress: null, done: false, lingering: false, error: '' })
      }
      load()
      return
    }
    message.success('草图已提交，出图后先看构图对不对')
    await waitBatch(work.work_id, data.result.batch_id, data.result.task_id_list)
    load()
  }

  const imageModelLabel = modelLabel(imageModels.find((item) => item.model_code === imageModelCode) || {})
  const videoModelLabel = modelLabel(videoModels.find((item) => item.model_code === videoModelCode) || {})
  const imageSelect = modelSelectProps(imageModelLabel)
  const videoSelect = modelSelectProps(videoModelLabel)
  const scriptReady = currentEpisode?.script_status === 'READY'

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="按顶部步骤出视频"
        description="顶部步骤会带到定稿、定妆、拆镜、首帧和生成视频。这一页改人话分镜不花钱；确认无误后可一键出首帧并批量生成镜头视频。有视频的镜头在卡片左边播放；放不了的会直接写明。"
      />
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
      {allVideosReady ? (
        <Alert
          type="success"
          showIcon
          message={`本集全部 ${rows.length} 个镜头视频已就绪，可以合成成片`}
          description="可直接进入合成成片把所有镜头拼接为完整 MP4 并烧录台词字幕；如需人声配音，也可先去配音字幕。"
          action={(
            <Space>
              <Button size="small" type="primary" onClick={() => onJumpTab?.('compose')}>下一步：去合成成片</Button>
              <Button size="small" onClick={() => onJumpTab?.('voice')}>去配音字幕（可选）</Button>
            </Space>
          )}
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
      <Space wrap>
        <Button onClick={() => setCanvasView(true)} disabled={!rows.length}>切到画布</Button>
        <Button loading={splitting} onClick={handleSplitShots}>从剧本拆镜</Button>
        <Button disabled={!rows.length} onClick={async () => {
          await featuresApi.previzGenerateText({ episode_id: episodeId, shot_id_list: selectedIds })
          message.success('已用人话写清每一镜')
          load()
        }}>生成人话分镜（免费）</Button>
      </Space>
      {shotsLoaded && rows.length === 0 ? (
        <Card>
          <Title level={5} style={{ marginTop: 0 }}>
            {scriptReady ? '本集剧本已定稿，请一键拆解分镜' : '本集还没有分镜（需先完成第 1 步剧本定稿）'}
          </Title>
          <Paragraph style={{ color: '#334155' }}>
            {scriptReady
              ? '点击下方「一键从剧本拆解分镜」，系统会自动生成本集镜头表、出场角色绑定和人话画面描述，随后即可生成草图与镜头视频。'
              : '分镜需要从已定稿的分场剧本自动拆解。请先前往「1. 剧本与定稿」生成本集剧本并点击「定稿剧本」，再回到本页一键拆镜。'}
          </Paragraph>
          <Space>
            {scriptReady ? (
              <Button type="primary" loading={splitting} onClick={handleSplitShots}>一键从剧本拆解分镜</Button>
            ) : (
              <Button type="primary" onClick={() => onJumpTab?.('script')}>去第 1 步完成剧本定稿</Button>
            )}
            <Button onClick={() => onJumpTab?.('characters')}>查看角色定妆</Button>
          </Space>
        </Card>
      ) : null}
      {rows.length > 0 ? (
        <Card
          size="small"
          title="本集出图与生成视频"
          extra={(
            <Text type="secondary">
              共 {rows.length} 镜 · 首帧 {firstFrameCount}/{rows.length} · 视频 {videoReadyCount}/{rows.length}
              {blockedCount > 0 ? ` · 待定妆解锁 ${blockedCount} 镜` : ''}
            </Text>
          )}
        >
          <Space direction="vertical" style={{ width: '100%' }} size="small">
            <Space wrap>
              <Button
                size="small"
                disabled={!pendingVideoIds.length}
                onClick={() => setSelected(pendingVideoIds)}
              >
                勾选待出视频镜头（{pendingVideoIds.length}）
              </Button>
              <Button
                size="small"
                onClick={() => setSelected(rows.map((r) => r.shot_id))}
              >
                全选本集（{rows.length}）
              </Button>
              <Button
                size="small"
                disabled={!selected.length}
                onClick={() => setSelected([])}
              >
                清空勾选
              </Button>
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
            <Space wrap>
              <Button onClick={async () => {
                setActionError(null)
                const { data } = await productionApi.estimateTasks({
                  episode_id: episodeId,
                  shot_ids: selectedIds,
                  task_type: 'IMAGE',
                  auto_next_flag: 0,
                  model_code: imageModelCode,
                })
                if (!data?.success) {
                  showActionError(data?.message || '草图费用预估未通过，请先锁定角色或场景参考图')
                  return
                }
                setCost(data?.result)
              }}>估草图费用</Button>
              <Button onClick={() => submitLevel('IMAGE', 0)}>出草图确认构图</Button>
              <Button onClick={async () => {
                const { data } = await featuresApi.frameLinkBatch({
                  episode_id: episodeId,
                  frame_link_mode: 'INHERIT_PREV',
                })
                if (data?.success) {
                  message.success(`已衔接 ${data.result.linked_count} 处，跨场 ${data.result.cross_scene_count} 处按剪辑语言保持不接`)
                  load()
                }
              }}>同场次首尾帧衔接</Button>
              <Button type="primary" onClick={() => submitLevel('VIDEO', 0)}>
                {selected.length
                  ? `生成已勾选镜头视频（${selected.length} 镜）`
                  : pendingVideoIds.length
                    ? `一键生成待出视频镜头（${pendingVideoIds.length} 镜）`
                    : '生成本集视频'}
              </Button>
            </Space>
            {cost ? (
              <Paragraph style={{ marginBottom: 0 }}>
                约 {cost.task_count} 镜 / {formatYuan(cost.estimated_cost_cent)} 元
                （预计 {tokenAmountLabel(estimateShotBatchTokens(selected.length ? selected : rows, 1000).billed_tokens)} · 余额 {formatYuan(cost.available_cent)} 元 · {publicModelText(cost.display_name || cost.model_code, cost.task_type) || '-'}）
                {(cost.shot_cost_list || []).length > 0
                  ? ` · 明细 ${(cost.shot_cost_list || []).slice(0, 5).map((s) => `镜${s.shot_no}:${formatYuan(s.estimated_cost_cent)}`).join('、')}`
                  : ''}
              </Paragraph>
            ) : (
              <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                {selectionFeeLine(selected.length, estimateShotBatchTokens(selected, 1000).billed_tokens)}
              </Paragraph>
            )}
          </Space>
        </Card>
      ) : null}
      <Collapse
        items={[{
          key: 'advanced-tools',
          label: '高级工具：手动改绑镜头角色场景 / 单张图片试做图生视频',
          children: (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Card size="small" title="这一集用到的角色和场景" extra={<Text type="secondary">勾选镜头后再绑定</Text>}>
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
                    disabled={!selected.length}
                    onClick={async () => {
                      for (const shotId of selected) {
                        const body = {
                          shot_id: shotId,
                          character_id_list: bindChars,
                          prop_id_list: bindProps,
                        }
                        if (bindScene !== undefined) body.scene_card_id = bindScene || ''
                        await productionApi.bindShotAssets(body)
                      }
                      message.success(`已绑定 ${selected.length} 镜`)
                      load()
                    }}
                  >绑定到已勾选镜头</Button>
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
      {rows.map((row) => {
        const readiness = evaluateShotReadiness(row, panel)
        const taskBadge = shotLatestTaskBadge(row)
        const playback = playbackKind[row.shot_id]
        const showTaskBadge = taskBadge && playback !== 'broken'
        const sceneCardName = (panel.scene_list || []).find((c) => c.scene_card_id === row.scene_card_id)?.card_name || ''
        const sceneKnown = knownSceneName({
          sceneName: sceneCardName,
          shotDesc: row.shot_desc,
          lookPlain: row.look_plain,
        })
        const spoken = reconcileEmptyScene(drafts[row.shot_id] ?? row.previz_text ?? '', sceneKnown)
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
            <Tag color={readiness.ready ? 'cyan' : 'default'}>
              {readiness.ready ? '锚点就绪' : '待定妆绑定'}
            </Tag>
            {row.stale_flag === 1 ? <Tag color="orange">定妆已变，建议重抽</Tag> : null}
            {row.blocked_flag === 1 ? <Tag color="red">待定妆解锁</Tag> : null}
            <Tag color={(row.prompt_score || 0) >= 70 ? 'green' : 'orange'}>提示词 {row.prompt_score ?? '-'} 分</Tag>
            <Button size="small" onClick={() => {
              setSelected((cur) => cur.includes(row.shot_id) ? cur.filter((id) => id !== row.shot_id) : cur.concat(row.shot_id))
            }}>{selected.includes(row.shot_id) ? '取消勾选' : '勾选'}</Button>
            {row.shot_status === 'LOCKED'
              ? <Button size="small" onClick={async () => { await productionApi.unlockShot(row.shot_id); load() }}>解锁</Button>
              : <Button size="small" onClick={async () => { await productionApi.lockShot(row.shot_id); load() }}>锁定</Button>}
          </Space>}
        >
          <Space align="start" size="large" style={{ width: '100%' }}>
            <ShotMediaColumn
              row={row}
              onRegenerate={shotOffersVideoRegenerate(row, playback) ? () => runOfficial([row.shot_id], { regenerate: true }) : null}
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
              <Paragraph style={{ marginBottom: 8 }}>
                <Text strong>这一镜会拍成：</Text>
                {row.look_plain || '先写人话分镜，系统会告诉你观众将看到什么。'}
              </Paragraph>
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
              </Space>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', marginBottom: 8 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>和上一镜</Text>
                <Select
                  size="small"
                  style={{ width: 120 }}
                  value={row.frame_link_mode || 'NONE'}
                  aria-label="和上一镜怎么接"
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
                {row.link_hint
                  ? <Tag color={row.link_effective_flag === 1 ? 'blue' : 'orange'}>{row.link_hint}</Tag>
                  : null}
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
              {row.reference_note ? (
                <Paragraph type="secondary" style={{ marginBottom: 8 }}>{row.reference_note}</Paragraph>
              ) : null}
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
              <Text type="secondary">人话分镜（改这里免费，不用写提示词）</Text>
              <TextArea
                className="shot-spoken"
                rows={4}
                style={{ marginTop: 6 }}
                value={spoken}
                onChange={(e) => setDrafts((cur) => ({ ...cur, [row.shot_id]: e.target.value }))}
              />
              <Space className="shot-actions" style={{ marginTop: 8 }} wrap>
                <Button size="small" onClick={async () => {
                  const text = spoken
                  const { data } = await featuresApi.previzSave({ shot_id: row.shot_id, previz_text: text })
                  const saved = data?.result || {}
                  patchRow(row.shot_id, {
                    previz_text: saved.previz_text || text,
                    look_plain: saved.look_plain,
                    image_prompt: saved.image_prompt,
                    prompt_score: saved.prompt_score,
                  })
                  message.success('已按人话更新这一镜')
                }}>保存人话</Button>
                <Button size="small" onClick={async () => {
                  const { data } = await featuresApi.previzPromptCheck({ shot_id: row.shot_id })
                  const check = data?.result || {}
                  patchRow(row.shot_id, {
                    look_plain: check.look_plain,
                    prompt_score: check.prompt_score,
                    prompt_issues: check.issue_list,
                  })
                }}>提示词会拍出什么</Button>
                <Button size="small" disabled={row.shot_status === 'LOCKED'} onClick={async () => {
                  setActionError(null)
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
                  message.success('正在出图确认构图')
                  await waitTask(work.work_id, data.result.task_id_list?.[0])
                  load()
                }}>出一张图看看</Button>
                <Button size="small" type="primary" disabled={row.shot_status === 'LOCKED'} onClick={() => runOfficial([row.shot_id], { regenerate: false })}>生成视频</Button>
                {shotOffersVideoRegenerate(row, playback) ? (
                  <Button size="small" type="primary" onClick={() => runOfficial([row.shot_id], { regenerate: true })}>重新生成</Button>
                ) : null}
              </Space>
              <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                有真实首帧或定妆照时走图生视频：384×256，约 3 秒，不是竖屏 9:16。没有真实照片时改走文生视频：约 720×480，约 6 秒。
              </Paragraph>
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
  const [loading, setLoading] = useState(false)
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
      setRows(Array.isArray(data?.result) ? data.result : [])
    } finally {
      setLoading(false)
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
        loading={loading}
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
      const [p, f, mp] = await Promise.all([
        productionApi.filmPrecheck(requestedId),
        productionApi.filmDetail(requestedId),
        productionApi.publishFilmToMediaPreflight({ episode_id: requestedId, work_id: work?.work_id }),
      ])
      if (seq !== requestSeq.current) return
      const nextPrecheck = p.data?.result ?? null
      const nextFilm = f.data?.result ?? null
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
        content: `将发布本作品全部已就绪且已过审的成片（约 ${count} 集）。确认继续？`,
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
    <Card title="第 5 步：合成成片与提审">
      <Paragraph style={{ marginBottom: 4 }}>
        <Text strong>{surface.headline}</Text>
      </Paragraph>
      <Paragraph style={{ color: '#334155' }}>{surface.nextStep}</Paragraph>
      {work?.work_status_name ? (
        <Paragraph type="secondary">
          作品阶段是「{work.work_status_name}」。上面说的是当前这一集，两处可以不一样。
        </Paragraph>
      ) : null}
      <Paragraph type="secondary" style={{ fontSize: 13 }}>{surface.paneText}</Paragraph>
      {statusSettled && film?.expired_flag === 1 ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="成片已过期"
          description="镜头重抽或配音重配后，旧成片不能下载，也不能再回放上次的 Tube 目录。请重新合成并通过审核后再发布。"
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
              message.success('合成任务已提交（有配音会混入成片）')
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
        <Button onClick={async () => {
          const { data } = await productionApi.submitReview(episodeId)
          if (data?.success) {
            message.success('已提交审核')
            await onReload()
          }
        }}>提交审核</Button>
        <Button onClick={async () => {
          await productionApi.withdrawReview(episodeId)
          message.success('已撤回')
          await onReload()
        }}>撤回审核</Button>
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
            <Paragraph key={hint} style={{ marginBottom: 4 }}>{hint}</Paragraph>
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
      {statusSettled && !film?.download_url ? (
        <Paragraph style={{ marginTop: 12 }}>
          这一集现在没有可以预览或下载的成片。
        </Paragraph>
      ) : null}
      {film?.download_url ? (
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
            style={{ width: '100%', maxWidth: 360, background: '#111' }}
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
  const [flowState, setFlowState] = useState({
    outlineCount: 0,
    scriptSceneCount: 0,
    consistency: null,
    shots: [],
  })
  const flowSeqRef = useRef(0)
  const reportComposeEntry = useCallback((id, updater) => {
    setComposeByEpisode((prev) => {
      const cur = prev[id]
      const next = typeof updater === 'function' ? updater(cur) : updater
      if (cur && cur.phase === next.phase && cur.film === next.film && cur.precheck === next.precheck) return prev
      return { ...prev, [id]: next }
    })
  }, [])
  const outlineTaskId = location.state?.outlineTaskId
  const requestedEpisodeId = new URLSearchParams(location.search).get('episode_id')
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

  const reloadStudio = useCallback(async () => {
    await Promise.all([load(), loadFlow()])
  }, [load, loadFlow])

  const jumpTab = useCallback((nextTab) => {
    if (nextTab) navigate(`/works/${workId}/${nextTab}`)
  }, [navigate, workId])

  useEffect(() => { load() }, [load])
  useEffect(() => { loadFlow() }, [loadFlow])

  const active = tab || 'script'
  const composeTab = active === 'compose'
  const episodes = work?.episode_list || []
  const current = useMemo(() => episodes.find((e) => e.episode_id === episodeId), [episodes, episodeId])
  const chipSurface = composeSurfaceFromEntry(composeByEpisode[episodeId])

  if (!work) return <Card loading />

  return (
    <div className="work-studio">
      <UsageReceipts workRef={work.work_id} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={4} style={{ marginTop: 0 }}>
            {work.work_title} <Text type="secondary">{work.work_status_name}</Text>
          </Title>
          <Paragraph>
            <Link to="/works">返回列表</Link>
            {' · '}
            {work.genre_name} · {work.episode_count} 集 · 已花费 {((work.cost_spent_cent || 0) / 100).toFixed(2)} 元
          </Paragraph>
        </div>
        <Space wrap>
          <Button onClick={() => setAssistantOpen(true)}>
            AI 协同专家
          </Button>
          <Button
            onClick={() => navigate(`/works/${workId}/edit-export`)}
          >
            导出剪辑工程
          </Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>当前剧集</span>
        <Select
          style={{ minWidth: 280 }}
          value={episodeId}
          onChange={setEpisodeId}
          options={episodes.map((ep) => ({
            value: ep.episode_id,
            label: episodeSelectLabel(ep, {
              onCompose: composeTab && ep.episode_id === episodeId,
              surface: chipSurface,
            }),
          }))}
        />
        {current ? (
          <Tag color={episodeComposeTagColor(composeTab, chipSurface)}>
            {episodeComposeTag(current, { onCompose: composeTab, surface: chipSurface })}
          </Tag>
        ) : null}
      </Space>
      <ProductionGuide
        work={work}
        episode={current}
        onOpenTab={(key) => {
          if (key && key !== active) navigate(`/works/${workId}/${key}`)
        }}
        onChanged={reloadStudio}
      />
      <Tabs
        className="work-studio-tabs"
        activeKey={active}
        onChange={(key) => navigate(`/works/${workId}/${key}`)}
        tabBarGutter={12}
        items={[
          { key: 'script', label: '剧本', children: <ScriptPane work={work} episodeId={episodeId} onReload={reloadStudio} onFlowChange={loadFlow} onJumpTab={jumpTab} initialOutlineTaskId={outlineTaskId} /> },
          { key: 'characters', label: '角色场景', children: <CharacterPane work={work} onFlowChange={loadFlow} onJumpTab={jumpTab} /> },
          { key: 'storyboard', label: '分镜', children: <StoryboardPane work={work} episodeId={episodeId} currentEpisode={current} onReload={reloadStudio} onFlowChange={loadFlow} onJumpTab={jumpTab} /> },
          { key: 'compose', label: '成片', children: (
            <ComposePane
              work={work}
              episodeId={episodeId}
              onReload={reloadStudio}
              onFlowChange={loadFlow}
              onJumpTab={jumpTab}
              composeEntry={composeByEpisode[episodeId]}
              onComposeEntry={reportComposeEntry}
              mediaPreflight={mediaPreflight}
              onMediaPreflight={setMediaPreflight}
            />
          ) },
          { key: 'source', label: '原著', children: <SourceImportPage work={work} /> },
          { key: 'voice', label: '配音', children: <VoiceStudioPage work={work} episodeId={episodeId} onJumpTab={jumpTab} /> },
          { key: 'preview', label: '预览', children: <EpisodePreviewPage work={work} episodeId={episodeId} /> },
          { key: 'story-map', label: '剧情', children: <StoryMapPage work={work} episodeId={episodeId} /> },
          { key: 'quality', label: '质量', children: <QualityReportPage work={work} episodeId={episodeId} /> },
          { key: 'continuity', label: '连贯性', children: <ContinuityCenterPage work={work} /> },
          { key: 'tasks', label: '任务', children: <TasksPane workId={work.work_id} episodeId={episodeId} /> },
          { key: 'edit-export', label: '剪辑', children: <EditExportPage episodeId={episodeId} /> },
        ]}
      />
      <DramaAssistantDrawer
        visible={assistantOpen}
        onClose={() => setAssistantOpen(false)}
        workId={work.work_id}
        episodeId={episodeId}
        onApplied={reloadStudio}
      />
      <style>{`
        .work-studio-tabs .ant-tabs-nav { margin-bottom: 12px; }
        .work-studio-tabs .ant-tabs-nav-list { flex-wrap: wrap; }
        .work-studio-tabs .ant-tabs-tab { white-space: normal; padding: 8px 0; }
        .work-studio-tabs .ant-tabs-ink-bar { height: 2px; }
      `}</style>
    </div>
  )
}
