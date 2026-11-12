import { modelPricingLabel } from '../../modelPricing'
import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import productionApi from '../../api/production'
import { voiceRowView } from './studioListUx'

const { Paragraph } = Typography

function formatYuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

export default function VoiceStudioPage({ work, episodeId }) {
  const [dubbings, setDubbings] = useState([])
  const [misaligned, setMisaligned] = useState(0)
  const [subtitles, setSubtitles] = useState([])
  const [voices, setVoices] = useState([])
  const [voiceModels, setVoiceModels] = useState([])
  const [voiceModelCode, setVoiceModelCode] = useState()
  const [asrModels, setAsrModels] = useState([])
  const [asrModelCode, setAsrModelCode] = useState()
  const [loading, setLoading] = useState(true)
  const [clipSeconds, setClipSeconds] = useState({})
  const [styleForm] = Form.useForm()

  const load = useCallback(async () => {
    if (!episodeId) return
    setLoading(true)
    try {
      const [dRes, sRes, vRes, shotRes] = await Promise.all([
        featuresApi.listDubbings(episodeId),
        featuresApi.subtitleDetail(episodeId),
        featuresApi.listVoices(),
        productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 200 }),
      ])
      const shots = Array.isArray(shotRes.data?.result) ? shotRes.data.result : []
      const byId = new Map(shots.map((shot) => [shot.shot_id, shot]))
      const list = (dRes.data?.result?.dubbing_list || []).map((row) => {
        const shot = byId.get(row.shot_id) || {}
        return {
          ...row,
          dialogue_text: row.dialogue_text || shot.dialogue_text || '',
          previz_text: row.previz_text || shot.previz_text || '',
          video_url: row.video_url || shot.video_url || '',
          shot_duration_sec: row.shot_duration_sec ?? shot.shot_duration_sec,
        }
      })
      setDubbings(list)
      setMisaligned(dRes.data?.result?.misaligned_count || 0)
      setSubtitles(sRes.data?.result?.subtitle_list || [])
      const style = sRes.data?.result?.subtitle_style || {}
      styleForm.setFieldsValue(style)
      setVoices(vRes.data?.result?.voice_list || [])
    } finally {
      setLoading(false)
    }
  }, [episodeId, styleForm])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const nodes = []
    dubbings.forEach((row) => {
      if (!row.video_url) return
      const video = document.createElement('video')
      video.preload = 'metadata'
      const onMeta = () => {
        const duration = video.duration
        if (!Number.isFinite(duration) || duration <= 0) return
        setClipSeconds((cur) => (cur[row.shot_id] === duration ? cur : { ...cur, [row.shot_id]: duration }))
      }
      video.addEventListener('loadedmetadata', onMeta)
      video.src = row.video_url
      nodes.push(video)
    })
    return () => {
      nodes.forEach((video) => {
        video.removeAttribute('src')
        video.load()
      })
    }
  }, [dubbings])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [voiceRes, asrRes] = await Promise.all([
        productionApi.listAiModels('VOICE'),
        productionApi.listAiModels('ASR'),
      ])
      if (cancelled) return
      const nextVoiceModels = voiceRes.data?.result?.model_list || []
      const nextAsrModels = asrRes.data?.result?.model_list || []
      setVoiceModels(nextVoiceModels)
      setAsrModels(nextAsrModels)
      setVoiceModelCode(nextVoiceModels.find((m) => m.default_flag === 1)?.model_code || nextVoiceModels[0]?.model_code)
      setAsrModelCode(nextAsrModels.find((m) => m.default_flag === 1)?.model_code || nextAsrModels[0]?.model_code)
    })()
    return () => { cancelled = true }
  }, [])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card
        loading={loading}
        title="配音"
        extra={<Space>
          <Select
            placeholder="配音模型"
            value={voiceModelCode}
            onChange={setVoiceModelCode}
            style={{ width: 220 }}
            options={voiceModels.map((m) => ({
              value: m.model_code,
              label: modelPricingLabel(m),
            }))}
          />
          <Button type="primary" onClick={async () => {
            const body = { episode_id: episodeId, model_code: voiceModelCode }
            const { data: estRes } = await featuresApi.estimateDubbing(body)
            if (!estRes?.success) return
            const est = estRes.result || {}
            const ok = await new Promise((resolve) => {
              Modal.confirm({
                title: '确认配音费用',
                content: `预计 ${formatYuan(est.estimated_cost_cent)} 元 · 余额 ${formatYuan(est.available_cent)} 元 · ${est.task_count || 0} 条台词`,
                okText: '确认提交',
                cancelText: '取消',
                onOk: () => resolve(true),
                onCancel: () => resolve(false),
              })
            })
            if (!ok) return
            const { data } = await featuresApi.submitDubbing(body)
            if (data?.success) {
              message.success(`已提交 ${data.result?.task_count || 0} 个配音任务`)
              await load()
            }
          }}>批量配音</Button>
          <Button onClick={async () => {
            const { data } = await featuresApi.alignBatchDubbing(episodeId)
            if (data?.success) {
              message.success(`已自动处理 ${data.result?.fixed_list?.length || 0} 项`)
              await load()
            }
          }}>一键对齐免费项</Button>
          <Button onClick={load}>刷新</Button>
        </Space>}
      >
        <Paragraph type="secondary">未对齐 {misaligned} · 音色库 {voices.length}</Paragraph>
        <Table
          rowKey="shot_id"
          size="small"
          pagination={false}
          dataSource={dubbings}
          columns={[
            { title: '镜号', dataIndex: 'shot_no', width: 70 },
            {
              title: '台词',
              render: (_, row) => voiceRowView(row, clipSeconds[row.shot_id]).dialogue || '（无对白）',
            },
            {
              title: '镜头秒',
              width: 168,
              render: (_, row) => voiceRowView(row, clipSeconds[row.shot_id]).timing,
            },
            { title: '音频秒', dataIndex: 'audio_duration_sec', width: 80 },
            {
              title: '对齐',
              width: 90,
              render: (_, row) => voiceRowView(row, clipSeconds[row.shot_id]).align,
            },
            { title: '建议', dataIndex: 'suggest_action', width: 120 },
            {
              title: '操作',
              width: 180,
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={async () => {
                    await featuresApi.regenerateDubbing({ shot_id: row.shot_id, model_code: voiceModelCode })
                    message.success('已重配')
                    await load()
                  }}>重配</Button>
                  {row.suggest_action && row.suggest_action !== 'EXTEND_SHOT' ? (
                    <Button size="small" onClick={async () => {
                      await featuresApi.alignDubbing({ shot_id: row.shot_id, align_action: row.suggest_action })
                      message.success('已对齐')
                      await load()
                    }}>按建议</Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Card
        title="字幕"
        extra={<Space>
          <Select
            placeholder="语音识别模型"
            value={asrModelCode}
            onChange={setAsrModelCode}
            style={{ width: 220 }}
            options={asrModels.map((m) => ({
              value: m.model_code,
              label: modelPricingLabel(m),
            }))}
          />
          <Button type="primary" onClick={async () => {
            const { data } = await featuresApi.subtitleTranscribe({
              episode_id: episodeId,
              model_code: asrModelCode,
              confirm_overwrite_flag: 1,
            })
            if (data?.success) {
              message.success(`已提交 ${data.result?.task_count || 0} 个语音识别任务`)
              await load()
            }
          }}>语音识别生成字幕</Button>
          <Button onClick={async () => {
            await featuresApi.subtitleRegen(episodeId)
            message.success('字幕已重生')
            await load()
          }}>重生字幕</Button>
        </Space>}
      >
        <Table
          rowKey="subtitle_id"
          size="small"
          pagination={false}
          dataSource={subtitles}
          columns={[
            { title: '序号', dataIndex: 'subtitle_no', width: 70 },
            { title: '开始ms', dataIndex: 'start_ms', width: 90 },
            { title: '结束ms', dataIndex: 'end_ms', width: 90 },
            {
              title: '文本',
              dataIndex: 'subtitle_text',
              render: (v, row) => (
                <Input
                  value={v}
                  onChange={(e) => {
                    const next = subtitles.map((s) =>
                      s.subtitle_id === row.subtitle_id ? { ...s, subtitle_text: e.target.value } : s)
                    setSubtitles(next)
                  }}
                  onBlur={async () => {
                    await featuresApi.subtitleUpdate({
                      subtitle_id: row.subtitle_id,
                      subtitle_text: row.subtitle_text,
                    })
                  }}
                />
              ),
            },
          ]}
        />
        <Form
          form={styleForm}
          layout="inline"
          style={{ marginTop: 16 }}
          onFinish={async (values) => {
            await featuresApi.subtitleStyleSave({ work_id: work.work_id, ...values })
            message.success('样式已保存')
          }}
        >
          <Form.Item name="font_size" label="字号"><InputNumber /></Form.Item>
          <Form.Item name="font_color" label="颜色"><Input style={{ width: 110 }} /></Form.Item>
          <Form.Item name="stroke_color" label="描边"><Input style={{ width: 110 }} /></Form.Item>
          <Form.Item name="position" label="位置">
            <Select style={{ width: 120 }} options={[
              { value: 'BOTTOM', label: '底部' },
              { value: 'TOP', label: '顶部' },
            ]} />
          </Form.Item>
          <Button type="primary" htmlType="submit">保存样式</Button>
        </Form>
      </Card>
    </Space>
  )
}
