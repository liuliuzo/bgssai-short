import { useCallback, useEffect, useState } from 'react'
import { Button, Card, List, Space, Table, Typography } from 'antd'
import featuresApi from '../../api/features'

const { Paragraph, Title } = Typography

export default function EpisodePreviewPage({ work, episodeId }) {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!episodeId) return
    setLoading(true)
    try {
      const { data } = await featuresApi.previewSummary(episodeId)
      setSummary(data?.result || null)
    } finally {
      setLoading(false)
    }
  }, [episodeId])

  useEffect(() => { load() }, [load])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card loading={loading} title="三问预览" extra={<Button onClick={load}>刷新</Button>}>
        <Paragraph>
          {summary?.episode_title || '-'} · {summary?.shot_count || 0} 镜 · {summary?.total_sec || 0} 秒 ·
          成片 {summary?.compose_status || '-'} · 配音 {summary?.dubbing_status || '-'}
        </Paragraph>
        <Title level={5}>三个问题</Title>
        <List
          size="small"
          bordered
          dataSource={summary?.three_answers || []}
          renderItem={(item) => <List.Item>{item}</List.Item>}
        />
      </Card>
      <Card title="帧序列">
        <Table
          rowKey="shot_id"
          size="small"
          pagination={false}
          dataSource={summary?.frame_list || []}
          columns={[
            { title: '镜', dataIndex: 'shot_no', width: 70 },
            { title: '秒', dataIndex: 'duration_sec', width: 70 },
            { title: '预演层级', dataIndex: 'previz_level', width: 110 },
            { title: '首帧', dataIndex: 'first_frame_asset_id' },
            { title: '预演文本', dataIndex: 'previz_text' },
          ]}
        />
      </Card>
    </Space>
  )
}
