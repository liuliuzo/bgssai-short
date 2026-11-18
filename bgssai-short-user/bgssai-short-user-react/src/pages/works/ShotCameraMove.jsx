import { useRef, useState } from 'react'
import { Select, Space, Typography, message } from 'antd'
import productionApi from '../../api/production'
import { CAMERA_MOVEMENTS } from '../../utils/CinematographyHelper'

const aliases = { FIXED: 'STATIC', PUSH: 'PUSH_IN', PULL: 'PULL_OUT', FOLLOW: 'TRACKING' }
const legacyLabels = { PAN: '横向摇镜', TILT: '上下摇镜', HANDHELD: '手持拍摄' }

// Both layouts share pending saves so switching views cannot dispatch an older camera move.
export function useShotCameraSaves() {
  const pending = useRef(new Map())

  function save(shotId, cameraMove) {
    const previous = pending.current.get(shotId)
    const request = (async () => {
      if (previous) await previous
      try {
        const { data } = await productionApi.updateShot({ shot_id: shotId, camera_move: cameraMove })
        if (!data?.success) throw new Error(data?.message || '运镜保存失败')
        return true
      } catch (err) {
        message.error(err?.response?.data?.message || err.message || '运镜保存失败')
        return false
      }
    })()
    pending.current.set(shotId, request)
    request.finally(() => {
      if (pending.current.get(shotId) === request) pending.current.delete(shotId)
    })
    return request
  }

  async function waitFor(shotIds) {
    const ids = shotIds?.length ? shotIds : [...pending.current.keys()]
    const results = await Promise.all(ids.map((id) => pending.current.get(id)))
    return results.every((result) => result !== false)
  }

  return { save, waitFor }
}

export default function ShotCameraMove({ shot, saves, onSaved }) {
  const [saving, setSaving] = useState(null)
  const current = aliases[shot.camera_move] || shot.camera_move
  const options = current && !CAMERA_MOVEMENTS.some((item) => item.value === current)
    ? [...CAMERA_MOVEMENTS, { value: current, label: legacyLabels[current] || current }]
    : CAMERA_MOVEMENTS

  return (
    <Space size={8} style={{ display: 'flex', marginBottom: 8 }}>
      <Typography.Text type="secondary">运镜</Typography.Text>
      <Select
        size="small"
        aria-label={`镜 ${shot.shot_no} 运镜`}
        style={{ width: 150 }}
        placeholder="未设置"
        value={saving || current || undefined}
        options={options}
        disabled={shot.shot_status === 'LOCKED' || Boolean(saving)}
        loading={Boolean(saving)}
        onChange={async (move) => {
          setSaving(move)
          try {
            if (await saves.save(shot.shot_id, move)) onSaved(shot.shot_id, move)
          } finally {
            setSaving(null)
          }
        }}
      />
    </Space>
  )
}
