import { modelPricingLabel } from '../../modelPricing'
import UsageReceipts from '../../components/UsageReceipts'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Collapse, Form, Input, Modal, Select, Space, Table, Tabs, Tag, Typography, message } from 'antd'
import { Link, useNavigate, useParams } from 'react-router-dom'
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
import DramaAssistantDrawer from '../../components/DramaAssistantDrawer'
import { SHOT_TYPES, CAMERA_MOVEMENTS, DRAMA_STYLES, QUAD_GRID_ANGLES, CONTINUITY_PRESETS } from '../../utils/CinematographyHelper'

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
          <p>共 {est.task_count || 0} 项（{est.model_code || '-'}）</p>
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

async function waitTask(workId, taskId) {
  if (!taskId) return null
  const rows = await waitTasks(workId, [taskId])
  return rows[0] || null
}

async function waitTasks(workId, taskIds) {
  const ids = [...new Set((taskIds || []).filter(Boolean))]
  if (!ids.length) return []
  for (let i = 0; i < 80; i += 1) {
    const { data } = await productionApi.listTasks({ work_id: workId, page_num: 1, page_size: 100 })
    const rows = data?.result || []
    const matched = ids.map((id) => rows.find((item) => item.task_id === id)).filter(Boolean)
    const done = matched.length === ids.length
      && matched.every((row) => row.task_status === 'SUCCEEDED'
        || row.task_status === 'FAILED'
        || row.task_status === 'CANCELED')
    if (done) return matched
    await sleep(1500)
  }
  return []
}

async function waitBatch(workId, batchId, taskIds) {
  if (batchId) {
    for (let i = 0; i < 80; i += 1) {
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

function ScriptPane({ work, episodeId, onReload }) {
  const [outline, setOutline] = useState([])
  const [script, setScript] = useState(null)
  const [textModels, setTextModels] = useState([])
  const [textModelCode, setTextModelCode] = useState()
  const [loading, setLoading] = useState(false)

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
    } finally {
      setLoading(false)
    }
  }, [work, episodeId])

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

  const runOutline = async () => {
    const { data } = await productionApi.generateOutline(work.work_id, textModelCode)
    if (!data?.success) return
    message.success('大纲任务已提交')
    await waitTask(work.work_id, data.result.task_id)
    await onReload()
    await load()
  }

  const saveOutline = async () => {
    await productionApi.saveOutline({ work_id: work.work_id, episode_list: outline })
    message.success('大纲已保存')
    await onReload()
  }

  const runScript = async () => {
    const { data } = await productionApi.generateScript(episodeId, undefined, textModelCode)
    if (!data?.success) return
    message.success('剧本任务已提交')
    await waitTask(work.work_id, data.result.task_id)
    await load()
    await onReload()
  }

  const saveScript = async () => {
    await productionApi.saveScript({
      episode_id: episodeId,
      script_version: script?.script_version,
      scene_list: script?.scene_list || [],
    })
    message.success('剧本已保存')
    await load()
  }

  const ready = async (force) => {
    const { data } = await productionApi.readyScript(episodeId, force)
    if (data?.success) {
      message.success('已定稿')
      await load()
      await onReload()
    }
  }

  const scenes = script?.scene_list || []
  const rhythm = script?.rhythm_check || {}

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card loading={loading} title="全集大纲" extra={<Space>
        <Select
          placeholder="文本模型"
          value={textModelCode}
          onChange={setTextModelCode}
          style={{ width: 220 }}
          options={textModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
        />
        <Button onClick={runOutline}>生成大纲</Button>
        <Button type="primary" onClick={saveOutline}>保存大纲</Button>
      </Space>}>
        <Table
          rowKey="episode_no"
          size="small"
          pagination={false}
          dataSource={outline}
          columns={[
            { title: '集', dataIndex: 'episode_no', width: 60 },
            {
              title: '标题',
              dataIndex: 'episode_title',
              render: (v, row, i) => <Input value={v} onChange={(e) => {
                const next = outline.slice()
                next[i] = { ...row, episode_title: e.target.value }
                setOutline(next)
              }} />,
            },
            {
              title: '梗概',
              dataIndex: 'episode_synopsis',
              render: (v, row, i) => <Input value={v} onChange={(e) => {
                const next = outline.slice()
                next[i] = { ...row, episode_synopsis: e.target.value }
                setOutline(next)
              }} />,
            },
            {
              title: '钩子',
              dataIndex: 'ending_hook',
              render: (v, row, i) => <Input value={v} onChange={(e) => {
                const next = outline.slice()
                next[i] = { ...row, ending_hook: e.target.value }
                setOutline(next)
              }} />,
            },
          ]}
        />
      </Card>
      <Card title={`第${work.episode_list?.find((e) => e.episode_id === episodeId)?.episode_no || ''}集剧本`} extra={<Space>
        <Select
          placeholder="文本模型"
          value={textModelCode}
          onChange={setTextModelCode}
          style={{ width: 220 }}
          options={textModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
        />
        <Button onClick={runScript}>生成剧本</Button>
        <Button onClick={saveScript}>保存</Button>
        <Button type="primary" onClick={() => ready(0)}>定稿</Button>
        <Button onClick={() => ready(1)}>强制定稿</Button>
      </Space>}>
        <Paragraph type="secondary">
          状态 {script?.script_status || '-'} / 版本 {script?.script_version || 0} / 节奏总分 {rhythm.total_score ?? script?.total_score ?? '-'}
        </Paragraph>
        <Collapse
          items={scenes.map((scene, si) => ({
            key: String(scene.scene_no || si),
            label: `场 ${scene.scene_no} ${scene.scene_location || ''} ${scene.scene_summary || ''}`,
            children: (scene.line_list || []).map((line, li) => (
              <Input.TextArea
                key={`${si}-${li}`}
                style={{ marginBottom: 8 }}
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
            )),
          }))}
        />
      </Card>
    </Space>
  )
}

function CharacterPane({ work }) {
  const [chars, setChars] = useState([])
  const [scenes, setScenes] = useState([])
  const [props, setProps] = useState([])
  const [imageModels, setImageModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [form] = Form.useForm()
  const [propForm] = Form.useForm()

  const load = useCallback(async () => {
    const { data } = await productionApi.consistencyPanel(work.work_id)
    setChars(data?.result?.character_list || [])
    setScenes(data?.result?.scene_list || [])
    setProps(data?.result?.prop_list || [])
  }, [work])

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
        {chars.map((row) => (
          <Card key={row.character_id} size="small" style={{ marginBottom: 8 }} title={`${row.character_name} (${row.card_status}) · ${row.used_shot_count || 0} 镜`} extra={<Space>
            <Button size="small" onClick={async () => {
              const { data } = await productionApi.portraitCharacter(row.character_id, imageModelCode)
              if (!data?.success) return
              const done = await waitTask(work.work_id, data.result.task_id)
              if (done && done.task_status !== 'SUCCEEDED') {
                message.error(`定妆失败：${done.fail_reason || done.fail_reason_code || '未知原因'}`)
              }
              load()
            }}>定妆</Button>
          </Space>}>
            <Space wrap>
              {(row.candidate_list || []).map((c) => (
                <div key={c.asset_id} style={{ textAlign: 'center' }}>
                  <img alt="" src={c.file_url} width={72} height={128} style={{ objectFit: 'cover', background: '#111' }} />
                  <div>
                    <Button size="small" type={row.ref_image_asset_id === c.asset_id ? 'primary' : 'default'} onClick={async () => {
                      await productionApi.lockCharacter(row.character_id, c.asset_id)
                      load()
                    }}>锁定</Button>
                  </div>
                </div>
              ))}
            </Space>
          </Card>
        ))}
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
        {scenes.map((row) => (
          <Card key={row.scene_card_id} size="small" style={{ marginBottom: 8 }} title={`${row.card_name} (${row.card_status}) · ${row.used_shot_count || 0} 镜`} extra={
            <Button size="small" onClick={async () => {
              const { data } = await productionApi.portraitScene(row.scene_card_id, imageModelCode)
              await waitTask(work.work_id, data.result.task_id)
              load()
            }}>生成参考图</Button>
          }>
            <Space wrap>
              {(row.candidate_list || []).map((c) => (
                <Button key={c.asset_id} size="small" onClick={async () => {
                  await productionApi.lockScene(row.scene_card_id, c.asset_id)
                  load()
                }}>锁定 {c.asset_id.slice(-6)}</Button>
              ))}
            </Space>
          </Card>
        ))}
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
        {props.map((row) => (
          <Card key={row.scene_card_id} size="small" style={{ marginBottom: 8 }} title={`${row.card_name} (${row.card_status}) · ${row.used_shot_count || 0} 镜`} extra={
            <Button size="small" onClick={async () => {
              const { data } = await productionApi.portraitScene(row.scene_card_id, imageModelCode)
              await waitTask(work.work_id, data.result.task_id)
              load()
            }}>生成参考图</Button>
          }>
            <Space wrap>
              {(row.candidate_list || []).map((c) => (
                <Button key={c.asset_id} size="small" onClick={async () => {
                  await productionApi.lockScene(row.scene_card_id, c.asset_id)
                  load()
                }}>锁定 {c.asset_id.slice(-6)}</Button>
              ))}
            </Space>
          </Card>
        ))}
      </Card>
    </Space>
  )
}

function modelLabel(row) { return modelPricingLabel(row) }

function StoryboardPane({ work, episodeId }) {
  const [canvasView, setCanvasView] = useState(false)
  const [rows, setRows] = useState([])
  const [selected, setSelected] = useState([])
  const [cost, setCost] = useState(null)
  const [drafts, setDrafts] = useState({})
  const [imageModels, setImageModels] = useState([])
  const [videoModels, setVideoModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [videoModelCode, setVideoModelCode] = useState()
  const [panel, setPanel] = useState({ character_list: [], scene_list: [], prop_list: [] })
  const [bindChars, setBindChars] = useState([])
  const [bindScene, setBindScene] = useState()
  const [bindProps, setBindProps] = useState([])

  const load = useCallback(async () => {
    if (!episodeId) return
    const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 })
    setRows(Array.isArray(data?.result) ? data.result : [])
    if (work?.work_id) {
      const panelRes = await productionApi.consistencyPanel(work.work_id)
      setPanel(panelRes.data?.result || { character_list: [], scene_list: [], prop_list: [] })
    }
  }, [episodeId, work])

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

  const patchRow = (shotId, fields) => {
    setRows((cur) => cur.map((r) => (r.shot_id === shotId ? { ...r, ...fields } : r)))
  }

  const selectedIds = selected.length ? selected : undefined

  if (canvasView) {
    return <StoryboardCanvas episodeId={episodeId} onSwitchToList={() => setCanvasView(false)} />
  }

  const submitLevel = async (taskType, autoNext) => {
    const estimateBody = {
      episode_id: episodeId,
      shot_ids: selectedIds,
      task_type: taskType,
      auto_next_flag: autoNext ? 1 : 0,
      model_code: taskType === 'VIDEO' ? videoModelCode : imageModelCode,
      next_model_code: autoNext ? videoModelCode : undefined,
    }
    const { data: estRes } = await productionApi.estimateTasks(estimateBody)
    if (!estRes?.success) return
    setCost(estRes.result)
    const ok = await confirmEstimate(estRes.result, taskType === 'VIDEO' ? '确认视频费用' : '确认出图费用')
    if (!ok) return
    const { data } = await productionApi.submitTasks(estimateBody)
    if (data?.success) {
      message.success(taskType === 'VIDEO' ? '视频已提交，可去任务页等结果' : '草图已提交，出图后先看构图对不对')
      await waitBatch(work.work_id, data.result.batch_id, data.result.task_id_list)
      load()
    }
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="先定妆、再写镜、再出图、再出视频"
        description="分镜会自动挂上角色/场景/道具。右侧面板可改绑。引用未定妆卡时提交会被拦截。定妆变更会标「定妆已变」。视频必须先有首帧。成片播放在 Media，这里只做出片。"
      />
      <Card size="small" title="资产一致性" extra={<Text type="secondary">勾选镜头后绑定，减少跨镜换脸</Text>}>
        <Space wrap>
          <Select
            mode="multiple"
            allowClear
            placeholder="角色"
            style={{ minWidth: 180 }}
            value={bindChars}
            onChange={setBindChars}
            options={(panel.character_list || []).map((c) => ({
              value: c.character_id,
              label: `${c.character_name} ${c.card_status}`,
            }))}
          />
          <Select
            allowClear
            placeholder="场景"
            style={{ minWidth: 160 }}
            value={bindScene}
            onChange={setBindScene}
            options={(panel.scene_list || []).map((c) => ({
              value: c.scene_card_id,
              label: `${c.card_name} ${c.card_status}`,
            }))}
          />
          <Select
            mode="multiple"
            allowClear
            placeholder="道具"
            style={{ minWidth: 160 }}
            value={bindProps}
            onChange={setBindProps}
            options={(panel.prop_list || []).map((c) => ({
              value: c.scene_card_id,
              label: `${c.card_name} ${c.card_status}`,
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
      </Card>
      <Card size="small" extra={<Space wrap>
        <Button onClick={() => setCanvasView(true)}>切到画布</Button>
        <Button onClick={async () => {
          await productionApi.splitShots(episodeId)
          await featuresApi.previzGenerateText({ episode_id: episodeId })
          message.success('已拆镜，并写好人话分镜')
          load()
        }}>从剧本拆镜</Button>
        <Button onClick={async () => {
          await featuresApi.previzGenerateText({ episode_id: episodeId, shot_id_list: selectedIds })
          message.success('已用人话写清每一镜')
          load()
        }}>生成人话分镜（免费）</Button>
        <Select
          placeholder="出图模型"
          value={imageModelCode}
          onChange={setImageModelCode}
          style={{ width: 220 }}
          options={imageModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
        />
        <Select
          placeholder="视频模型"
          value={videoModelCode}
          onChange={setVideoModelCode}
          style={{ width: 220 }}
          options={videoModels.map((m) => ({ value: m.model_code, label: modelLabel(m) }))}
        />
        <Button onClick={async () => {
          const { data } = await productionApi.estimateTasks({
            episode_id: episodeId,
            shot_ids: selectedIds,
            task_type: 'IMAGE',
            auto_next_flag: 0,
            model_code: imageModelCode,
          })
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
        <Button type="primary" onClick={() => submitLevel('VIDEO', 0)}>构图OK，生成视频</Button>
      </Space>}>
        {cost ? (
          <Paragraph style={{ marginBottom: 0 }}>
            约 {cost.task_count} 镜 / {formatYuan(cost.estimated_cost_cent)} 元
            （余额 {formatYuan(cost.available_cent)} 元 · {cost.model_code || '-'}）
            {(cost.shot_cost_list || []).length > 0
              ? ` · 明细 ${(cost.shot_cost_list || []).slice(0, 5).map((s) => `镜${s.shot_no}:${formatYuan(s.estimated_cost_cent)}`).join('、')}`
              : ''}
          </Paragraph>
        ) : (
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            勾选镜头后估费用。生成前会再弹窗确认预计费用与余额。
          </Paragraph>
        )}
      </Card>
      {rows.map((row) => (
        <Card
          key={row.shot_id}
          size="small"
          title={`镜 ${row.shot_no} · ${row.shot_duration_sec || 3}秒 · ${row.shot_status || 'PENDING'}`}
          extra={<Space>
            {row.latest_task ? (
              <Tag color={row.latest_task.task_status === 'FAILED' ? 'red' : row.latest_task.task_status === 'SUCCEEDED' ? 'green' : 'blue'}>
                {row.latest_task.task_type_name || row.latest_task.task_type} {row.latest_task.task_status_name || row.latest_task.task_status}
              </Tag>
            ) : null}
            {row.stale_flag === 1 ? <Tag color="orange">定妆已变，建议重抽</Tag> : null}
            {row.blocked_flag === 1 ? <Tag color="red">未就绪</Tag> : null}
            <Tag color={(row.prompt_score || 0) >= 70 ? 'green' : 'orange'}>提示词 {row.prompt_score ?? '-'} 分</Tag>
            <Select
              size="small"
              style={{ width: 140 }}
              value={row.frame_link_mode || 'NONE'}
              onChange={async (mode) => {
                await featuresApi.frameLinkSave({
                  shot_id: row.shot_id,
                  frame_link_mode: mode,
                  model_code: videoModelCode,
                })
                load()
              }}
              options={[
                { value: 'NONE', label: '不衔接' },
                { value: 'INHERIT_PREV', label: '接上一镜' },
                { value: 'BOOKEND', label: '首尾帧' },
              ]}
            />
            {row.link_hint
              ? <Tag color={row.link_effective_flag === 1 ? 'blue' : 'orange'}>{row.link_hint}</Tag>
              : null}
            <Button size="small" onClick={() => {
              setSelected((cur) => cur.includes(row.shot_id) ? cur.filter((id) => id !== row.shot_id) : cur.concat(row.shot_id))
            }}>{selected.includes(row.shot_id) ? '取消勾选' : '勾选'}</Button>
            {row.shot_status === 'LOCKED'
              ? <Button size="small" onClick={async () => { await productionApi.unlockShot(row.shot_id); load() }}>解锁</Button>
              : <Button size="small" onClick={async () => { await productionApi.lockShot(row.shot_id); load() }}>锁定</Button>}
          </Space>}
        >
          <Space align="start" size="large" style={{ width: '100%' }}>
            <div style={{ width: 90, flexShrink: 0 }}>
              {row.first_frame_url
                ? <img alt="" src={row.first_frame_url} width={90} style={{ borderRadius: 8, display: 'block' }} />
                : <div style={{ width: 90, height: 160, background: '#f1f5f9', borderRadius: 8, padding: 8, fontSize: 12, color: '#64748b' }}>还没出图</div>}
              {row.last_frame_url ? (
                <div style={{ marginTop: 6 }}>
                  <img alt="尾帧" src={row.last_frame_url} width={90} style={{ borderRadius: 8, display: 'block' }} />
                  <Text type="secondary" style={{ fontSize: 11 }}>尾帧</Text>
                </div>
              ) : null}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <Paragraph style={{ marginBottom: 8 }}>
                <Text strong>这一镜会拍成：</Text>
                {row.look_plain || '先写人话分镜，系统会告诉你观众将看到什么。'}
              </Paragraph>
              <Space wrap size={[4, 4]} style={{ marginBottom: 8 }}>
                {(row.character_ref_json || []).map((id) => {
                  const card = (panel.character_list || []).find((c) => c.character_id === id)
                  return <Tag key={id}>{card?.character_name || id}</Tag>
                })}
                {row.scene_card_id ? (
                  <Tag color="blue">{(panel.scene_list || []).find((c) => c.scene_card_id === row.scene_card_id)?.card_name || row.scene_card_id}</Tag>
                ) : null}
                {(row.prop_ref_json || []).map((id) => {
                  const card = (panel.prop_list || []).find((c) => c.scene_card_id === id)
                  return <Tag key={id} color="gold">{card?.card_name || id}</Tag>
                })}
              </Space>
              <div style={{ marginTop: 6, marginBottom: 4 }}>
                <Space wrap size={[4, 4]}>
                  <Text type="secondary" style={{ fontSize: 12 }}>🎥 运镜基调：</Text>
                  {CAMERA_MOVEMENTS.slice(1, 6).map((cm) => (
                    <Tag
                      key={cm.value}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setDrafts((cur) => {
                          const old = cur[row.shot_id] ?? row.previz_text ?? ''
                          return { ...cur, [row.shot_id]: old ? `${old}，【运镜：${cm.label}】` : `【运镜：${cm.label}】` }
                        })
                      }}
                    >
                      {cm.label.split(' ')[0]}
                    </Tag>
                  ))}
                  {DRAMA_STYLES.slice(0, 3).map((st) => (
                    <Tag
                      key={st.key}
                      color="purple"
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setDrafts((cur) => {
                          const old = cur[row.shot_id] ?? row.previz_text ?? ''
                          return { ...cur, [row.shot_id]: old ? `${old}，【基调：${st.label}】` : `【基调：${st.label}】` }
                        })
                      }}
                    >
                      {st.label.split(' ')[0]}
                    </Tag>
                  ))}
                  {QUAD_GRID_ANGLES.map((qa) => (
                    <Tag
                      key={qa.value}
                      color="blue"
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setDrafts((cur) => {
                          const old = cur[row.shot_id] ?? row.previz_text ?? ''
                          return { ...cur, [row.shot_id]: old ? `${old}，【机位：${qa.label.split(' ')[0]}】` : `【机位：${qa.label.split(' ')[0]}】` }
                        })
                      }}
                    >
                      {qa.label.split(' ')[0]}
                    </Tag>
                  ))}
                  {CONTINUITY_PRESETS.map((cp) => (
                    <Tag
                      key={cp.value}
                      color="green"
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setDrafts((cur) => {
                          const old = cur[row.shot_id] ?? row.previz_text ?? ''
                          return { ...cur, [row.shot_id]: old ? `${old}，【衔接：${cp.label}】` : `【衔接：${cp.label}】` }
                        })
                      }}
                    >
                      {cp.label}
                    </Tag>
                  ))}
                </Space>
              </div>
              <Text type="secondary">人话分镜（改这里免费，不用写提示词）</Text>
              <TextArea
                rows={4}
                style={{ marginTop: 6 }}
                value={drafts[row.shot_id] ?? row.previz_text ?? ''}
                onChange={(e) => setDrafts((cur) => ({ ...cur, [row.shot_id]: e.target.value }))}
              />
              <Space style={{ marginTop: 8 }} wrap>
                <Button size="small" onClick={async () => {
                  const text = drafts[row.shot_id] ?? row.previz_text
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
                  const body = {
                    episode_id: episodeId,
                    shot_ids: [row.shot_id],
                    task_type: 'IMAGE',
                    auto_next_flag: 0,
                    model_code: imageModelCode,
                  }
                  const { data: estRes } = await productionApi.estimateTasks(body)
                  if (!estRes?.success) return
                  const ok = await confirmEstimate(estRes.result, '确认出图费用')
                  if (!ok) return
                  const { data } = await productionApi.submitTasks(body)
                  if (data?.success) {
                    message.success('正在出图确认构图')
                    await waitTask(work.work_id, data.result.task_id_list?.[0])
                    load()
                  }
                }}>出一张图看看</Button>
                <Button size="small" type="primary" disabled={!row.first_frame_asset_id || row.shot_status === 'LOCKED'} onClick={async () => {
                  const body = {
                    episode_id: episodeId,
                    shot_ids: [row.shot_id],
                    task_type: 'VIDEO',
                    auto_next_flag: 0,
                    model_code: videoModelCode,
                  }
                  const { data: estRes } = await productionApi.estimateTasks(body)
                  if (!estRes?.success) return
                  const ok = await confirmEstimate(estRes.result, '确认视频费用')
                  if (!ok) return
                  const { data } = await productionApi.submitTasks(body)
                  if (data?.success) {
                    message.success('构图已确认，开始生成视频')
                    await waitTask(work.work_id, data.result.task_id_list?.[0])
                    load()
                  }
                }}>生成视频</Button>
              </Space>
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
      ))}
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
          { title: '目标', dataIndex: 'target_type', width: 110, render: (v) => v || '-' },
          { title: '状态', dataIndex: 'task_status_name', width: 110, render: (v, row) => <Tag>{v || row.task_status}</Tag> },
          { title: '进度', dataIndex: 'progress_percent', width: 80 },
          { title: '费用(分)', dataIndex: 'cost_cent', width: 90 },
          { title: '失败', dataIndex: 'fail_reason' },
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
  const msg = data?.message || fallback || '发布失败'
  if (code === '4006' || /READY|APPROVED|过期|未就绪|审核/.test(msg)) {
    return { title: '无法发布（闸门未通过）', detail: msg }
  }
  if (code === '502000' || /不可达|不可用|连接失败|Media 服务/.test(msg)) {
    return { title: 'Media 服务不可用', detail: msg }
  }
  if (code === '504000' || /超时/.test(msg)) {
    return { title: 'Media 请求超时', detail: msg }
  }
  if (/6\d{4}/.test(msg) || /^6\d{4}$/.test(code)) {
    return { title: 'Media 业务拒绝', detail: msg }
  }
  if (/未配置|base-url|ingest-token/.test(msg)) {
    return { title: 'Media 配置缺失', detail: msg }
  }
  return { title: '发布失败', detail: msg }
}

function showPublishSuccess(result) {
  const mediaId = result?.media_id || ''
  const playUrl = result?.play_url || ''
  const replayed = !!result?.replayed
  const pending = result?.status === 'PENDING' || result?.commercial_ready === false
  if (pending && result?.status !== 'READY') {
    const note = result?.publish_log?.error || 'Media 已受理，素材尚未验证为可播放。这不是发布成功。'
    Modal.info({
      title: 'Media 已受理，尚未可播放',
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
    title: replayed ? '已发布到 Media（幂等回放，未新建目录）' : '已发布到 Media',
    width: 520,
    content: (
      <div>
        <Paragraph style={{ marginBottom: 8 }}>
          <Text type="secondary">media_id</Text>
          <br />
          <Text copyable={{ text: mediaId }}>{mediaId || '-'}</Text>
        </Paragraph>
        <Paragraph style={{ marginBottom: 12 }}>
          <Text type="secondary">play_url</Text>
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
                message.success('已复制 media_id')
              } catch {
                message.warning('复制失败，请手动选择')
              }
            }}>复制 media_id</Button>
          ) : null}
          {playUrl ? (
            <Button onClick={async () => {
              try {
                await navigator.clipboard.writeText(playUrl)
                message.success('已复制 play_url')
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

function ComposePane({ work, episodeId, onReload }) {
  const [precheck, setPrecheck] = useState(null)
  const [film, setFilm] = useState(null)
  const [mediaPreflight, setMediaPreflight] = useState(null)
  const [publishing, setPublishing] = useState(false)
  const [batchPublishing, setBatchPublishing] = useState(false)
  const [batchProgress, setBatchProgress] = useState(null)
  const [publishError, setPublishError] = useState(null)
  const [composeCost, setComposeCost] = useState(null)
  const [musicModels, setMusicModels] = useState([])
  const [musicModelCode, setMusicModelCode] = useState()
  const [musicPrompt, setMusicPrompt] = useState('电影感纯音乐，情绪递进，适合作为短剧背景音乐')

  const load = useCallback(async () => {
    if (!episodeId) return
    const [p, f, mp] = await Promise.all([
      productionApi.filmPrecheck(episodeId),
      productionApi.filmDetail(episodeId),
      productionApi.publishFilmToMediaPreflight({ episode_id: episodeId, work_id: work?.work_id }),
    ])
    setPrecheck(p.data?.result)
    setFilm(f.data?.result)
    setMediaPreflight(mp.data?.result || null)
  }, [episodeId, work?.work_id])

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

  const canPublish = !!film?.film_id
    && film?.compose_status === 'READY'
    && film?.expired_flag !== 1
    && mediaPreflight?.config_ready !== false
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
        title: '批量发布到 Media',
        content: `将发布本作品全部 READY 且已过审的成片（约 ${count} 集）。确认继续？`,
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
    <Card title="成片与提审">
      <Paragraph>合成状态 {film?.compose_status || '-'} / 预检 {precheck?.ready ? '可合成' : '未就绪'} / 风险 {precheck?.risk_level || '-'}{film?.expired_flag === 1 ? ' / 成片已过期' : ''}</Paragraph>
      {film?.expired_flag === 1 ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="成片已过期"
          description="镜头重抽或配音重配后，旧成片不能下载，也不能再回放上次的 Media 目录。请重新合成并通过审核后再发布。"
        />
      ) : null}
      {(precheck?.blocker_list || []).length ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="合成前置未满足"
          description={(precheck.blocker_list || []).map((b) => `${b.blocker_desc}（${b.count || 0}）`).join('；')}
        />
      ) : null}
      {mediaPreflight && mediaPreflight.config_ready === false ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="Media 发布预检：配置未就绪"
          description={(mediaPreflight.warnings || []).join('；')
            || '请配置 bgssai.media.base-url / ingest-token（或 sys_secret）后再发布'}
        />
      ) : null}
      {mediaPreflight && mediaPreflight.config_ready && mediaPreflight.publishable === false && mediaPreflight.gate_reason ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="当前成片暂不可发布到 Media"
          description={mediaPreflight.gate_reason}
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
        <Button type="primary" disabled={!precheck?.ready} onClick={async () => {
          const { data: estRes } = await productionApi.composeEstimate(episodeId)
          if (!estRes?.success) return
          setComposeCost(estRes.result)
          const ok = await confirmEstimate(estRes.result, '确认合成费用')
          if (!ok) return
          const { data } = await productionApi.composeFilm(episodeId)
          if (data?.success) {
            message.success('合成任务已提交（有配音会混入成片）')
            await waitTask(work.work_id, data.result.task_id)
            await load()
            await onReload()
          }
        }}>合成</Button>
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
        >发布到 Media</Button>
        <Button
          loading={batchPublishing}
          disabled={batchPublishing || publishing || mediaPreflight?.config_ready === false
            || !(mediaPreflight?.batch_ready_count > 0)}
          onClick={runBatchPublish}
        >
          批量发布 READY 集
          {mediaPreflight?.batch_ready_count != null ? `（${mediaPreflight.batch_ready_count}）` : ''}
        </Button>
        <Button onClick={async () => {
          await load()
          message.success('已刷新发布状态')
        }}>刷新发布状态</Button>
      </Space>
      {batchProgress ? (
        <Alert
          type="info"
          showIcon
          style={{ marginTop: 12 }}
          message={`批量发布进度 ${batchProgress.progress_percent ?? 0}% · 可播放 ${batchProgress.succeeded || 0} / 已受理未就绪 ${batchProgress.pending || 0} / 失败 ${batchProgress.failed || 0} / 共 ${batchProgress.total || 0}`}
          description={(batchProgress.item_list || []).slice(0, 12).map((it) => (
            <div key={`${it.episode_id}-${it.film_id}`}>
              {it.progress_index}/{it.progress_total} {it.episode_id} · {it.ok ? (it.status === 'PENDING' ? `已受理未就绪 ${it.media_id || ''}` : `可播放 ${it.media_id || ''}`) : `失败 ${it.error || ''}`}
            </div>
          ))}
        />
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
          type={publishLog.status === 'FAILED' ? 'error' : publishLog.status === 'PENDING' ? 'info' : (publishLog.last_error ? 'warning' : 'success')}
          showIcon
          style={{ marginTop: 12 }}
          message={`最近一次发布 · ${publishLog.status || '-'} · ${publishLog.media_id || '无 media_id'}`}
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
                        message.warning('复制失败')
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
  const [work, setWork] = useState(null)
  const [episodeId, setEpisodeId] = useState()
  const [assistantOpen, setAssistantOpen] = useState(false)

  const load = useCallback(async () => {
    const { data } = await productionApi.getWork(workId)
    const detail = data?.result
    setWork(detail)
    setEpisodeId((prev) => prev || detail?.episode_list?.[0]?.episode_id)
  }, [workId])

  useEffect(() => { load() }, [load])

  const active = tab || 'script'
  const episodes = work?.episode_list || []
  const current = useMemo(() => episodes.find((e) => e.episode_id === episodeId), [episodes, episodeId])

  if (!work) return <Card loading />

  return (
    <div>
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
          <Button
            type="primary"
            style={{ background: 'linear-gradient(135deg, #722ed1, #1677ff)', borderColor: 'transparent' }}
            onClick={() => setAssistantOpen(true)}
          >
            🎬 AI协同专家
          </Button>
          <Button
            onClick={() => navigate(`/works/${workId}/edit-export`)}
          >
            📦 导出剪辑工程 (剪映/FCP/EDL)
          </Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 12 }}>
        <span>当前剧集</span>
        <Select
          style={{ minWidth: 240 }}
          value={episodeId}
          onChange={setEpisodeId}
          options={episodes.map((ep) => ({
            value: ep.episode_id,
            label: `第${ep.episode_no}集 ${ep.episode_title || ''} (${ep.script_status}/${ep.review_status})`,
          }))}
        />
        {current ? <Tag>{current.compose_status}</Tag> : null}
      </Space>
      <Tabs
        activeKey={active}
        onChange={(key) => navigate(`/works/${workId}/${key}`)}
        items={[
          { key: 'source', label: '原著导入', children: <SourceImportPage work={work} /> },
          { key: 'script', label: '剧本', children: <ScriptPane work={work} episodeId={episodeId} onReload={load} /> },
          { key: 'characters', label: '资产一致性', children: <CharacterPane work={work} /> },
          { key: 'storyboard', label: '创作台', children: <StoryboardPane work={work} episodeId={episodeId} /> },
          { key: 'voice', label: '配音字幕', children: <VoiceStudioPage work={work} episodeId={episodeId} /> },
          { key: 'story-map', label: '剧情图谱', children: <StoryMapPage work={work} episodeId={episodeId} /> },
          { key: 'quality', label: '质量报告', children: <QualityReportPage work={work} episodeId={episodeId} /> },
          { key: 'preview', label: '三问预览', children: <EpisodePreviewPage work={work} episodeId={episodeId} /> },
          { key: 'continuity', label: '连贯性', children: <ContinuityCenterPage work={work} /> },
          { key: 'tasks', label: '本剧任务', children: <TasksPane workId={work.work_id} episodeId={episodeId} /> },
          { key: 'compose', label: '成片提审', children: <ComposePane work={work} episodeId={episodeId} onReload={load} /> },
          { key: 'edit-export', label: '剪辑工程', children: <EditExportPage episodeId={episodeId} /> },
        ]}
      />
      <DramaAssistantDrawer
        visible={assistantOpen}
        onClose={() => setAssistantOpen(false)}
      />
    </div>
  )
}
