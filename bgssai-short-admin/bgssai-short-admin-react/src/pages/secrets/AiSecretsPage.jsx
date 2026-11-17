import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Checkbox, Form, Input, Modal, Select, Space, Switch, Table, Tabs, Typography, message } from 'antd'
import secretsApi from '../../api/secrets'

const { Title } = Typography

const ENDPOINT_OPTIONS = [
  { value: 'sms.tencentcloudapi.com', label: 'sms.tencentcloudapi.com（国内站）' },
  { value: 'sms.intl.tencentcloudapi.com', label: 'sms.intl.tencentcloudapi.com（国际站）' },
]
const DOMESTIC_REGION_OPTIONS = [
  { value: 'ap-guangzhou', label: 'ap-guangzhou' },
  { value: 'ap-beijing', label: 'ap-beijing' },
  { value: 'ap-nanjing', label: 'ap-nanjing' },
]
const INTL_REGION_OPTIONS = [
  { value: 'ap-guangzhou', label: 'ap-guangzhou' },
  { value: 'ap-beijing', label: 'ap-beijing' },
  { value: 'ap-nanjing', label: 'ap-nanjing' },
  { value: 'ap-singapore', label: 'ap-singapore' },
  { value: 'eu-frankfurt', label: 'eu-frankfurt' },
]
const PARAM_COUNT_OPTIONS = [
  { value: '2', label: '2（[验证码, 分钟]）' },
  { value: '3', label: '3（[产品名, 验证码, 分钟]）' },
  { value: '4', label: '4（[产品名, 序号, 验证码, 分钟]）' },
]
const ENABLED_OPTIONS = [
  { value: '0', label: '0（关闭）' },
  { value: '1', label: '1（开启）' },
]

export default function AiSecretsPage() {
  const [tab, setTab] = useState('ai')
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [clearSecret, setClearSecret] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      if (tab === 'sms') {
        const [domRes, extRes] = await Promise.all([
          secretsApi.list({ key_prefix: 'bgssai.sms.' }),
          secretsApi.list({ key_prefix: 'sms.' }),
        ])
        const domList = domRes.data?.result?.secret_list || []
        const extList = extRes.data?.result?.secret_list || []
        setRows([...domList, ...extList])
      } else {
        const { data } = await secretsApi.list({ key_prefix: 'ai.' })
        setRows(data?.result?.secret_list || [])
      }
    } finally {
      setLoading(false)
    }
  }, [tab])

  useEffect(() => { load() }, [load])

  const runTest = async (secretKey) => {
    const { data } = await secretsApi.test({ secret_key: secretKey })
    if (data?.result?.ok) {
      message.success(data.result.message)
    } else {
      message.warning(data?.result?.message || '未配置')
    }
  }

  const renderValueControl = () => {
    const key = editing?.secret_key || ''
    if (editing?.is_sensitive === false) {
      if (key === 'sms.endpoint' || key === 'sms.intl.endpoint') {
        return <Select options={ENDPOINT_OPTIONS} />
      }
      if (key === 'sms.region') {
        return <Select options={DOMESTIC_REGION_OPTIONS} />
      }
      if (key === 'sms.intl.region') {
        return <Select options={INTL_REGION_OPTIONS} />
      }
      if (key === 'sms.template_param_count' || key === 'sms.intl.template_param_count') {
        return <Select options={PARAM_COUNT_OPTIONS} />
      }
      if (key === 'sms.intl.enabled') {
        return <Select options={ENABLED_OPTIONS} />
      }
      return <Input placeholder="可留空" />
    }
    return <Input.Password placeholder="留空不改" disabled={clearSecret} />
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>密钥与通道配置</Title>
      <Card
        extra={tab === 'sms' ? (
          <Space>
            <Button onClick={() => runTest('sms.domestic')}>检测国内通道</Button>
            <Button onClick={() => runTest('sms.intl')}>检测国际通道</Button>
          </Space>
        ) : null}
      >
        <Tabs
          activeKey={tab}
          onChange={setTab}
          items={[
            { key: 'ai', label: 'AI 密钥' },
            { key: 'sms', label: '短信通道' },
          ]}
        />
        <Table
          rowKey="secret_key"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '密钥', dataIndex: 'secret_key' },
            { title: '说明', dataIndex: 'description' },
            {
              title: '已配置',
              dataIndex: 'configured_flag',
              width: 90,
              render: (v) => (v === 1 ? '是' : '否'),
            },
            {
              title: '当前值 / 掩码',
              width: 220,
              render: (_, row) => (row.is_sensitive === false ? (row.config_value || '') : (row.secret_value_masked || '')),
            },
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
                    setClearSecret(false)
                    form.setFieldsValue({
                      secret_key: row.secret_key,
                      secret_value: row.is_sensitive === false ? (row.config_value ?? '') : '',
                      enabled: row.enabled === 1,
                      description: row.description,
                    })
                    setOpen(true)
                  }}
                  >
                    编辑
                  </Button>
                  <Button size="small" onClick={() => runTest(row.secret_key)}>
                    测试
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title="更新配置"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          const body = {
            secret_key: values.secret_key,
            enabled: values.enabled ? 1 : 0,
            description: values.description,
          }
          if (clearSecret) {
            body.secret_value = ''
          } else if (editing?.is_sensitive === false) {
            body.secret_value = String(values.secret_value ?? '').trim()
          } else if (values.secret_value != null && String(values.secret_value).length > 0) {
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
            label="配置值 / 密钥值"
            extra={editing?.is_sensitive !== false && editing?.configured_flag === 1 ? editing.secret_value_masked : undefined}
          >
            {renderValueControl()}
          </Form.Item>
          {editing?.is_sensitive !== false && editing?.secret_key?.startsWith('sms.intl.') && (
            <Form.Item>
              <Checkbox checked={clearSecret} onChange={(e) => setClearSecret(e.target.checked)}>
                清空该密钥以复用国内凭证
              </Checkbox>
            </Form.Item>
          )}
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
