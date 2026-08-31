import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Switch, Table, Typography, message } from 'antd'
import subscriptionPlansApi from '../../api/subscriptionPlans'

const { Title, Paragraph } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function SubscriptionPlansPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await subscriptionPlansApi.list()
      setRows(data?.result?.plan_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>订阅套餐</Title>
      <Paragraph type="secondary">创建或更新 plan_code；金额单位为分。</Paragraph>
      <Card extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>新建 / 更新</Button>}>
        <Table
          rowKey="plan_code"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '编码', dataIndex: 'plan_code' },
            { title: '名称', dataIndex: 'plan_name' },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            { title: '月度发放(元)', dataIndex: 'monthly_balance_cent', render: yuan },
            { title: '月度上限(元)', dataIndex: 'monthly_limit_cent', render: yuan },
            { title: '启用', dataIndex: 'is_active', render: (v) => (v === 1 ? '是' : '否') },
            {
              title: '操作',
              render: (_, row) => <Button size="small" onClick={() => { form.setFieldsValue({ ...row, is_active: row.is_active === 1 }); setOpen(true) }}>编辑</Button>,
            },
          ]}
        />
      </Card>
      <Modal title="订阅套餐" open={open} onCancel={() => setOpen(false)} onOk={async () => {
        const values = await form.validateFields()
        const { data } = await subscriptionPlansApi.save({
          ...values,
          is_active: values.is_active ? 1 : 0,
        })
        if (data?.success) {
          message.success('已保存')
          setOpen(false)
          load()
        }
      }}>
        <Form form={form} layout="vertical">
          <Form.Item name="plan_code" label="编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="plan_name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="price_cent" label="售价(分)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="monthly_balance_cent" label="月度发放(分)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="monthly_limit_cent" label="月度上限(分)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="sort_order" label="排序" initialValue={0}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="is_active" label="启用" valuePropName="checked" initialValue><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
