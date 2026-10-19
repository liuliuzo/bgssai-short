import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from 'antd'
import creatorsApi from '../../api/creators'

const { Title, Paragraph } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function CreatorsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [enabledFlag, setEnabledFlag] = useState()
  const [quotaUser, setQuotaUser] = useState(null)
  const [form] = Form.useForm()

  const load = useCallback(async (num, size, kw, enabled) => {
    setLoading(true)
    try {
      const { data } = await creatorsApi.list({
        page_num: num,
        page_size: size,
        keyword: kw,
        enabled_flag: enabled,
      })
      setRows(Array.isArray(data?.result) ? data.result : [])
      setTotal(Number(data?.total_size) || 0)
      setPageNum(Number(data?.page_num) || num)
      setPageSize(Number(data?.page_size) || size)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(1, 20) }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>创作者</Title>
      <Paragraph type="secondary">可按用户名或姓名查找，冻结后该创作者无法继续制作，也可以调整额度。</Paragraph>
      <Card>
        <Space style={{ marginBottom: 12 }}>
          <Input placeholder="用户名 / 姓名 / user_id" value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 220 }} />
          <Select allowClear placeholder="全部" style={{ width: 140 }} value={enabledFlag} onChange={setEnabledFlag}
            options={[{ value: 1, label: '正常' }, { value: 0, label: '冻结' }]} />
          <Button onClick={() => load(1, pageSize, keyword, enabledFlag)}>查询</Button>
        </Space>
        <Table
          rowKey="user_id"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (n) => `共 ${n} 条`,
            onChange: (num, size) => load(num, size, keyword, enabledFlag),
          }}
          columns={[
            { title: '用户名', dataIndex: 'user_name' },
            { title: '姓名', dataIndex: 'real_name' },
            { title: '状态', dataIndex: 'enabled_flag', width: 80, render: (v) => (v === 1 ? '正常' : '冻结') },
            { title: '周期余额', dataIndex: 'balance_cent', render: yuan },
            { title: '加量余额', dataIndex: 'extra_balance_cent', render: yuan },
            {
              title: '操作',
              width: 200,
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={async () => {
                    await creatorsApi.toggle({ user_id: row.user_id, enabled_flag: row.enabled_flag === 1 ? 0 : 1 })
                    message.success(row.enabled_flag === 1 ? '已冻结' : '已解冻')
                    load(pageNum, pageSize, keyword, enabledFlag)
                  }}>{row.enabled_flag === 1 ? '冻结' : '解冻'}</Button>
                  <Button size="small" onClick={() => { setQuotaUser(row); form.resetFields() }}>调额</Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Modal title={`调额 ${quotaUser?.user_name || ''}`} open={!!quotaUser} onCancel={() => setQuotaUser(null)}
        onOk={async () => {
          const values = await form.validateFields()
          const { data } = await creatorsApi.adjustQuota({
            user_id: quotaUser.user_id,
            balance_delta_cent: values.balance_delta_cent,
            extra_delta_cent: values.extra_delta_cent,
            monthly_limit_cent: values.monthly_limit_cent,
            remark: values.remark,
          })
          if (data?.success) {
            message.success('已调额')
            setQuotaUser(null)
            load(pageNum, pageSize, keyword, enabledFlag)
          }
        }}>
        <Form form={form} layout="vertical">
          <Form.Item name="balance_delta_cent" label="周期余额增量(分)" initialValue={0}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="extra_delta_cent" label="加量余额增量(分)" initialValue={0}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="monthly_limit_cent" label="月度上限(分)"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="remark" label="备注" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
