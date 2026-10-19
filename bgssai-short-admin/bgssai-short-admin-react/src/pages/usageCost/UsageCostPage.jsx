import { useCallback, useEffect, useState } from 'react'
import { Button, Card, DatePicker, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd'
import dayjs from 'dayjs'
import usageApi from '../../api/usage'
import rechargeApi from '../../api/recharge'

const { Title, Paragraph, Text } = Typography
const { RangePicker } = DatePicker

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function UsageCostPage() {
  const [summary, setSummary] = useState(null)
  const [creators, setCreators] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [range, setRange] = useState([dayjs().subtract(30, 'day'), dayjs()])
  const [abilityType, setAbilityType] = useState()
  const [applies, setApplies] = useState([])
  const [handleRow, setHandleRow] = useState(null)
  const [form] = Form.useForm()

  const load = useCallback(async (num, size, dates, ability) => {
    setLoading(true)
    try {
      const params = {
        start_date: dates?.[0] ? dates[0].format('YYYY-MM-DD') : undefined,
        end_date: dates?.[1] ? dates[1].format('YYYY-MM-DD') : undefined,
        ability_type: ability,
      }
      const [sumRes, creatorRes, applyRes] = await Promise.all([
        usageApi.summary(params),
        usageApi.byCreator({
          page_num: num,
          page_size: size,
          start_date: params.start_date,
          end_date: params.end_date,
        }),
        rechargeApi.list({ page_num: 1, page_size: 20 }),
      ])
      setSummary(sumRes.data?.result || null)
      setCreators(Array.isArray(creatorRes.data?.result) ? creatorRes.data.result : [])
      setTotal(Number(creatorRes.data?.total_size) || 0)
      setPageNum(Number(creatorRes.data?.page_num) || num)
      setPageSize(Number(creatorRes.data?.page_size) || size)
      setApplies(Array.isArray(applyRes.data?.result) ? applyRes.data.result : [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(1, 20, range, abilityType) }, [load, range, abilityType])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>平台用量与成本</Title>
      <Paragraph type="secondary">汇总近一段时间内已成功任务的成本，并可按创作者查看。充值申请可在此通过或驳回。</Paragraph>
      <Space style={{ marginBottom: 12 }}>
        <RangePicker value={range} onChange={setRange} />
        <Select
          allowClear
          placeholder="能力类型"
          style={{ width: 140 }}
          value={abilityType}
          onChange={setAbilityType}
          options={[
            { value: 'TEXT', label: '文本' },
            { value: 'IMAGE', label: '图片' },
            { value: 'VIDEO', label: '视频' },
            { value: 'VOICE', label: '配音' },
            { value: 'ASR', label: '语音识别' },
            { value: 'MUSIC', label: '音乐生成' },
            { value: 'COMPOSE', label: '合成' },
          ]}
        />
        <Button onClick={() => load(1, pageSize, range, abilityType)}>查询</Button>
      </Space>
      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Space size="large" wrap>
          <Text>总成本 {yuan(summary?.total_cost_cent)} 元</Text>
          <Text>任务数 {summary?.task_count ?? '-'}</Text>
          <Text>成功率 {summary?.success_rate_percent ?? '-'}%</Text>
          <Text>平均耗时 {summary?.avg_duration_sec ?? '-'} 秒</Text>
        </Space>
      </Card>
      <Card title="按能力" style={{ marginBottom: 16 }}>
        <Table
          rowKey="ability_type"
          size="small"
          pagination={false}
          dataSource={summary?.by_ability_list || []}
          columns={[
            { title: '能力', dataIndex: 'ability_type_name', render: (v, row) => v || row.ability_type },
            { title: '成本(元)', dataIndex: 'cost_cent', render: yuan },
            { title: '任务数', dataIndex: 'task_count' },
          ]}
        />
      </Card>
      <Card title="按日" style={{ marginBottom: 16 }}>
        <Table
          rowKey="day"
          size="small"
          pagination={false}
          dataSource={summary?.by_day_list || []}
          columns={[
            { title: '日期', dataIndex: 'day' },
            { title: '成本(元)', dataIndex: 'cost_cent', render: yuan },
            { title: '任务数', dataIndex: 'task_count' },
          ]}
        />
      </Card>
      <Card title="按创作者" style={{ marginBottom: 16 }}>
        <Table
          rowKey="user_id"
          size="small"
          loading={loading}
          dataSource={creators}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (n) => `共 ${n} 条`,
            onChange: (num, size) => load(num, size, range, abilityType),
          }}
          columns={[
            { title: '用户', dataIndex: 'user_name' },
            { title: '姓名', dataIndex: 'real_name' },
            { title: 'user_id', dataIndex: 'user_id' },
            { title: '成本(元)', dataIndex: 'cost_cent', render: yuan },
            { title: '任务数', dataIndex: 'task_count' },
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
            { title: '申请号', dataIndex: 'apply_id' },
            { title: '用户', dataIndex: 'user_name' },
            { title: '金额(元)', dataIndex: 'apply_amount_cent', render: yuan },
            { title: '状态', dataIndex: 'apply_status', render: (v) => ({ SUBMITTED: '已提交', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回' }[v] || v) },
            { title: '用户备注', dataIndex: 'user_remark' },
            {
              title: '操作',
              width: 160,
              render: (_, row) => (
                (row.apply_status === 'SUBMITTED' || row.apply_status === 'PROCESSING') ? (
                  <Button size="small" onClick={() => { setHandleRow(row); form.resetFields() }}>处理</Button>
                ) : '-'
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title={`处理申请 ${handleRow?.apply_id || ''}`}
        open={!!handleRow}
        onCancel={() => setHandleRow(null)}
        onOk={async () => {
          const values = await form.validateFields()
          const { data } = await rechargeApi.handle({
            apply_id: handleRow.apply_id,
            handle_action: values.handle_action,
            remark: values.remark,
          })
          if (data?.success) {
            message.success('已处理')
            setHandleRow(null)
            load(pageNum, pageSize, range, abilityType)
          }
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="handle_action" label="动作" rules={[{ required: true }]} initialValue="COMPLETE">
            <Select options={[
              { value: 'COMPLETE', label: '通过并加额' },
              { value: 'REJECT', label: '驳回' },
            ]}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
