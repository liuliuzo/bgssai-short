import { useCallback, useEffect, useState } from 'react'
import { Button, Card, DatePicker, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from 'antd'
import dayjs from 'dayjs'
import usageApi from '../../api/usage'

const { Title, Paragraph, Text } = Typography
const { RangePicker } = DatePicker

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function UsageBillingPage() {
  const [summary, setSummary] = useState(null)
  const [records, setRecords] = useState([])
  const [applies, setApplies] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [episodeTotal, setEpisodeTotal] = useState(null)
  const [range, setRange] = useState([dayjs().subtract(30, 'day'), dayjs()])
  const [workId, setWorkId] = useState()
  const [episodeId, setEpisodeId] = useState()
  const [abilityType, setAbilityType] = useState()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async (num, size, dates, work, episode, ability) => {
    setLoading(true)
    try {
      const params = {
        start_date: dates?.[0] ? dates[0].format('YYYY-MM-DD') : undefined,
        end_date: dates?.[1] ? dates[1].format('YYYY-MM-DD') : undefined,
      }
      const [sumRes, recRes, applyRes] = await Promise.all([
        usageApi.summary(params),
        usageApi.records({
          ...params,
          page_num: num,
          page_size: size,
          work_id: work,
          episode_id: episode,
          ability_type: ability,
        }),
        usageApi.listRecharge(),
      ])
      setSummary(sumRes.data?.result || null)
      const result = recRes.data?.result || {}
      setRecords(result.record_list || [])
      setTotal(Number(result.total_size) || 0)
      setPageNum(Number(result.page_num) || num)
      setPageSize(Number(result.page_size) || size)
      setEpisodeTotal(result.episode_total_cost_cent == null ? null : result.episode_total_cost_cent)
      setApplies(applyRes.data?.result?.apply_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(1, 20, range, workId, episodeId, abilityType) }, [load, range, workId, episodeId, abilityType])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>用量与账单</Title>
      <Paragraph type="secondary">
        失败任务不计费；余额低于近一集均价 3 倍时 low_balance_flag=1。
      </Paragraph>
      <Space style={{ marginBottom: 12 }} wrap>
        <RangePicker value={range} onChange={setRange} />
        <Input placeholder="work_id" value={workId} onChange={(e) => setWorkId(e.target.value || undefined)} style={{ width: 160 }} allowClear />
        <Input placeholder="episode_id" value={episodeId} onChange={(e) => setEpisodeId(e.target.value || undefined)} style={{ width: 160 }} allowClear />
        <Select
          allowClear
          placeholder="能力"
          style={{ width: 120 }}
          value={abilityType}
          onChange={setAbilityType}
          options={[
            { value: 'TEXT', label: '文本' },
            { value: 'IMAGE', label: '图像' },
            { value: 'VIDEO', label: '视频' },
            { value: 'VOICE', label: '配音' },
            { value: 'COMPOSE', label: '合成' },
          ]}
        />
        <Button onClick={() => load(1, pageSize, range, workId, episodeId, abilityType)}>查询</Button>
        <Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>提交充值申请</Button>
      </Space>
      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Space size="large" wrap>
          <Text>可用 {yuan(summary?.available_cent)} 元</Text>
          <Text>周期 {yuan(summary?.balance_cent)} / 加量 {yuan(summary?.extra_balance_cent)}</Text>
          <Text>本月已用 {yuan(summary?.month_spent_cent)} / 上限 {yuan(summary?.monthly_limit_cent)}</Text>
          <Text>近一集均价 {yuan(summary?.recent_episode_avg_cost_cent)} 元</Text>
          <Text>约还能做 {summary?.estimated_remaining_episode_count ?? 0} 集</Text>
          {summary?.low_balance_flag === 1 ? <Text type="danger">余额偏低预警</Text> : null}
        </Space>
      </Card>
      <Card title="按作品" style={{ marginBottom: 16 }}>
        <Table
          rowKey="work_id"
          size="small"
          pagination={false}
          dataSource={summary?.by_work_list || []}
          columns={[
            { title: '作品', dataIndex: 'work_title' },
            { title: '成本(元)', dataIndex: 'cost_cent', render: yuan },
            { title: '任务数', dataIndex: 'task_count' },
          ]}
        />
      </Card>
      <Card title="按能力" style={{ marginBottom: 16 }}>
        <Table
          rowKey="ability_type"
          size="small"
          pagination={false}
          dataSource={summary?.by_ability_list || []}
          columns={[
            { title: '能力', dataIndex: 'ability_type_name' },
            { title: '成本(元)', dataIndex: 'cost_cent', render: yuan },
            { title: '任务数', dataIndex: 'task_count' },
          ]}
        />
      </Card>
      <Card
        title="用量流水"
        extra={episodeTotal == null ? null : <Text type="secondary">本集累计 {yuan(episodeTotal)} 元</Text>}
        style={{ marginBottom: 16 }}
      >
        <Table
          rowKey="record_id"
          size="small"
          loading={loading}
          dataSource={records}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size, range, workId, episodeId, abilityType),
          }}
          columns={[
            { title: '时间', dataIndex: 'created_time', width: 160 },
            { title: '能力', dataIndex: 'ability_type_name', width: 80 },
            { title: '模型', dataIndex: 'model_code', width: 120 },
            { title: '档位', dataIndex: 'quality_level_name', width: 80 },
            {
              title: '关联',
              render: (_, row) => `${row.work_title || row.work_id || '-'} · E${row.episode_no ?? '-'} · 镜 ${row.shot_no ?? '-'}`,
            },
            { title: '用量', dataIndex: 'usage_amount', width: 70 },
            { title: '单价(元)', dataIndex: 'unit_price_cent', width: 90, render: yuan },
            { title: '金额(元)', dataIndex: 'cost_cent', width: 90, render: yuan },
          ]}
        />
      </Card>
      <Card title="充值申请">
        <Table
          rowKey="apply_id"
          size="small"
          dataSource={applies}
          pagination={false}
          columns={[
            { title: '申请时间', dataIndex: 'created_time' },
            { title: '金额(元)', dataIndex: 'apply_amount_cent', render: yuan },
            { title: '状态', dataIndex: 'apply_status' },
            { title: '备注', dataIndex: 'user_remark' },
            { title: '运营备注', dataIndex: 'operator_remark' },
            { title: '处理时间', dataIndex: 'handled_time' },
          ]}
        />
      </Card>
      <Modal
        title="提交充值申请"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          const { data } = await usageApi.applyRecharge({
            apply_amount_cent: values.apply_amount_cent,
            remark: values.remark,
          })
          if (data?.success) {
            message.success('已提交')
            setOpen(false)
            load(pageNum, pageSize, range, workId, episodeId, abilityType)
          }
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="apply_amount_cent" label="金额(分)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
