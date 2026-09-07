import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Drawer, Input, Space, Tag, Tooltip, Typography, message } from 'antd'
import productionApi from '../../api/production'
import featuresApi from '../../api/features'

const { Paragraph, Text } = Typography
const { TextArea } = Input

// 与列表视图同一套状态语义，不另发明一套颜色口径
const STATUS_STYLE = {
  PENDING: { background: '#f1f5f9', borderColor: '#94a3b8', label: '未开始' },
  TEXT_READY: { background: '#dbeafe', borderColor: '#2563eb', label: '文字稿' },
  DRAFT_READY: { background: '#ede9fe', borderColor: '#7c3aed', label: '草图' },
  IMAGE_READY: { background: '#cffafe', borderColor: '#0891b2', label: '首帧' },
  VIDEO_READY: { background: '#dcfce7', borderColor: '#059669', label: '视频' },
  LOCKED: { background: '#bbf7d0', borderColor: '#047857', label: '已定稿' },
  FAILED: { background: '#fee2e2', borderColor: '#dc2626', label: '失败' },
}

function styleOf(row) {
  const base = STATUS_STYLE[row.shot_status] || STATUS_STYLE.PENDING
  if (row.blocked_flag === 1) return STATUS_STYLE.FAILED
  return base
}

export default function StoryboardCanvas({ episodeId, onSwitchToList }) {
  const [rows, setRows] = useState([])
  const [selected, setSelected] = useState([])
  const [cost, setCost] = useState(null)
  const [dragging, setDragging] = useState(null)
  const [editing, setEditing] = useState(null)
  const [draft, setDraft] = useState('')

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
      task_type: 'IMAGE',
      auto_next_flag: 0,
    })
    setCost(data?.result)
  }

  const submit = async () => {
    const { data } = await productionApi.submitTasks({
      episode_id: episodeId,
      shot_ids: selected.length ? selected : undefined,
      task_type: 'IMAGE',
      auto_next_flag: 0,
    })
    if (data?.success) {
      message.success('已提交，去任务中心看进度')
      setCost(null)
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
    message.success('已保存')
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
            <Button size="small" onClick={estimate}>估费用</Button>
            <Button size="small" type="primary" disabled={!cost} onClick={submit}>批量生成</Button>
          </Space>
        )}
      >
        {cost ? (
          <Paragraph style={{ marginBottom: 0 }}>
            约 {cost.task_count} 镜 / {((cost.estimated_cost_cent || 0) / 100).toFixed(2)} 元
            （{cost.model_code || '-'}）。确认后提交，与列表视图走同一道预算闸门。
          </Paragraph>
        ) : (
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            点节点加选，或点左侧场次名选中整场。选完先估费用再提交。
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
                      width: 86, flexShrink: 0, padding: 6, borderRadius: 6, fontSize: 11,
                      background: style.background,
                      border: `${row.stale_flag === 1 ? 2 : 1}px solid ${row.stale_flag === 1 ? '#d97706' : style.borderColor}`,
                      boxShadow: selected.includes(row.shot_id) ? 'inset 0 0 0 2px #2563eb' : 'none',
                      cursor: locked ? 'not-allowed' : 'grab',
                      opacity: locked ? 0.75 : 1,
                    }}
                  >
                    <div style={{
                      height: 56, borderRadius: 4, marginBottom: 4, overflow: 'hidden',
                      background: '#e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: '#94a3b8', fontSize: 10,
                    }}>
                      {row.first_frame_url
                        ? <img alt="" src={row.first_frame_url} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        : style.label}
                    </div>
                    <div style={{ fontWeight: 700 }}>镜 {row.shot_no}</div>
                    <div style={{ color: '#64748b' }}>{row.shot_duration_sec || 3}s · {style.label}</div>
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
        单击选中、双击改文字、拖动改顺序。<Text strong>拖动只改顺序</Text>——不删素材、不改状态、不触发重抽。
        跨场拖不动：那意味着这一镜换了场景，属于剧本层的事。
      </Paragraph>

      <Drawer
        open={!!editing}
        width={480}
        onClose={() => setEditing(null)}
        title={editing ? `镜 ${editing.shot_no} · 人话分镜` : ''}
        extra={<Button type="primary" onClick={saveEditor}>保存</Button>}
      >
        <Paragraph type="secondary" style={{ fontSize: 12 }}>
          改这里免费。保存后列表视图同步——两个视图读写的是同一份数据。
        </Paragraph>
        <TextArea rows={12} value={draft} onChange={(e) => setDraft(e.target.value)} />
      </Drawer>
    </div>
  )
}
