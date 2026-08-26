import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Space, Table, Typography, message } from 'antd'
import featuresApi from '../../api/features'

const { Paragraph } = Typography

export default function ContinuityCenterPage({ work }) {
  const [overview, setOverview] = useState(null)
  const [foreshadows, setForeshadows] = useState([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!work?.work_id) return
    setLoading(true)
    try {
      const [oRes, fRes] = await Promise.all([
        featuresApi.continuityOverview(work.work_id),
        featuresApi.continuityForeshadows(work.work_id),
      ])
      setOverview(oRes.data?.result || null)
      setForeshadows(fRes.data?.result?.foreshadow_list || [])
    } finally {
      setLoading(false)
    }
  }, [work])

  useEffect(() => { load() }, [load])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card
        loading={loading}
        title="连贯性中心"
        extra={<Space>
          <Button type="primary" onClick={async () => {
            const { data } = await featuresApi.continuityRecheck({ work_id: work.work_id })
            if (data?.success) {
              message.success(`新检出 ${data.result?.created_count || 0} 条`)
              await load()
            }
          }}>重新体检</Button>
          <Button onClick={load}>刷新</Button>
        </Space>}
      >
        <Paragraph type="secondary">待处理 {overview?.open_issue_count || 0}</Paragraph>
        <Table
          rowKey="issue_id"
          size="small"
          pagination={false}
          dataSource={overview?.issue_list || []}
          columns={[
            { title: '类型', dataIndex: 'issue_type', width: 100 },
            { title: '严重度', dataIndex: 'severity', width: 90 },
            { title: '说明', dataIndex: 'issue_desc' },
            { title: '镜头', dataIndex: 'shot_id' },
            { title: '状态', dataIndex: 'handle_status', width: 100 },
          ]}
        />
      </Card>
      <Card title="伏笔">
        <Form layout="inline" onFinish={async (values) => {
          await featuresApi.continuityForeshadowSave({
            work_id: work.work_id,
            ...values,
            status: 'OPEN',
          })
          message.success('伏笔已保存')
          await load()
        }}>
          <Form.Item name="plant_episode_no" label="埋点集" rules={[{ required: true }]}>
            <InputNumber min={1} />
          </Form.Item>
          <Form.Item name="payoff_episode_no" label="回收集">
            <InputNumber min={1} />
          </Form.Item>
          <Form.Item name="foreshadow_desc" label="描述" rules={[{ required: true }]}>
            <Input style={{ width: 260 }} />
          </Form.Item>
          <Button type="primary" htmlType="submit">新增</Button>
        </Form>
        <Table
          style={{ marginTop: 12 }}
          rowKey="foreshadow_id"
          size="small"
          pagination={false}
          dataSource={foreshadows}
          columns={[
            { title: '埋点集', dataIndex: 'plant_episode_no', width: 90 },
            { title: '回收集', dataIndex: 'payoff_episode_no', width: 90 },
            { title: '描述', dataIndex: 'foreshadow_desc' },
            { title: '状态', dataIndex: 'status', width: 90 },
          ]}
        />
      </Card>
    </Space>
  )
}
