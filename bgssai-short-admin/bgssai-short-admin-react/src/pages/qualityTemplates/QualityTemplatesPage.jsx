import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Space, Table, Tag, Typography, message } from 'antd'
import qualityApi from '../../api/qualityTemplates'

const { Title } = Typography

export default function QualityTemplatesPage() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()
  const [thresholdForm] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await qualityApi.listTemplates()
      setList(data?.result?.template_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>质量结构模板</Title>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Card title="新建模板">
          <Form
            form={form}
            layout="inline"
            onFinish={async (values) => {
              const body = {
                template_name: values.template_name,
                genre: values.genre,
                hook_type_list: (values.hook_type_list || '').split(',').map((s) => s.trim()).filter(Boolean),
                taboo_list: (values.taboo_list || '').split(',').map((s) => s.trim()).filter(Boolean),
                beat_sheet_json: { note: values.beat_note || '' },
                turning_point_json: { note: values.turning_note || '' },
                enabled_flag: 1,
              }
              const { data } = await qualityApi.createTemplate(body)
              if (data?.success) {
                message.success('已创建')
                form.resetFields()
                await load()
              }
            }}
          >
            <Form.Item name="template_name" label="名称" rules={[{ required: true }]}>
              <Input style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="genre" label="题材" rules={[{ required: true }]}>
              <Input style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="hook_type_list" label="钩子">
              <Input style={{ width: 180 }} placeholder="逗号分隔" />
            </Form.Item>
            <Form.Item name="taboo_list" label="禁忌">
              <Input style={{ width: 180 }} placeholder="逗号分隔" />
            </Form.Item>
            <Form.Item name="beat_note" label="节拍备注"><Input style={{ width: 140 }} /></Form.Item>
            <Button type="primary" htmlType="submit">创建</Button>
          </Form>
        </Card>
        <Card title="模板列表" loading={loading}>
          <Table
            rowKey="template_id"
            size="small"
            dataSource={list}
            columns={[
              { title: 'ID', dataIndex: 'template_id', width: 160 },
              { title: '名称', dataIndex: 'template_name' },
              { title: '题材', dataIndex: 'genre', width: 100 },
              {
                title: '启用',
                dataIndex: 'enabled_flag',
                width: 90,
                render: (v) => (v === 1 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
              },
              {
                title: '操作',
                width: 160,
                render: (_, row) => (
                  <Button size="small" onClick={async () => {
                    await qualityApi.toggleEnabled({ template_id: row.template_id })
                    message.success('已切换')
                    await load()
                  }}>启停</Button>
                ),
              },
            ]}
          />
        </Card>
        <Card title="默认阈值">
          <Form
            form={thresholdForm}
            layout="inline"
            initialValues={{ genre: 'DEFAULT', default_threshold_score: 75, first_episode_threshold_score: 85 }}
            onFinish={async (values) => {
              await qualityApi.thresholdSave(values)
              message.success('阈值已保存')
            }}
          >
            <Form.Item name="genre" label="题材"><Input style={{ width: 120 }} /></Form.Item>
            <Form.Item name="default_threshold_score" label="默认"><InputNumber min={1} max={100} /></Form.Item>
            <Form.Item name="first_episode_threshold_score" label="首集"><InputNumber min={1} max={100} /></Form.Item>
            <Button type="primary" htmlType="submit">保存</Button>
          </Form>
        </Card>
      </Space>
    </div>
  )
}
