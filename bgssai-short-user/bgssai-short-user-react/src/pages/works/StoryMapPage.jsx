import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Space, Table, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import { draftFlagLabel, labelOf, RELATION_TYPE_LABEL, softenTechId } from '../../lib/displayLabels'

const { Paragraph } = Typography

export default function StoryMapPage({ work, episodeId }) {
  const [tension, setTension] = useState(null)
  const [relations, setRelations] = useState(null)
  const [band, setBand] = useState([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!work?.work_id) return
    setLoading(true)
    try {
      if (episodeId) {
        const tRes = await featuresApi.storyMapTension(episodeId)
        setTension(tRes.data?.result || null)
      }
      const [rRes, bRes] = await Promise.all([
        featuresApi.storyMapRelations(work.work_id),
        featuresApi.storyMapTensionBand(work.work_id),
      ])
      setRelations(rRes.data?.result || null)
      setBand(bRes.data?.result?.band_list || [])
    } finally {
      setLoading(false)
    }
  }, [work, episodeId])

  useEffect(() => { load() }, [load])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card
        loading={loading}
        title="剧情"
        extra={<Button type="primary" onClick={async () => {
          const { data } = await featuresApi.storyMapGenerate({
            work_id: work.work_id,
            episode_id: episodeId,
            map_scope: 'ALL',
          })
          if (data?.success) {
            message.success(data.result?.draft_flag ? '已生成（草稿估算）' : '已生成')
            await load()
          }
        }}>生成图谱</Button>}
      >
        <Paragraph type="secondary">
          本集时长 {tension?.duration_sec ?? '-'} 秒 · {draftFlagLabel(tension?.draft_flag)}
        </Paragraph>
        <Table
          rowKey={(r) => `${r.at_sec}-${r.shot_no}`}
          size="small"
          pagination={false}
          dataSource={tension?.point_list || []}
          columns={[
            { title: '秒', dataIndex: 'at_sec', width: 70 },
            { title: '镜', dataIndex: 'shot_no', width: 70 },
            { title: '场', dataIndex: 'scene_no', width: 70 },
            { title: '张力', dataIndex: 'tension_score', width: 80 },
          ]}
        />
      </Card>
      <Card title="偏差">
        <Table
          rowKey={(_, i) => String(i)}
          size="small"
          pagination={false}
          dataSource={tension?.deviation_list || []}
          columns={[
            { title: '类型', dataIndex: 'deviation_type', width: 100 },
            { title: '说明', dataIndex: 'issue_desc' },
            { title: '建议', dataIndex: 'suggestion' },
          ]}
        />
      </Card>
      <Card title="人物关系">
        <Table
          rowKey={(r) => `${r.from_character_id}-${r.to_character_id}-${r.relation_type}`}
          size="small"
          pagination={false}
          dataSource={relations?.edge_list || []}
          columns={[
            { title: '从', dataIndex: 'from_character_id', render: (v, row) => row.from_character_name || softenTechId(v, 'character') },
            { title: '到', dataIndex: 'to_character_id', render: (v, row) => row.to_character_name || softenTechId(v, 'character') },
            { title: '关系', dataIndex: 'relation_type', render: (v, row) => row.relation_type_name && row.relation_type_name !== v ? row.relation_type_name : labelOf(RELATION_TYPE_LABEL, v, v) },
            { title: '说明', dataIndex: 'relation_desc' },
          ]}
        />
      </Card>
      <Card title="全剧张力条带">
        <Table
          rowKey="episode_id"
          size="small"
          pagination={false}
          dataSource={band}
          columns={[
            { title: '集', dataIndex: 'episode_no', width: 70 },
            { title: '均张力', dataIndex: 'avg_tension_score', width: 100 },
            { title: '反转', dataIndex: 'reversal_flag', width: 80 },
          ]}
        />
      </Card>
    </Space>
  )
}
