import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Space, Switch, Table, Typography, message } from 'antd'
import secretsApi from '../../api/secrets'

const { Title, Paragraph } = Typography

export default function AiSecretsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await secretsApi.list({ key_prefix: 'ai.' })
      setRows(data?.result?.secret_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>AI 密钥</Title>
      <Paragraph type="secondary">
        业务第三方凭证落库（sys_secret）。中间件参数仍写 application-*.properties。演示前至少配置
        ai.tokenhub_cn.api_key 与 ai.tokenhub_global.api_key（可空值回填自旧键 ai.tencent_tokenhub.api_key）。
      </Paragraph>
      <Card>
        <Table
          rowKey="secret_key"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '密钥键', dataIndex: 'secret_key' },
            { title: '说明', dataIndex: 'description' },
            {
              title: '已配置',
              dataIndex: 'configured_flag',
              width: 90,
              render: (v) => (v === 1 ? '是' : '否'),
            },
            { title: '掩码', dataIndex: 'secret_value_masked', width: 160 },
            {
              title: '启用',
              dataIndex: 'enabled',
              width: 80,
              render: (v) => (v === 1 ? '是' : '否'),
            },
            {
              title: '操作',
              width: 220,
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={() => {
                    setEditing(row)
                    form.setFieldsValue({
                      secret_key: row.secret_key,
                      secret_value: '',
                      enabled: row.enabled === 1,
                      description: row.description,
                    })
                    setOpen(true)
                  }}
                  >
                    编辑
                  </Button>
                  <Button size="small" onClick={async () => {
                    const { data } = await secretsApi.test({ secret_key: row.secret_key })
                    if (data?.result?.ok) {
                      message.success(data.result.message)
                    } else {
                      message.warning(data?.result?.message || '未配置')
                    }
                  }}
                  >
                    测试
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title="更新 AI 密钥"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          const body = {
            secret_key: values.secret_key,
            enabled: values.enabled ? 1 : 0,
            description: values.description,
          }
          if (values.secret_value != null && String(values.secret_value).length > 0) {
            body.secret_value = values.secret_value
          }
          await secretsApi.update(body)
          message.success('已保存')
          setOpen(false)
          load()
        }}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="secret_key" label="密钥键">
            <Input disabled />
          </Form.Item>
          <Form.Item
            name="secret_value"
            label="密钥值"
            extra={editing?.configured_flag === 1 ? `当前掩码 ${editing.secret_value_masked}；留空表示不改值` : '当前为空'}
          >
            <Input.Password placeholder="粘贴 API Key（留空不改）" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
