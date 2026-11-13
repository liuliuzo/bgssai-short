import { useEffect, useRef, useState } from 'react'
import { Alert, Button, Card, Input, Select, Space, Tag, Upload, message, Modal } from 'antd'
import productionApi from '../../api/production'
import { scrubVendorText } from '../../publicModel'
import {
  LONG_RUN_HINT,
  NEW_TASK_WARNING,
  buildImageToVideoSubmit,
  imageToVideoUnavailable,
  newClientRequestId,
  pollDelayMs,
} from '../../imageToVideo'

export default function ImageToVideoPanel({ workId, episodeId, shots }) {
  const [capability, setCapability] = useState(null)
  const [asset, setAsset] = useState(null)
  const [shotId, setShotId] = useState()
  const [prompt, setPrompt] = useState('')
  const [width, setWidth] = useState('')
  const [height, setHeight] = useState('')
  const [frames, setFrames] = useState('')
  const [task, setTask] = useState(null)
  const [clientId, setClientId] = useState(() => newClientRequestId())
  const [submitting, setSubmitting] = useState(false)
  const attempt = useRef(0)
  const unavailable = imageToVideoUnavailable(capability)
  const busy = task && (task.task_status === 'QUEUED' || task.task_status === 'RUNNING')
  const shot = (shots || []).find((item) => item.shot_id === shotId)
  const sourceUrl = asset?.preview_url || shot?.first_frame_url || ''

  useEffect(() => {
    let stop = false
    productionApi.imageToVideoCapability()
      .then(({ data }) => {
        if (!stop) {
          setCapability(data?.result || { available: 0, reason: '图生视频暂不可用。文生视频不受影响。' })
        }
      })
      .catch(() => {
        if (!stop) setCapability({ available: 0, reason: '图生视频暂不可用。文生视频不受影响。' })
      })
    return () => { stop = true }
  }, [])

  useEffect(() => {
    attempt.current = 0
  }, [task?.task_id])

  useEffect(() => {
    if (!busy || !task?.task_id) return undefined
    const timer = setTimeout(async () => {
      try {
        const { data } = await productionApi.imageToVideoStatus(task.task_id)
        if (data?.result) setTask(data.result)
      } catch (error) {
        // 连接失败只延后下一次查询，不重新提交。
      }
      attempt.current += 1
    }, pollDelayMs(attempt.current))
    return () => clearTimeout(timer)
  }, [busy, task])

  const shotOptions = (shots || [])
    .filter((item) => item.first_frame_asset_id)
    .map((item) => ({ value: item.shot_id, label: `镜 ${item.shot_no} 首帧` }))

  return (
    <Card
      size="small"
      style={{ marginBottom: 12 }}
      title={<Space>图生视频 <Tag>AI生成</Tag> <Tag>BGSSAI Video</Tag></Space>}
    >
      {unavailable ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message={capability?.reason || '图生视频暂不可用。文生视频不受影响。'}
        />
      ) : null}
      <p style={{ marginTop: 0, color: '#334155' }}>镜头上的「生成视频」在有真实画面时走同一条图生视频，成片是 384×256、约 3 秒，不是竖屏 9:16。这里也可以另选一张图。宽、高、帧数可以留空。</p>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space wrap>
          <Upload
            accept="image/png,image/jpeg"
            showUploadList={false}
            beforeUpload={async (file) => {
              const { data } = await productionApi.uploadImage(file)
              if (data?.result?.asset_id) {
                setAsset(data.result)
                message.success('图片已上传')
              }
              return false
            }}
          >
            <Button>上传 PNG 或 JPEG</Button>
          </Upload>
          <Select
            allowClear
            placeholder="或选择镜头首帧"
            style={{ minWidth: 180 }}
            value={shotId}
            onChange={setShotId}
            options={shotOptions}
          />
        </Space>
        {sourceUrl ? <img alt="原图" src={sourceUrl} style={{ maxWidth: 240, maxHeight: 160 }} /> : null}
        <Input.TextArea
          rows={2}
          maxLength={2000}
          value={prompt}
          placeholder="用一句话说明画面怎么动。成片是 384×256、约 3 秒，出片比文生视频更慢。"
          onChange={(event) => setPrompt(event.target.value)}
        />
        <Space wrap>
          <Input style={{ width: 140 }} value={width} placeholder="宽度，可留空" onChange={(event) => setWidth(event.target.value)} />
          <Input style={{ width: 140 }} value={height} placeholder="高度，可留空" onChange={(event) => setHeight(event.target.value)} />
          <Input style={{ width: 140 }} value={frames} placeholder="帧数，可留空" onChange={(event) => setFrames(event.target.value)} />
        </Space>
        <Space wrap>
          <Button
            type="primary"
            loading={submitting}
            disabled={unavailable || busy}
            onClick={async () => {
              const built = buildImageToVideoSubmit({
                prompt,
                assetId: asset?.asset_id,
                shotId: asset?.asset_id ? '' : shotId,
                workId,
                episodeId,
                clientRequestId: clientId,
                width,
                height,
                numFrames: frames,
              })
              if (built.error) {
                message.warning(built.error)
                return
              }
              setSubmitting(true)
              try {
                const { data } = await productionApi.submitImageToVideo(built.body)
                if (data?.result?.task_id) setTask(data.result)
              } finally {
                setSubmitting(false)
              }
            }}
          >
            生成图生视频
          </Button>
          <Button
            disabled={busy}
            onClick={() => {
              Modal.confirm({
                title: '新建图生视频任务',
                content: NEW_TASK_WARNING,
                onOk: () => {
                  setClientId(newClientRequestId())
                  setTask(null)
                },
              })
            }}
          >
            新建任务
          </Button>
        </Space>
        {busy ? <Alert type="info" showIcon message={LONG_RUN_HINT} /> : null}
        {task ? (
          <Space direction="vertical">
            <span>{task.task_status_name || task.task_status} · AI生成</span>
            {task.fail_reason ? <span>{scrubVendorText(task.fail_reason)}</span> : null}
            {task.video_preview_url ? (
              <video src={task.video_preview_url} controls style={{ maxWidth: 320 }} />
            ) : null}
          </Space>
        ) : null}
      </Space>
    </Card>
  )
}
