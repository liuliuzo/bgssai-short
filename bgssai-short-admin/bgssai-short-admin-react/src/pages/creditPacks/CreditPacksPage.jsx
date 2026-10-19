import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Switch, Table, Typography, message } from 'antd'
import creditPacksApi from '../../api/creditPacks'
import { centToYuanNumber, yuanToCent } from '../../lib/money'

const { Title, Paragraph } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function CreditPacksPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await creditPacksApi.list()
      setRows(data?.result?.pack_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>加量包</Title>
      <Paragraph type="secondary">售价和到账额度按元填写。</Paragraph>
      <Card extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>新建 / 更新</Button>}>
        <Table
          rowKey="pack_code"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '编码', dataIndex: 'pack_code' },
            { title: '名称', dataIndex: 'name' },
            { title: '到账(元)', dataIndex: 'credit_cent', render: yuan },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            { title: '启用', dataIndex: 'is_active', render: (v) => (v === 1 ? '是' : '否') },
            {
              title: '操作',
              render: (_, row) => <Button size="small" onClick={() => {
                form.setFieldsValue({
                  pack_code: row.pack_code,
                  name: row.name,
                  credit_yuan: centToYuanNumber(row.credit_cent),
                  price_yuan: centToYuanNumber(row.price_cent),
                  sort_order: row.sort_order,
                  is_active: row.is_active === 1,
                })
                setOpen(true)
              }}>编辑</Button>,
            },
          ]}
        />
      </Card>
      <Modal title="加量包" open={open} onCancel={() => setOpen(false)} onOk={async () => {
        const values = await form.validateFields()
        const { data } = await creditPacksApi.save({
          pack_code: values.pack_code,
          name: values.name,
          credit_cent: yuanToCent(values.credit_yuan),
          price_cent: yuanToCent(values.price_yuan),
          sort_order: values.sort_order,
          is_active: values.is_active ? 1 : 0,
        })
        if (data?.success) {
          message.success('已保存')
          setOpen(false)
          load()
        }
      }}>
        <Form form={form} layout="vertical">
          <Form.Item name="pack_code" label="编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="credit_yuan" label="到账(元)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} step={0.01} /></Form.Item>
          <Form.Item name="price_yuan" label="售价(元)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} step={0.01} /></Form.Item>
          <Form.Item name="sort_order" label="排序" initialValue={0}><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="is_active" label="启用" valuePropName="checked" initialValue><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
