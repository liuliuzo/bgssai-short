import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Switch, Table, Tabs, Typography, message } from 'antd'
import { deployEnvLabel } from '../../lib/formatTime'
import payMerchantsApi from '../../api/payMerchants'

const { Title, Paragraph } = Typography

function ChannelTable({ rows, loading, onEdit, onActive, nameKey, extraKey }) {
  return (
    <Table
      rowKey="id"
      size="small"
      loading={loading}
      dataSource={rows}
      pagination={false}
      columns={[
        { title: '商户名', dataIndex: 'merchant_name' },
        { title: extraKey === 'mch_id' ? '商户号' : 'AppId', dataIndex: extraKey === 'mch_id' ? 'mch_id' : 'app_id' },
        { title: '部署环境 / 支付网关', dataIndex: 'env_mode', render: (v) => `${deployEnvLabel()} · ${v === 'SANDBOX' ? '沙箱网关' : '正式网关'}` },
        { title: '密钥掩码', key: 'secret_mask', render: (_, row) => row.app_private_key || row.mch_private_key || row.api_v3_key || '—' },
        { title: '启用', dataIndex: 'is_enabled', render: (v) => (v ? '是' : '否') },
        { title: '收款', dataIndex: 'is_collection_active', render: (v) => (v ? '是' : '否') },
        {
          title: '操作',
          render: (_, row) => (
            <>
              <Button size="small" onClick={() => onEdit(row)}>编辑</Button>
              {' '}
              <Button size="small" onClick={() => onActive(row.id)}>设为收款</Button>
            </>
          ),
        },
      ]}
    />
  )
}

export default function PayMerchantsPage() {
  const [alipay, setAlipay] = useState([])
  const [wechat, setWechat] = useState([])
  const [loading, setLoading] = useState(false)
  const [channel, setChannel] = useState('alipay')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [a, w] = await Promise.all([payMerchantsApi.listAlipay(), payMerchantsApi.listWechat()])
      setAlipay(Array.isArray(a.data?.result) ? a.data.result : [])
      setWechat(Array.isArray(w.data?.result) ? w.data.result : [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const save = async () => {
    const values = await form.validateFields()
    const body = { ...values, is_enabled: !!values.is_enabled }
    const { data } = channel === 'alipay'
      ? await payMerchantsApi.saveAlipay(body)
      : await payMerchantsApi.saveWechat(body)
    if (data?.success) {
      message.success('已保存')
      setOpen(false)
      load()
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>支付商户</Title>
      <Paragraph type="secondary">管理支付宝与微信支付收款商户配置。敏感密钥仅展示掩码，编辑时留空即保留原有配置。</Paragraph>
      <Card extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>新建</Button>}>
        <Tabs
          onChange={setChannel}
          items={[
            {
              key: 'alipay',
              label: '支付宝',
              children: <ChannelTable rows={alipay} loading={loading} extraKey="app_id" onEdit={(row) => {
                form.setFieldsValue({
                  ...row,
                  app_private_key: '',
                  alipay_public_key: '',
                })
                setChannel('alipay')
                setOpen(true)
              }} onActive={async (id) => { await payMerchantsApi.activeAlipay(id); load() }} />,
            },
            {
              key: 'wechat',
              label: '微信',
              children: <ChannelTable rows={wechat} loading={loading} extraKey="mch_id" onEdit={(row) => {
                form.setFieldsValue({
                  ...row,
                  api_v3_key: '',
                  mch_private_key: '',
                  wechat_public_key: '',
                  apiclient_cert: undefined,
                  mch_cert_p12: undefined,
                  api_v2_key: undefined,
                })
                setChannel('wechat')
                setOpen(true)
              }} onActive={async (id) => { await payMerchantsApi.activeWechat(id); load() }} />,
            },
          ]}
        />
      </Card>
      <Modal title={channel === 'alipay' ? '支付宝商户' : '微信商户'} open={open} onCancel={() => setOpen(false)} onOk={save} width={640}>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="merchant_name" label="商户名" rules={[{ required: true }]}><Input /></Form.Item>
          {channel === 'alipay' ? (
            <>
              <Form.Item name="app_id" label="AppId" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="app_private_key" label="应用私钥"><Input.TextArea rows={3} placeholder="留空表示保持库中原值" /></Form.Item>
              <Form.Item name="alipay_public_key" label="支付宝公钥"><Input.TextArea rows={3} placeholder="留空表示保持库中原值" /></Form.Item>
            </>
          ) : (
            <>
              <Form.Item name="mch_id" label="商户号" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="app_id" label="AppId" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="merchant_serial_no" label="商户证书序列号" rules={[{ required: true }]}><Input placeholder="API 证书序列号，生产下单必填" /></Form.Item>
              <Form.Item name="api_v3_key" label="APIv3 Key"><Input placeholder="留空表示保持库中原值" /></Form.Item>
              <Form.Item name="mch_private_key" label="商户私钥"><Input.TextArea rows={3} placeholder="留空表示保持库中原值" /></Form.Item>
              <Form.Item name="wechat_public_key_id" label="微信支付公钥 ID"><Input /></Form.Item>
              <Form.Item name="wechat_public_key" label="微信支付公钥"><Input.TextArea rows={3} placeholder="留空表示保持库中原值" /></Form.Item>
            </>
          )}
          <Form.Item name="notify_url" label="回调 URL" rules={[{ required: true }]}><Input placeholder="https://…/bgssai/user/payment/{wechat|alipay}/notify" /></Form.Item>
          <Form.Item name="env_mode" label="支付网关模式（PRODUCTION 正式 / SANDBOX 沙箱）" initialValue="PRODUCTION"><Input /></Form.Item>
          <Form.Item name="is_enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
