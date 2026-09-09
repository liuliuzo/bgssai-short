import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from 'antd'
import aiModelsApi from '../../api/aiModels'

const { Title, Paragraph } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

const ABILITY_OPTIONS = [
  { value: 'TEXT', label: 'TEXT' },
  { value: 'IMAGE', label: 'IMAGE' },
  { value: 'VIDEO', label: 'VIDEO' },
  { value: 'VOICE', label: 'VOICE' },
  { value: 'ASR', label: 'ASR' },
  { value: 'MUSIC', label: 'MUSIC' },
  { value: 'COMPOSE', label: 'COMPOSE' },
]

export default function AiModelsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [abilityType, setAbilityType] = useState()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async (ability) => {
    setLoading(true)
    try {
      const { data } = await aiModelsApi.list({ ability_type: ability })
      setRows(data?.result?.model_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>AI 能力与模型</Title>
      <Paragraph type="secondary">单价单位为分；密钥写在「AI 密钥」页的 sys_secret，本页 config_json 只存 secret_ref。</Paragraph>
      <Card
        extra={
          <Button type="primary" onClick={() => {
            form.resetFields()
            form.setFieldsValue({
              quality_level: 'STANDARD',
              price_unit: 'PER_CALL',
              unit_price_cent: 50,
              timeout_sec: 120,
              max_retry: 2,
              enabled_flag: 1,
              default_flag: 0,
            })
            setOpen(true)
          }}
          >
            新增模型
          </Button>
        }
      >
        <Space style={{ marginBottom: 12 }}>
          <Select
            allowClear
            placeholder="能力类型"
            style={{ width: 160 }}
            value={abilityType}
            onChange={setAbilityType}
            options={ABILITY_OPTIONS}
          />
          <Button onClick={() => load(abilityType)}>查询</Button>
        </Space>
        <Table
          rowKey="model_id"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '能力', dataIndex: 'ability_type', width: 90 },
            { title: '模型', dataIndex: 'model_code' },
            { title: '名称', dataIndex: 'model_name' },
            { title: '供应商', dataIndex: 'provider_code', width: 120 },
            { title: '区域', dataIndex: 'region_label', width: 80, render: (v, row) => v || (row.region === 'intl' ? '国外' : '国内') },
            { title: '档位', dataIndex: 'quality_level', width: 100 },
            { title: '计费', dataIndex: 'price_unit', width: 120 },
            { title: '单价(元)', dataIndex: 'unit_price_cent', width: 100, render: yuan },
            { title: '默认', dataIndex: 'default_flag', width: 70, render: (v) => (v === 1 ? '是' : '-') },
            { title: '启用', dataIndex: 'enabled_flag', width: 70, render: (v) => (v === 1 ? '是' : '否') },
            {
              title: '操作',
              width: 220,
              render: (_, row) => (
                <Space>
                  {row.default_flag !== 1 ? (
                    <Button size="small" onClick={async () => {
                      await aiModelsApi.setDefault({ model_id: row.model_id })
                      message.success('已设默认')
                      load(abilityType)
                    }}
                    >
                      设默认
                    </Button>
                  ) : null}
                  <Button size="small" onClick={async () => {
                    await aiModelsApi.toggleEnabled({
                      model_id: row.model_id,
                      enabled_flag: row.enabled_flag === 1 ? 0 : 1,
                    })
                    message.success(row.enabled_flag === 1 ? '已停用' : '已启用')
                    load(abilityType)
                  }}
                  >
                    {row.enabled_flag === 1 ? '停用' : '启用'}
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title="新增模型"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          const { data } = await aiModelsApi.create({
            ...values,
            default_flag: Number(values.default_flag) === 1 ? 1 : 0,
            enabled_flag: Number(values.enabled_flag) === 1 ? 1 : 0,
          })
          if (data?.success) {
            message.success('已创建')
            setOpen(false)
            load(abilityType)
          }
        }}
        width={640}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="ability_type" label="能力类型" rules={[{ required: true }]}>
            <Select options={ABILITY_OPTIONS} />
          </Form.Item>
          <Form.Item name="provider_code" label="供应商" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="model_code" label="模型编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="model_name" label="模型名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="quality_level" label="质量档" rules={[{ required: true }]}>
            <Select options={[
              { value: 'FAST', label: 'FAST' },
              { value: 'STANDARD', label: 'STANDARD' },
              { value: 'HIGH', label: 'HIGH' },
            ]}
            />
          </Form.Item>
          <Form.Item name="price_unit" label="计费单位" rules={[{ required: true }]}>
            <Select options={[
              { value: 'PER_CALL', label: 'PER_CALL' },
              { value: 'PER_SECOND', label: 'PER_SECOND' },
              { value: 'PER_KILO_TOKEN', label: 'PER_KILO_TOKEN' },
              { value: 'PER_MINUTE', label: 'PER_MINUTE' },
            ]}
            />
          </Form.Item>
          <Form.Item name="unit_price_cent" label="单价(分)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="timeout_sec" label="超时(秒)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item name="max_retry" label="最大重试" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="config_json" label="config_json">
            <Input.TextArea rows={3} placeholder='{"base_url":"...","secret_ref":"ai.openai_compat.api_key"}' />
          </Form.Item>
          <Form.Item name="default_flag" label="设为默认">
            <Select options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
          </Form.Item>
          <Form.Item name="enabled_flag" label="启用">
            <Select options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
