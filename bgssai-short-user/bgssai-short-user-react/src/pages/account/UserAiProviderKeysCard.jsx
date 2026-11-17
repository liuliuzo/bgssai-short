import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Segmented,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import userAiKeysApi from '../../api/userAiKeys'
import { buildProviderKeyPayload, formatProviderStatus } from './userAiProviderKeysHelper'

export default function UserAiProviderKeysCard() {
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState([])
  const [editing, setEditing] = useState(null)
  const [authMode, setAuthMode] = useState('API_KEY')
  const [submitting, setSubmitting] = useState(false)
  const [togglingCode, setTogglingCode] = useState('')
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await userAiKeysApi.list()
      setItems(Array.isArray(data?.items) ? data.items : [])
    } catch (err) {
      message.error(err?.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const openModal = (record) => {
    const mode = record.supports_ak_sk && record.auth_mode === 'AK_SK' ? 'AK_SK' : 'API_KEY'
    setAuthMode(mode)
    setEditing(record)
    form.setFieldsValue({
      apiKey: '',
      accessKey: '',
      secretKey: '',
      baseUrl: record.base_url || '',
      enabled: record.configured ? Boolean(record.enabled) : true,
    })
  }

  const handleSave = async () => {
    if (!editing) {
      return
    }
    const values = await form.validateFields()
    const { payload, error } = buildProviderKeyPayload({
      providerCode: editing.provider_code,
      authMode,
      supportsAkSk: editing.supports_ak_sk,
      configured: editing.configured,
      apiKey: values.apiKey,
      accessKey: values.accessKey,
      secretKey: values.secretKey,
      baseUrl: values.baseUrl,
      enabled: values.enabled,
    })
    if (error) {
      message.error(error)
      return
    }
    setSubmitting(true)
    try {
      await userAiKeysApi.upsert(payload)
      message.success('已保存')
      setEditing(null)
      await load()
    } catch (err) {
      message.error(err?.message || '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggle = async (record, checked) => {
    setTogglingCode(record.provider_code)
    try {
      await userAiKeysApi.upsert({
        provider_code: record.provider_code,
        enabled: checked,
      })
      await load()
    } catch (err) {
      message.error(err?.message || '更新失败')
    } finally {
      setTogglingCode('')
    }
  }

  const handleDelete = async (providerCode) => {
    try {
      await userAiKeysApi.remove(providerCode)
      message.success('已删除')
      await load()
    } catch (err) {
      message.error(err?.message || '删除失败')
    }
  }

  const columns = [
    {
      title: '厂商',
      key: 'provider',
      render: (_, r) => (
        <Space size={6}>
          <span>{r.provider_name}</span>
          <Typography.Text type="secondary">{r.provider_code}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_, r) => {
        const label = formatProviderStatus(r)
        if (!r.configured) {
          return <Tag>{label}</Tag>
        }
        return <Tag color={r.enabled ? 'green' : 'default'}>{label}</Tag>
      },
    },
    {
      title: '密钥',
      key: 'masked',
      render: (_, r) =>
        r.configured ? (
          <Space size={6}>
            <Typography.Text code>{r.masked_key || '-'}</Typography.Text>
            {r.auth_mode === 'AK_SK' && <Tag>AK/SK</Tag>}
          </Space>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    {
      title: '接口地址',
      key: 'baseUrl',
      render: (_, r) => (
        <Typography.Text type={r.base_url ? undefined : 'secondary'}>
          {r.base_url || r.default_base_url || '-'}
        </Typography.Text>
      ),
    },
    {
      title: '启用',
      key: 'enabled',
      width: 80,
      render: (_, r) =>
        r.configured ? (
          <Switch
            size="small"
            checked={Boolean(r.enabled)}
            loading={togglingCode === r.provider_code}
            onChange={(checked) => handleToggle(r, checked)}
          />
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_, r) => (
        <Space size={8}>
          <Button type="link" size="small" style={{ padding: 0 }} onClick={() => openModal(r)}>
            {r.configured ? '更新' : '配置'}
          </Button>
          {r.configured && (
            <Popconfirm title="删除该厂商密钥？" onConfirm={() => handleDelete(r.provider_code)}>
              <Button type="link" danger size="small" style={{ padding: 0 }}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <>
      <Card
        title="厂商 API Key"
        loading={loading && items.length === 0}
        style={{ marginBottom: 16 }}
      >
        <Table
          size="small"
          rowKey="provider_code"
          dataSource={items}
          columns={columns}
          pagination={false}
        />
      </Card>

      <Modal
        open={Boolean(editing)}
        title={editing ? `${editing.provider_name} (${editing.provider_code})` : ''}
        onCancel={() => setEditing(null)}
        onOk={handleSave}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          {editing?.supports_ak_sk && (
            <Form.Item label="鉴权方式">
              <Segmented
                value={authMode}
                onChange={(val) => setAuthMode(val)}
                options={[
                  { label: 'API Key', value: 'API_KEY' },
                  { label: 'Access Key + Secret Key', value: 'AK_SK' },
                ]}
              />
            </Form.Item>
          )}

          {editing?.supports_ak_sk && authMode === 'AK_SK' ? (
            <>
              <Form.Item name="accessKey" label="Access Key">
                <Input
                  autoComplete="off"
                  placeholder={editing?.masked_access_key || 'Access Key'}
                />
              </Form.Item>
              <Form.Item name="secretKey" label="Secret Key">
                <Input.Password
                  autoComplete="new-password"
                  placeholder={editing?.has_secret_key ? '留空保持不变' : 'Secret Key'}
                />
              </Form.Item>
            </>
          ) : (
            <Form.Item name="apiKey" label="API Key">
              <Input.Password
                autoComplete="new-password"
                placeholder={editing?.configured ? editing?.masked_key || '留空保持不变' : 'API Key'}
              />
            </Form.Item>
          )}

          <Form.Item name="baseUrl" label="接口地址">
            <Input
              autoComplete="off"
              allowClear
              placeholder={editing?.default_base_url || 'https://...'}
            />
          </Form.Item>

          <Form.Item name="enabled" label="启用状态" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
