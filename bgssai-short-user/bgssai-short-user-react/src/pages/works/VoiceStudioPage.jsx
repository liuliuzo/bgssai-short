import { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import productionApi from '../../api/production'
import { friendlyFailReason } from '../../lib/displayLabels'
import { modelPricingCompactLabel, modelPricingLabel } from '../../modelPricing'
import { subtitleClock, voiceRowView, voiceStudioSummary } from './studioListUx'

const { Title, Paragraph } = Typography

function formatYuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

export default function VoiceStudioPage({ work, episodeId, onJumpTab }) {
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
  const [voiceFailures, setVoiceFailures] = useState([])
  const [styleForm] = Form.useForm()

  const load = useCallback(async () => {
    if (!episodeId) return
    setLoading(true)
    try {
      const [dRes, sRes, vRes, shotRes, taskRes] = await Promise.all([
        featuresApi.listDubbings(episodeId),
        featuresApi.subtitleDetail(episodeId),
        featuresApi.listVoices(),
        productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 200 }),
        work?.work_id
          ? productionApi.listTasks({ work_id: work.work_id, task_status: 'FAILED', page_num: 1, page_size: 50 })
          : Promise.resolve({ data: { result: [] } }),
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
      const failed = (Array.isArray(taskRes.data?.result) ? taskRes.data.result : []).filter((row) => {
        const type = String(row.task_type || '').toUpperCase()
        if (type !== 'VOICE' && type !== 'DUBBING') return false
        return !row.episode_id || row.episode_id === episodeId
      })
      setVoiceFailures(failed)
    } finally {
      setLoading(false)
    }
  }, [episodeId, styleForm, work?.work_id])

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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <Title level={5} style={{ margin: 0 }}>第 4 步：配音与字幕（可选）</Title>
          <Paragraph style={{ marginBottom: 0, color: '#334155' }}>
            可为有台词的镜头生成人声配音并对齐时长；如暂不配音，可直接跳过本步去合成成片（默认按镜头对白自动烧录字幕）。
          </Paragraph>
        </div>
        {onJumpTab ? (
          <Button type="primary" onClick={() => onJumpTab('compose')}>去第 5 步：合成成片</Button>
        ) : null}
      </div>
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
              label: modelPricingCompactLabel(m),
              title: modelPricingLabel(m),
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
        {voiceFailures.length ? (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 12 }}
            message={`${voiceFailures.length} 条配音没有做成`}
            description={friendlyFailReason(voiceFailures[0].fail_reason) || '模型服务暂时不可用，请稍后重试。可以点这一行的「重配」。'}
          />
        ) : null}
        <Paragraph style={{ color: '#334155' }}>{voiceStudioSummary(dubbings, misaligned)} 音色库 {voices.length} 个。</Paragraph>
        <Table
          rowKey="shot_id"
          size="small"
          pagination={false}
          tableLayout="fixed"
          dataSource={dubbings}
          columns={[
            { title: '镜号', dataIndex: 'shot_no', width: 72 },
            {
              title: '台词',
              width: '36%',
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
                  {row.suggest_action ? (
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
              label: modelPricingCompactLabel(m),
              title: modelPricingLabel(m),
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
          <Button onClick={() => {
            Modal.confirm({
              title: '按对白重新整理字幕',
              content: '这会按现有对白重新整理这一集的字幕，不会新建一部作品，也不会重新配音。',
              okText: '开始整理',
              cancelText: '取消',
              onOk: async () => {
                await featuresApi.subtitleRegen(episodeId)
                message.success('字幕已按对白整理')
                await load()
              },
            })
          }}>按对白重新整理字幕</Button>
        </Space>}
      >
        {!subtitles.length ? (
          <Paragraph style={{ color: '#334155' }}>还没有字幕。配音完成后，点「语音识别生成字幕」。</Paragraph>
        ) : null}
        <Table
          rowKey="subtitle_id"
          size="small"
          pagination={false}
          tableLayout="fixed"
          locale={{ emptyText: '还没有字幕。配音完成后，点「语音识别生成字幕」。' }}
          dataSource={subtitles}
          columns={[
            { title: '序号', dataIndex: 'subtitle_no', width: 72 },
            { title: '开始', dataIndex: 'start_ms', width: 110, render: (value) => subtitleClock(value) },
            { title: '结束', dataIndex: 'end_ms', width: 110, render: (value) => subtitleClock(value) },
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
          <Paragraph style={{ width: '100%', color: '#334155' }}>留空时用白字、黑边、底部。</Paragraph>
          <Form.Item name="font_size" label="字号"><InputNumber placeholder="例如 28" /></Form.Item>
          <Form.Item name="font_color" label="颜色"><Input style={{ width: 140 }} placeholder="例如 #ffffff" /></Form.Item>
          <Form.Item name="stroke_color" label="描边"><Input style={{ width: 140 }} placeholder="例如 #000000" /></Form.Item>
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
