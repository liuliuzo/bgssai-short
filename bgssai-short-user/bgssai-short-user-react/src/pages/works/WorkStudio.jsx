import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Collapse, Form, Input, Select, Space, Table, Tabs, Tag, Typography, message } from 'antd'
import { Link, useNavigate, useParams } from 'react-router-dom'
import productionApi from '../../api/production'
import VoiceStudioPage from './VoiceStudioPage'
import StoryMapPage from './StoryMapPage'
import QualityReportPage from './QualityReportPage'
import EpisodePreviewPage from './EpisodePreviewPage'
import ContinuityCenterPage from './ContinuityCenterPage'

const { Title, Paragraph, Text } = Typography

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

  const runOutline = async () => {
    const { data } = await productionApi.generateOutline(work.work_id)
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
    const { data } = await productionApi.generateScript(episodeId)
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

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card title="角色卡" extra={<Button onClick={load}>刷新</Button>}>
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
              const { data } = await productionApi.portraitCharacter(row.character_id)
              await waitTask(work.work_id, data.result.task_id)
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
              const { data } = await productionApi.portraitScene(row.scene_card_id)
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

function StoryboardPane({ work, episodeId }) {
  const [rows, setRows] = useState([])
  const [selected, setSelected] = useState([])
  const [cost, setCost] = useState(null)
  const [modelCode, setModelCode] = useState()

  const load = useCallback(async () => {
    if (!episodeId) return
    const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 100 })
    setRows(Array.isArray(data?.result) ? data.result : [])
  }, [episodeId])

  useEffect(() => { load() }, [load])

  return (
    <Card title="分镜" extra={<Space>
      <Input
        placeholder="model_code 可选"
        value={modelCode}
        onChange={(e) => setModelCode(e.target.value || undefined)}
        style={{ width: 160 }}
        allowClear
      />
      <Button onClick={async () => {
        await productionApi.splitShots(episodeId)
        message.success('已拆镜')
        load()
      }}>从剧本拆镜</Button>
      <Button onClick={async () => {
        const { data } = await productionApi.estimateTasks({
          episode_id: episodeId,
          shot_ids: selected,
          task_type: 'IMAGE',
          auto_next_flag: 1,
          model_code: modelCode,
        })
        setCost(data?.result)
      }}>估价</Button>
      <Button type="primary" onClick={async () => {
        const { data } = await productionApi.submitTasks({
          episode_id: episodeId,
          shot_ids: selected.length ? selected : undefined,
          task_type: 'IMAGE',
          auto_next_flag: 1,
          model_code: modelCode,
        })
        if (data?.success) {
          message.success(`已提交 ${data.result.task_id_list?.length || 0} 个任务`)
          await waitTask(work.work_id, data.result.task_id_list?.[0])
          load()
        }
      }}>提交生成</Button>
    </Space>}>
      {cost ? (
        <Paragraph>
          约 {cost.task_count} 条 / {((cost.estimated_cost_cent || 0) / 100).toFixed(2)} 元
          （模型 {cost.model_code || '-'} · 可先做 {cost.affordable_episode_count ?? 0} 集）
        </Paragraph>
      ) : null}
      <Table
        rowKey="shot_id"
        size="small"
        dataSource={rows}
        rowSelection={{ selectedRowKeys: selected, onChange: setSelected }}
        pagination={false}
        columns={[
          { title: '镜', dataIndex: 'shot_no', width: 60 },
          { title: '状态', dataIndex: 'shot_status', width: 120 },
          { title: '描述', dataIndex: 'shot_desc' },
          { title: '对白', dataIndex: 'dialogue_text' },
          {
            title: '首帧',
            width: 80,
            render: (_, row) => row.first_frame_url ? <img alt="" src={row.first_frame_url} width={36} height={64} /> : '-',
          },
          {
            title: '操作',
            width: 140,
            render: (_, row) => (
              <Space>
                {row.shot_status === 'LOCKED'
                  ? <Button size="small" onClick={async () => { await productionApi.unlockShot(row.shot_id); load() }}>解锁</Button>
                  : <Button size="small" onClick={async () => { await productionApi.lockShot(row.shot_id); load() }}>锁定</Button>}
              </Space>
            ),
          },
        ]}
      />
    </Card>
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

  return (
    <Card title="成片与提审">
      <Paragraph>合成状态 {film?.compose_status || '-'} / 预检 {precheck?.ready ? '可合成' : '未就绪'} / 风险 {precheck?.risk_level || '-'}</Paragraph>
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
          { key: 'script', label: '剧本', children: <ScriptPane work={work} episodeId={episodeId} onReload={load} /> },
          { key: 'characters', label: '角色与场景', children: <CharacterPane work={work} /> },
          { key: 'storyboard', label: '分镜生成', children: <StoryboardPane work={work} episodeId={episodeId} /> },
          { key: 'voice', label: '配音字幕', children: <VoiceStudioPage work={work} episodeId={episodeId} /> },
          { key: 'story-map', label: '剧情图谱', children: <StoryMapPage work={work} episodeId={episodeId} /> },
          { key: 'quality', label: '质量报告', children: <QualityReportPage work={work} episodeId={episodeId} /> },
          { key: 'preview', label: '三问预览', children: <EpisodePreviewPage work={work} episodeId={episodeId} /> },
          { key: 'continuity', label: '连贯性', children: <ContinuityCenterPage work={work} /> },
          { key: 'tasks', label: '本剧任务', children: <TasksPane workId={work.work_id} episodeId={episodeId} /> },
          { key: 'compose', label: '成片提审', children: <ComposePane work={work} episodeId={episodeId} onReload={load} /> },
        ]}
      />
    </div>
  )
}
