import { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Card, List, Space, Typography } from 'antd'
import featuresApi from '../../api/features'
import { labelOf, PREVIZ_LEVEL_LABEL } from '../../lib/displayLabels'

const { Paragraph, Text } = Typography

export default function EpisodePreviewPage({ work, episodeId }) {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [cur, setCur] = useState(0)

  const load = useCallback(async () => {
    if (!episodeId) return
    setLoading(true)
    try {
      const { data } = await featuresApi.previewSummary(episodeId)
      setSummary(data?.result || null)
      setCur(0)
    } finally {
      setLoading(false)
    }
  }, [episodeId])

  useEffect(() => { load() }, [load])

  const frames = summary?.frame_list || []
  const frame = frames[cur] || null

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="这就是成片未来的样子（还不花视频钱）"
        description="用文字和已出的图，按真实镜头时长把这一集过一遍。构图不对就回创作台改人话，不要直接重抽视频。"
      />
      <Card loading={loading} title={`${summary?.episode_title || '看样片'} · ${summary?.shot_count || 0} 镜 · ${summary?.total_sec || 0} 秒`} extra={<Button onClick={load}>刷新</Button>}>
        <List
          size="small"
          bordered
          dataSource={summary?.three_answers || []}
          renderItem={(item) => <List.Item>{item}</List.Item>}
        />
      </Card>
      <Space align="start" size="large" wrap>
        <Card title="竖屏连看" style={{ width: 320 }}>
          {frame ? (
            <div>
              {frame.first_frame_url
                ? <img alt="" src={frame.first_frame_url} style={{ width: 260, borderRadius: 12, display: 'block', margin: '0 auto' }} />
                : (
                  <div style={{
                    width: 260, minHeight: 420, margin: '0 auto', borderRadius: 12,
                    background: '#0f172a', color: '#e2e8f0', padding: 16, fontSize: 14, lineHeight: 1.7,
                  }}>
                    <Text style={{ color: '#94a3b8' }}>镜 {frame.shot_no} · 文字预演</Text>
                    <Paragraph style={{ color: '#f8fafc', marginTop: 12 }}>{frame.look_plain || frame.previz_text || '这一镜还没写清。'}</Paragraph>
                  </div>
                )}
              <Paragraph style={{ marginTop: 12, marginBottom: 4 }}>
                <Text strong>字幕</Text> {frame.dialogue_text || '（无对白）'}
              </Paragraph>
              <Paragraph type="secondary">{frame.look_plain}</Paragraph>
              <Space>
                <Button disabled={cur <= 0} onClick={() => setCur((n) => n - 1)}>上一镜</Button>
                <Text>镜 {frame.shot_no} / {frames.length} · {frame.duration_sec}s · {labelOf(PREVIZ_LEVEL_LABEL, frame.previz_level, frame.previz_level)}</Text>
                <Button disabled={cur >= frames.length - 1} onClick={() => setCur((n) => n + 1)}>下一镜</Button>
              </Space>
            </div>
          ) : <Paragraph type="secondary">先去创作台拆镜，这里才能连着看。</Paragraph>}
        </Card>
        <Card title="整集缩略" style={{ flex: 1, minWidth: 280 }}>
          <Space wrap>
            {frames.map((f, i) => (
              <button
                key={f.shot_id}
                type="button"
                onClick={() => setCur(i)}
                style={{
                  width: 56, padding: 0, border: i === cur ? '2px solid #1677ff' : '1px solid #e2e8f0',
                  borderRadius: 6, background: '#fff', cursor: 'pointer',
                }}
              >
                {f.first_frame_url
                  ? <img alt="" src={f.first_frame_url} width={52} height={88} style={{ display: 'block', borderRadius: 4 }} />
                  : <div style={{ width: 52, height: 88, fontSize: 11, padding: 4 }}>{f.shot_no}</div>}
              </button>
            ))}
          </Space>
        </Card>
      </Space>
    </Space>
  )
}
