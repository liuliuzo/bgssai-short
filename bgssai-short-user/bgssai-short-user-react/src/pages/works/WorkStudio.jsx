import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Collapse, Form, Input, Select, Space, Table, Tabs, Tag, Typography, message } from 'antd'
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

const { Title, Paragraph, Text } = Typography
const { TextArea } = Input

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function waitTask(workId, taskId) {
  for (let i = 0; i < 40; i += 1) {
    const { data } = await productionApi.listTasks({ work_id: workId, page_num: 1, page_size: 50 })
    const row = (data?.result || []).find((item) => item.task_id === taskId)
    if (row && (row.task_status === 'SUCCEEDED' || row.task_status === 'FAILED' || row.task_status === 'CANCELED')) {
      return row
    }
    await sleep(1500)
  }
  return null
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
  const [imageModels, setImageModels] = useState([])
  const [imageModelCode, setImageModelCode] = useState()
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    const [c, s] = await Promise.all([
      productionApi.listCharacters(work.work_id),
      productionApi.listScenes(work.work_id),
    ])
    setChars(c.data?.result?.character_list || [])
    setScenes(s.data?.result?.scene_card_list || [])
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
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {chars.map((row) => (
          <Card key={row.character_id} size="small" style={{ marginBottom: 8 }} title={`${row.character_name} (${row.card_status})`} extra={<Space>
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
          await productionApi.createScene({ work_id: work.work_id, ...v })
          load()
        }}>
          <Form.Item name="card_name" rules={[{ required: true }]}><Input placeholder="场景名" /></Form.Item>
          <Form.Item><Button htmlType="submit">新增</Button></Form.Item>
        </Form>
        {scenes.map((row) => (
          <Card key={row.scene_card_id} size="small" style={{ marginBottom: 8 }} title={`${row.card_name} (${row.card_status})`} extra={
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

function modelLabel(row) {
  if (!row) return ''
  const price = ((Number(row.unit_price_cent) || 0) / 100).toFixed(2)
  const region = row.region_label || (row.region === 'intl' ? '国外' : '国内')
  return `${row.model_name} · ${region} · ${price}元`
}

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

  const load = useCallback(async () => {
    if (!episodeId) return
    const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 })
    setRows(Array.isArray(data?.result) ? data.result : [])
  }, [episodeId])

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
    const { data } = await productionApi.submitTasks({
      episode_id: episodeId,
      shot_ids: selectedIds,
      task_type: taskType,
      auto_next_flag: autoNext ? 1 : 0,
      model_code: taskType === 'VIDEO' ? videoModelCode : imageModelCode,
      next_model_code: autoNext ? videoModelCode : undefined,
    })
    if (data?.success) {
      message.success(taskType === 'VIDEO' ? '视频已提交，可去任务页等结果' : '草图已提交，出图后先看构图对不对')
      await waitTask(work.work_id, data.result.task_id_list?.[0])
      load()
    }
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="先看懂这一镜，再花钱生成"
        description="改人话分镜免费。出一张图只确认构图。构图没问题再生成视频。不要用视频去试错。"
      />
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
            约 {cost.task_count} 镜 / {((cost.estimated_cost_cent || 0) / 100).toFixed(2)} 元
            （{cost.model_code || '-'}）。这是出图费用，还不包含视频。
          </Paragraph>
        ) : (
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            勾选镜头后估费用。默认先出图、不自动出视频。
          </Paragraph>
        )}
      </Card>
      {rows.map((row) => (
        <Card
          key={row.shot_id}
          size="small"
          title={`镜 ${row.shot_no} · ${row.shot_duration_sec || 3}秒 · ${row.shot_status || 'PENDING'}`}
          extra={<Space>
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
                <Button size="small" onClick={async () => {
                  const { data } = await productionApi.submitTasks({
                    episode_id: episodeId,
                    shot_ids: [row.shot_id],
                    task_type: 'IMAGE',
                    auto_next_flag: 0,
                    model_code: imageModelCode,
                  })
                  if (data?.success) {
                    message.success('正在出图确认构图')
                    await waitTask(work.work_id, data.result.task_id_list?.[0])
                    load()
                  }
                }}>出一张图看看</Button>
                <Button size="small" type="primary" disabled={!row.first_frame_asset_id} onClick={async () => {
                  const { data } = await productionApi.submitTasks({
                    episode_id: episodeId,
                    shot_ids: [row.shot_id],
                    task_type: 'VIDEO',
                    auto_next_flag: 0,
                    model_code: videoModelCode,
                  })
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

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await productionApi.listTasks({
        work_id: workId,
        episode_id: episodeId || undefined,
        page_num: 1,
        page_size: 50,
      })
      setRows(Array.isArray(data?.result) ? data.result : [])
    } finally {
      setLoading(false)
    }
  }, [workId, episodeId])

  useEffect(() => {
    load()
    const timer = setInterval(load, 2500)
    return () => clearInterval(timer)
  }, [load])

  return (
    <Table
      rowKey="task_id"
      size="small"
      loading={loading}
      dataSource={rows}
      pagination={false}
      columns={[
        { title: '类型', dataIndex: 'task_type', width: 90 },
        { title: '状态', dataIndex: 'task_status', width: 110, render: (v) => <Tag>{v}</Tag> },
        { title: '进度', dataIndex: 'progress_percent', width: 80 },
        { title: '费用(分)', dataIndex: 'cost_cent', width: 90 },
        { title: '失败', dataIndex: 'fail_reason' },
        {
          title: '操作',
          width: 140,
          render: (_, row) => (
            <Space>
              {row.task_status === 'FAILED' ? <Button size="small" onClick={async () => { await productionApi.retryTask(row.task_id); load() }}>重试</Button> : null}
              {row.task_status === 'QUEUED' || row.task_status === 'RUNNING'
                ? <Button size="small" onClick={async () => { await productionApi.cancelTask(row.task_id); load() }}>取消</Button>
                : null}
            </Space>
          ),
        },
      ]}
    />
  )
}

function ComposePane({ work, episodeId, onReload }) {
  const [precheck, setPrecheck] = useState(null)
  const [film, setFilm] = useState(null)
  const [musicModels, setMusicModels] = useState([])
  const [musicModelCode, setMusicModelCode] = useState()
  const [musicPrompt, setMusicPrompt] = useState('电影感纯音乐，情绪递进，适合作为短剧背景音乐')

  const load = useCallback(async () => {
    if (!episodeId) return
    const [p, f] = await Promise.all([
      productionApi.filmPrecheck(episodeId),
      productionApi.filmDetail(episodeId),
    ])
    setPrecheck(p.data?.result)
    setFilm(f.data?.result)
  }, [episodeId])

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

  return (
    <Card title="成片与提审">
      <Paragraph>合成状态 {film?.compose_status || '-'} / 预检 {precheck?.ready ? '可合成' : '未就绪'} / 风险 {precheck?.risk_level || '-'}</Paragraph>
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
      <Space>
        <Button onClick={load}>刷新</Button>
        <Button type="primary" onClick={async () => {
          const { data } = await productionApi.composeFilm(episodeId)
          if (data?.success) {
            message.success('合成任务已提交')
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
      </Space>
      {film?.download_url ? (
        <Paragraph style={{ marginTop: 12 }}>
          成片预览 <a href={film.download_url} target="_blank" rel="noreferrer">{film.download_url}</a>
        </Paragraph>
      ) : null}
      {film?.film_asset_id ? <img alt="film" src={film.download_url} style={{ maxWidth: 180, marginTop: 12 }} /> : null}
    </Card>
  )
}

export default function WorkStudio() {
  const { workId, tab } = useParams()
  const navigate = useNavigate()
  const [work, setWork] = useState(null)
  const [episodeId, setEpisodeId] = useState()

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
      <Title level={4} style={{ marginTop: 0 }}>
        {work.work_title} <Text type="secondary">{work.work_status_name}</Text>
      </Title>
      <Paragraph>
        <Link to="/works">返回列表</Link>
        {' · '}
        {work.genre_name} · {work.episode_count} 集 · 已花费 {((work.cost_spent_cent || 0) / 100).toFixed(2)} 元
      </Paragraph>
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
          { key: 'characters', label: '角色与场景', children: <CharacterPane work={work} /> },
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
    </div>
  )
}
