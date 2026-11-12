import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Select, Space, Table, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import { labelOf, GATE_STATUS_LABEL, HANDLE_STATUS_LABEL } from '../../lib/displayLabels'

const { Paragraph } = Typography

const GATES = [
  { value: 'G1_SCRIPT', label: '剧本门' },
  { value: 'G2_STORYBOARD', label: '分镜门' },
  { value: 'G3_VISUAL', label: '画面门' },
  { value: 'G4_FILM', label: '成片门' },
]

export default function QualityReportPage({ work, episodeId }) {
  const [gateCode, setGateCode] = useState('G1_SCRIPT')
  const [result, setResult] = useState(null)
  const [profile, setProfile] = useState(null)
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!work?.work_id) return
    setLoading(true)
    try {
      const [pRes, lRes] = await Promise.all([
        featuresApi.qualityProfile(work.work_id),
        featuresApi.qualityThresholdLogs(work.work_id),
      ])
      setProfile(pRes.data?.result || null)
      setLogs(lRes.data?.result?.log_list || [])
      if (episodeId) {
        const gRes = await featuresApi.qualityGateResult(gateCode, episodeId)
        setResult(gRes.data?.result || null)
      }
    } finally {
      setLoading(false)
    }
  }, [work, episodeId, gateCode])

  useEffect(() => { load() }, [load])

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card
        loading={loading}
        title="质量"
        extra={<Space>
          <Select style={{ width: 140 }} value={gateCode} options={GATES} onChange={setGateCode} />
          <Button type="primary" onClick={async () => {
            const { data } = await featuresApi.qualityGateRun({
              work_id: work.work_id,
              episode_id: episodeId,
              gate_code: gateCode,
            })
            if (data?.success) {
              message.success(`评分 ${data.result?.score}`)
              setResult(data.result)
              await load()
            }
          }}>跑门禁</Button>
          <Button onClick={load}>刷新</Button>
        </Space>}
      >
        <Paragraph>
          作品阈值 {profile?.quality_threshold_score ?? '-'} · 总分 {profile?.overall_quality_score ?? '-'} ·
          当前门 {labelOf(GATE_STATUS_LABEL, result?.gate_status, result?.gate_status || '-')} / {result?.score ?? '-'}
        </Paragraph>
        <Table
          rowKey="issue_id"
          size="small"
          pagination={false}
          dataSource={result?.issue_list || []}
          columns={[
            { title: '维度', dataIndex: 'dimension', width: 120 },
            { title: '问题', dataIndex: 'issue_desc' },
            { title: '建议', dataIndex: 'fix_hint' },
            { title: '状态', dataIndex: 'handle_status', width: 100, render: (v) => labelOf(HANDLE_STATUS_LABEL, v, v) },
            {
              title: '操作',
              width: 200,
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={async () => {
                    await featuresApi.qualityIssueHandle({ issue_id: row.issue_id, handle_status: 'FIXED' })
                    message.success('已标记修复')
                    await load()
                  }}>已修复</Button>
                  <Button size="small" onClick={async () => {
                    await featuresApi.qualityIssueHandle({
                      issue_id: row.issue_id,
                      handle_status: 'WONT_FIX',
                      handle_reason: '有意为之',
                    })
                    message.success('已标记不改')
                    await load()
                  }}>不改</Button>
                </Space>
              ),
            },
          ]}
        />
        {result?.gate_id && result?.gate_status === 'FAILED' ? (
          <Button style={{ marginTop: 12 }} onClick={async () => {
            await featuresApi.qualityForcePass({
              gate_id: result.gate_id,
              force_pass_reason: '赶进度强制放行',
            })
            message.success('已强制放行')
            await load()
          }}>强制放行</Button>
        ) : null}
      </Card>
      <Card title="阈值调整">
        <Form layout="inline" onFinish={async (values) => {
          await featuresApi.qualityThresholdAdjust({
            work_id: work.work_id,
            new_threshold_score: values.new_threshold_score,
            adjust_reason: values.adjust_reason,
          })
          message.success('阈值已更新')
          await load()
        }}>
          <Form.Item name="new_threshold_score" label="新阈值" rules={[{ required: true }]}>
            <InputNumber min={1} max={100} />
          </Form.Item>
          <Form.Item name="adjust_reason" label="理由"><Input style={{ width: 220 }} /></Form.Item>
          <Button type="primary" htmlType="submit">保存</Button>
        </Form>
        <Table
          style={{ marginTop: 12 }}
          rowKey="log_id"
          size="small"
          pagination={false}
          dataSource={logs}
          columns={[
            { title: '原值', dataIndex: 'old_threshold_score', width: 80 },
            { title: '新值', dataIndex: 'new_threshold_score', width: 80 },
            { title: '理由', dataIndex: 'adjust_reason' },
          ]}
        />
      </Card>
      <Card title="多方案" extra={<Button onClick={async () => {
        const { data } = await featuresApi.qualityVariantsGenerate({
          work_id: work.work_id,
          episode_id: episodeId,
          variant_node: 'OPENING',
          target_id: episodeId,
          variant_count: 3,
        })
        if (data?.success) {
          message.success(`生成 ${data.result?.variant_list?.length || 0} 个方案`)
        }
      }}>生成开场方案</Button>}>
        <Paragraph type="secondary">关键节点多方案择优入口已接通。</Paragraph>
      </Card>
    </Space>
  )
}
