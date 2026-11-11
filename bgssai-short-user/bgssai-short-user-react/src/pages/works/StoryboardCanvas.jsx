import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Drawer, Image, Input, Select, Space, Tag, Tooltip, Typography, message } from 'antd'
import productionApi from '../../api/production'
import featuresApi from '../../api/features'
import { SHOT_TYPES, CAMERA_MOVEMENTS, QUAD_GRID_ANGLES, CONTINUITY_PRESETS } from '../../utils/CinematographyHelper'
import { publicModelText } from '../../publicModel'
import { shotCanvasStatusKey } from './shotCardMedia'

const { Paragraph, Text } = Typography
const { TextArea } = Input

// 与列表视图同一套状态语义，不另发明一套颜色口径
const STATUS_STYLE = {
  PENDING: { background: '#f1f5f9', borderColor: '#94a3b8', label: '未开始' },
  TEXT_READY: { background: '#dbeafe', borderColor: '#2563eb', label: '文字稿' },
  DRAFT_READY: { background: '#ede9fe', borderColor: '#7c3aed', label: '草图' },
  IMAGE_GENERATING: { background: '#cffafe', borderColor: '#0891b2', label: '出图中' },
  IMAGE_READY: { background: '#cffafe', borderColor: '#0891b2', label: '首帧' },
  VIDEO_GENERATING: { background: '#dcfce7', borderColor: '#059669', label: '出视频' },
  VIDEO_READY: { background: '#dcfce7', borderColor: '#059669', label: '视频' },
  LOCKED: { background: '#bbf7d0', borderColor: '#047857', label: '已定稿' },
  FAILED: { background: '#fee2e2', borderColor: '#dc2626', label: '失败' },
}

function styleOf(row) {
  const key = shotCanvasStatusKey(row)
  return STATUS_STYLE[key] || STATUS_STYLE.PENDING
}

export default function StoryboardCanvas({ episodeId, onSwitchToList }) {
  const [rows, setRows] = useState([])
  const [selected, setSelected] = useState([])
  const [cost, setCost] = useState(null)
  const [dragging, setDragging] = useState(null)
  const [editing, setEditing] = useState(null)
  const [draft, setDraft] = useState('')
  const [batchTaskType, setBatchTaskType] = useState('IMAGE')
  const [submittingSingle, setSubmittingSingle] = useState(false)

  const load = useCallback(async () => {
    if (!episodeId) return
    const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 200 })
    setRows(Array.isArray(data?.result) ? data.result : [])
  }, [episodeId])

  useEffect(() => { load() }, [load])

  // 行 = 场次，列 = 镜号。位置全部由数据算出来，不存任何坐标
  const lanes = useMemo(() => {
    const grouped = new Map()
    rows.forEach((row) => {
      const key = row.scene_no ?? 0
      if (!grouped.has(key)) grouped.set(key, [])
      grouped.get(key).push(row)
    })
    return Array.from(grouped.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([sceneNo, shots]) => ({
        sceneNo,
        shots: shots.slice().sort((a, b) => (a.shot_no || 0) - (b.shot_no || 0)),
      }))
  }, [rows])

  const toggle = (shotId) => {
    setSelected((cur) => (cur.includes(shotId) ? cur.filter((id) => id !== shotId) : cur.concat(shotId)))
  }

  const selectLane = (lane) => {
    const ids = lane.shots.map((s) => s.shot_id)
    const allIn = ids.every((id) => selected.includes(id))
    setSelected((cur) => (allIn ? cur.filter((id) => !ids.includes(id)) : Array.from(new Set(cur.concat(ids)))))
  }

  const estimate = async () => {
    const { data } = await productionApi.estimateTasks({
      episode_id: episodeId,
      shot_ids: selected.length ? selected : undefined,
      task_type: batchTaskType,
      auto_next_flag: 0,
    })
    setCost(data?.result)
  }

  const submit = async () => {
    const { data } = await productionApi.submitTasks({
      episode_id: episodeId,
      shot_ids: selected.length ? selected : undefined,
      task_type: batchTaskType,
      auto_next_flag: 0,
    })
    if (data?.success) {
      message.success(`已提交 ${batchTaskType} 批量生成任务，去任务中心看进度`)
      setCost(null)
      load()
    }
  }

  const appendDraftTag = (tag) => {
    setDraft((cur) => (cur && cur.trim() ? `${cur.trim()}, ${tag}` : tag))
  }

  const runSingleShot = async (taskType) => {
    if (!editing) return
    setSubmittingSingle(true)
    try {
      const { data } = await productionApi.submitTasks({
        episode_id: episodeId,
        shot_ids: [editing.shot_id],
        task_type: taskType,
        auto_next_flag: 0,
      })
      if (data?.success) {
        message.success(`已提交第 ${editing.shot_no} 镜 ${taskType} 任务`)
        await load()
      }
    } catch (err) {
      message.error(err.message || '提交失败，请稍后重试')
    } finally {
      setSubmittingSingle(false)
    }
  }

  const lockCurrent = async () => {
    if (!editing) return
    const { data } = await productionApi.lockShot(editing.shot_id)
    if (data?.success) {
      message.success(`第 ${editing.shot_no} 镜已锁定`)
      setEditing((prev) => ({ ...prev, shot_status: 'LOCKED' }))
      load()
    }
  }

  const unlockCurrent = async () => {
    if (!editing) return
    const { data } = await productionApi.unlockShot(editing.shot_id)
    if (data?.success) {
      message.success(`第 ${editing.shot_no} 镜已解锁`)
      setEditing((prev) => ({ ...prev, shot_status: 'PENDING' }))
      load()
    }
  }

  const drop = async (lane, targetShot) => {
    if (!dragging || dragging.shot_id === targetShot.shot_id) return
    if ((dragging.scene_no ?? 0) !== (targetShot.scene_no ?? 0)) {
      message.warning('跨场拖不动：换场是剧本层的事，请回剧本改场次划分')
      setDragging(null)
      return
    }
    const ids = lane.shots.map((s) => s.shot_id).filter((id) => id !== dragging.shot_id)
    const at = ids.indexOf(targetShot.shot_id)
    ids.splice(at, 0, dragging.shot_id)
    setDragging(null)
    const { data } = await productionApi.reorderShots({
      episode_id: episodeId,
      scene_no: lane.sceneNo,
      shot_id_list: ids,
    })
    if (data?.success) {
      const stale = data.result?.stale_shot_no_list || []
      message.success(stale.length
        ? `顺序已改；镜 ${stale.join('、')} 的上一镜变了，可能接不上（未替你重抽）`
        : '顺序已改')
      load()
    }
  }

  const openEditor = (row) => {
    setEditing(row)
    setDraft(row.previz_text || '')
  }

  const saveEditor = async () => {
    await featuresApi.previzSave({ shot_id: editing.shot_id, previz_text: draft })
    message.success('已保存人话分镜提示词')
    setEditing(null)
    load()
  }

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="画布是第二视图，不是替代"
        description="改一镜的文字还是列表顺手；看整集到哪了、把一整场一起重跑，画布顺手。两个视图读写同一份数据、同一组接口，批量操作走的也是同一道费用预估。"
        action={<Button size="small" onClick={onSwitchToList}>切回列表</Button>}
      />

      <Card
        size="small"
        style={{ marginBottom: 12 }}
        title={`已选 ${selected.length} 镜`}
        extra={(
          <Space>
            <Button size="small" onClick={() => setSelected([])}>清空选择</Button>
            <Select
              size="small"
              value={batchTaskType}
              onChange={(val) => { setBatchTaskType(val); setCost(null) }}
              style={{ width: 110 }}
              options={[
                { value: 'IMAGE', label: '首帧出图' },
                { value: 'VIDEO', label: '生成视频' },
                { value: 'VOICE', label: '合成配音' },
              ]}
            />
            <Button size="small" onClick={estimate}>估费用</Button>
            <Button size="small" type="primary" disabled={!cost} onClick={submit}>批量生成</Button>
          </Space>
        )}
      >
        {cost ? (
          <Paragraph style={{ marginBottom: 0 }}>
            约 {cost.task_count} 镜 / {((cost.estimated_cost_cent || 0) / 100).toFixed(2)} 元
            （{publicModelText(cost.display_name || cost.model_code, batchTaskType) || '-'} · {batchTaskType}）。确认后提交，与列表视图走同一道预算闸门。
          </Paragraph>
        ) : (
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            点节点加选，或点左侧场次名选中整场。选完选择任务类型先估费用再提交。
          </Paragraph>
        )}
      </Card>

      <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 8, padding: 12 }}>
        {lanes.map((lane) => (
          <div
            key={lane.sceneNo}
            style={{
              display: 'flex', alignItems: 'center', gap: 8, padding: 8, marginBottom: 10,
              background: '#fff', border: '1px solid #eef2f7', borderRadius: 6, overflowX: 'auto',
            }}
          >
            <div
              style={{ width: 120, flexShrink: 0, fontSize: 12, color: '#64748b', cursor: 'pointer' }}
              onClick={() => selectLane(lane)}
            >
              场次 {lane.sceneNo || '-'}
              <div style={{ fontSize: 11 }}>{lane.shots.length} 镜 · 点选整场</div>
            </div>
            {lane.shots.map((row) => {
              const style = styleOf(row)
              const locked = row.shot_status === 'LOCKED'
              return (
                <Tooltip
                  key={row.shot_id}
                  title={`${style.label}${row.link_hint ? ` · ${row.link_hint}` : ''}${locked ? ' · 已锁定，拖不动' : ''}`}
                >
                  <div
                    draggable={!locked}
                    onDragStart={() => setDragging(row)}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={() => drop(lane, row)}
                    onClick={() => toggle(row.shot_id)}
                    onDoubleClick={() => openEditor(row)}
                    style={{
                      width: 96, flexShrink: 0, padding: 6, borderRadius: 6, fontSize: 11,
                      background: style.background,
                      border: `${row.stale_flag === 1 ? 2 : 1}px solid ${row.stale_flag === 1 ? '#d97706' : style.borderColor}`,
                      boxShadow: selected.includes(row.shot_id) ? 'inset 0 0 0 2px #2563eb' : 'none',
                      cursor: locked ? 'not-allowed' : 'grab',
                      opacity: locked ? 0.8 : 1,
                      position: 'relative',
                    }}
                  >
                    <div style={{
                      height: 60, borderRadius: 4, marginBottom: 4, overflow: 'hidden',
                      background: '#1e293b', display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: '#94a3b8', fontSize: 10, position: 'relative',
                    }}>
                      {row.video_url ? (
                        <video src={row.video_url} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : row.first_frame_url ? (
                        <img alt="" src={row.first_frame_url} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : (
                        style.label
                      )}
                      {row.video_url ? (
                        <div style={{ position: 'absolute', top: 2, right: 2, background: 'rgba(0,0,0,0.65)', padding: '1px 4px', borderRadius: 2, color: '#fff', fontSize: 9 }}>
                          视频
                        </div>
                      ) : null}
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 700 }}>镜 {row.shot_no}</span>
                      {locked ? <span>已锁</span> : null}
                    </div>
                    <div style={{ color: '#64748b', fontSize: 10, display: 'flex', justifyContent: 'space-between' }}>
                      <span>{row.shot_duration_sec || 3}s</span>
                      <span>{style.label}</span>
                    </div>
                  </div>
                </Tooltip>
              )
            })}
          </div>
        ))}
        {!lanes.length ? <Text type="secondary">本集还没有镜头，先去列表视图从剧本拆镜。</Text> : null}
        <Space wrap style={{ marginTop: 8, fontSize: 11 }}>
          {Object.entries(STATUS_STYLE).map(([key, value]) => (
            <Tag key={key} style={{ background: value.background, borderColor: value.borderColor }}>
              {value.label}
            </Tag>
          ))}
          <Tag style={{ background: '#dcfce7', borderColor: '#d97706', borderWidth: 2 }}>上游变了可能接不上</Tag>
        </Space>
      </div>

      <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 10 }}>
        单击选中、双击改文字与视听生成、拖动改顺序。<Text strong>拖动只改顺序</Text>——不删素材、不改状态、不触发重抽。
        跨场拖不动：那意味着这一镜换了场景，属于剧本层的事。
      </Paragraph>

      <Drawer
        open={!!editing}
        width={560}
        onClose={() => setEditing(null)}
        title={editing ? `镜 ${editing.shot_no} (场 ${editing.scene_no || 1}) · 分镜与视听生成` : ''}
        extra={(
          <Space>
            {editing?.shot_status === 'LOCKED' ? (
              <Button size="small" onClick={unlockCurrent}>解锁</Button>
            ) : (
              <Button size="small" onClick={lockCurrent}>锁定</Button>
            )}
            <Button type="primary" onClick={saveEditor}>保存提示词</Button>
          </Space>
        )}
      >
        {editing ? (
          <div>
            {editing.video_url ? (
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ display: 'block', marginBottom: 4 }}>视频成片预览：</Text>
                <video src={editing.video_url} controls style={{ width: '100%', maxHeight: 200, borderRadius: 6, background: '#000' }} />
              </div>
            ) : editing.first_frame_url ? (
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ display: 'block', marginBottom: 4 }}>首帧画面（点击放大）：</Text>
                <Image src={editing.first_frame_url} alt="首帧" style={{ width: '100%', maxHeight: 200, objectFit: 'cover', borderRadius: 6 }} preview={{ mask: '查看' }} />
              </div>
            ) : null}

            {editing.voice_url || editing.audio_url ? (
              <div style={{ marginBottom: 12, background: '#f0fdf4', padding: '6px 10px', borderRadius: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
                <Tag color="green">对白配音</Tag>
                <audio controls src={editing.voice_url || editing.audio_url} style={{ height: 26, flex: 1 }} />
              </div>
            ) : null}

            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 6, padding: 10, marginBottom: 12 }}>
              <div style={{ marginBottom: 4, display: 'flex', justifyContent: 'space-between' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>对白台词：</Text>
                <Text type="secondary" style={{ fontSize: 11 }}>时长 {editing.shot_duration_sec || 3} 秒</Text>
              </div>
              <div style={{ fontSize: 13, fontWeight: 500 }}>
                {editing.dialogue || '（纯动作环境音，无对白）'}
              </div>
            </div>

            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              镜头画面提示词（人话分镜）：
            </Text>
            <TextArea
              rows={4}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="描述镜头构图、机位视角与角色动作..."
              style={{ marginBottom: 12 }}
            />

            <div style={{ marginBottom: 10 }}>
              <Text strong style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                景别景深:
              </Text>
              <Space wrap size={[4, 4]}>
                {SHOT_TYPES.map((st) => (
                  <Button key={st.value} size="small" style={{ fontSize: 11 }} onClick={() => appendDraftTag(st.prompt)}>
                    + {st.label.split(' ')[0]}
                  </Button>
                ))}
              </Space>
            </div>

            <div style={{ marginBottom: 10 }}>
              <Text strong style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                运镜方式:
              </Text>
              <Space wrap size={[4, 4]}>
                {CAMERA_MOVEMENTS.slice(0, 8).map((cm) => (
                  <Button key={cm.value} size="small" style={{ fontSize: 11 }} onClick={() => appendDraftTag(cm.prompt)}>
                    + {cm.label.split(' ')[0]}
                  </Button>
                ))}
              </Space>
            </div>

            <div style={{ marginBottom: 10 }}>
              <Text strong style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                四机位视角:
              </Text>
              <Space wrap size={[4, 4]}>
                {QUAD_GRID_ANGLES.map((q) => (
                  <Button key={q.value} size="small" style={{ fontSize: 11 }} onClick={() => appendDraftTag(q.prompt)}>
                    + {q.label.split(' ')[0]}
                  </Button>
                ))}
              </Space>
            </div>

            <div style={{ marginBottom: 14 }}>
              <Text strong style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                连续性衔接:
              </Text>
              <Space wrap size={[4, 4]}>
                {CONTINUITY_PRESETS.map((cp) => (
                  <Button key={cp.value} size="small" style={{ fontSize: 11 }} onClick={() => appendDraftTag(cp.prompt)}>
                    + {cp.label}
                  </Button>
                ))}
              </Space>
            </div>

            <div style={{ borderTop: '1px solid #eef2f7', paddingTop: 12 }}>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>镜头独立派发生成：</Text>
              <Space wrap>
                <Button loading={submittingSingle} onClick={() => runSingleShot('IMAGE')}>出首帧图</Button>
                <Button loading={submittingSingle} onClick={() => runSingleShot('VIDEO')}>生成视频</Button>
                <Button loading={submittingSingle} onClick={() => runSingleShot('VOICE')}>合成对白</Button>
              </Space>
            </div>
          </div>
        ) : null}
      </Drawer>
    </div>
  )
}
