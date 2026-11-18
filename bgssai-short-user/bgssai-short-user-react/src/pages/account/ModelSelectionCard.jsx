import { useState } from 'react'
import { Alert, AutoComplete, Button, Card, Checkbox, Form, Input, Modal, Popconfirm, Segmented, Select, Space, Switch, Table, Tag, message } from 'antd'
import api from '../../api/userAiModelKeys'
import { ABILITY_LABEL, modelSelectionFields, modelOptions, modelsForAbility } from './modelKeySelection'

const filters = [
  { value: 'ALL', label: '全部' },
  ...Object.entries(ABILITY_LABEL).map(([value, label]) => ({ value, label: value === 'VOICE' ? '配音' : label })),
]
const required = [{ required: true, whitespace: true, message: '请填写此项' }]

export default function ModelSelectionCard({ rows, presets, credentials, busy, disabled, loading, run }) {
  const [filter, setFilter] = useState('ALL')
  const [dialog, setDialog] = useState(null)
  const [form] = Form.useForm()
  const code = Form.useWatch('provider_code', form)
  const ability = Form.useWatch('ability_type', form)
  const picked = Form.useWatch('abilities', form) || []
  const configured = modelOptions(rows)
  const ready = new Set(credentials.filter((credential) => Number(credential.active) === 1).map((credential) => credential.provider_code))
  const providers = presets.filter((p) => ready.has(p.provider_code))
  const addProvider = providers.find((p) => filter === 'ALL' || p.abilities?.some((a) => a.ability_type === filter))
  const preset = presets.find((p) => p.provider_code === code)
  const abilities = preset?.abilities || []
  const models = abilities.find((item) => item.ability_type === ability)?.models || []
  const editing = !!dialog?.row
  const batch = dialog?.batch

  function hasModel(provider, type, model, except) {
    return configured.some((row) => Number(row.platform_model) !== 1 && row.key_id !== except && row.provider_code === provider
      && row.ability_type === type && row.model_code === model.trim())
  }

  function chooseProvider(provider, preferredAbility, multiple) {
    const available = provider?.abilities || []
    const next = available.find((a) => a.ability_type === preferredAbility) || available[0]
    const byAbility = {}
    for (const item of available) {
      const first = item.models?.find((m) => !hasModel(provider.provider_code, item.ability_type, m.model_code))
      if (first) byAbility[item.ability_type] = first.model_code
    }
    const batchAbilities = Object.keys(byAbility).filter((type) => filter === 'ALL' || type === filter)
    const first = next?.models?.find((model) => model.model_code === byAbility[next.ability_type])
    form.setFieldsValue({
      provider_code: provider?.provider_code, ability_type: next?.ability_type,
      model_code: first?.model_code || '', model_name: first?.model_name || '',
      abilities: multiple ? batchAbilities : [], models_by_ability: byAbility,
      activate_on_create: !configured.some((row) => Number(row.platform_model) !== 1 && row.provider_code === provider?.provider_code),
    })
  }

  function open(row, multiple = false) {
    form.resetFields()
    const provider = row ? presets.find((p) => p.provider_code === row.provider_code) : addProvider
    chooseProvider(provider, filter, multiple)
    if (row) form.setFieldsValue({ provider_code: row.provider_code, ability_type: row.ability_type, model_code: row.model_code, model_name: row.model_name })
    setDialog({ row, batch: multiple })
  }

  async function save(values) {
    let payload
    if (batch) {
      const items = values.abilities.map((type) => {
        const code = values.models_by_ability[type]
        const model = abilities.find((a) => a.ability_type === type)?.models?.find((m) => m.model_code === code)
        return modelSelectionFields(type, code, model?.model_name || code)
      })
      payload = { provider_code: values.provider_code, items, activate_on_create: !!values.activate_on_create }
    } else {
      payload = {
        provider_code: values.provider_code,
        ...modelSelectionFields(values.ability_type, values.model_code, values.model_name),
      }
      if (hasModel(values.provider_code, values.ability_type, values.model_code, dialog.row?.key_id)) {
        message.warning('该能力已添加此模型，可直接在列表中选用')
        return
      }
      if (editing) {
        payload.key_id = dialog.row.key_id
        payload.provider_model_selection = true
      } else {
        payload.activate_on_create = !!values.activate_on_create
      }
    }
    const request = editing ? () => api.update(payload) : () => api.create(payload)
    if (await run('save-model', request, '模型配置已保存')) setDialog(null)
  }

  const columns = [
    { title: '能力', dataIndex: 'ability_type', render: (type) => ABILITY_LABEL[type] || type },
    { title: '供应商', dataIndex: 'provider_name', render: (name, row) => name || row.provider_code },
    { title: '模型', dataIndex: 'model_name' },
    { title: '模型编码', dataIndex: 'model_code' },
    { title: '状态', render: (_, row) => {
      const platform = Number(row.platform_model) === 1
      const enabled = Number(row.enabled) === 1
      const needsCredential = !enabled && !platform && !ready.has(row.provider_code)
      return (
        <Space size={6}>
          <Switch size="small" checked={enabled} disabled={disabled || needsCredential}
            aria-label={`启用${ABILITY_LABEL[row.ability_type] || row.ability_type}模型 ${row.model_name || row.model_code}`}
            title={needsCredential ? '请先在密钥管理中选用该供应商的密钥' : undefined}
            loading={busy === 'toggle-' + row.key_id}
            onChange={(checked) => run('toggle-' + row.key_id, () => api.toggle({
              key_id: row.key_id, enabled: checked ? 1 : 0, provider_model_selection: !platform,
            }), checked ? '已选用模型' : '已停用模型')} />
          {enabled ? <Tag color="blue">{Number(row.default_model) === 1 ? '默认模型' : '已启用'}</Tag> : '备用'}
        </Space>
      )
    } },
    { title: '操作', render: (_, row) => {
      const platform = Number(row.platform_model) === 1
      const enabled = Number(row.enabled) === 1
      const canSelect = platform || ready.has(row.provider_code)
      return (
        <Space wrap size={4}>
          {enabled && Number(row.default_model) !== 1 && <Button size="small" disabled={disabled || !canSelect}
            onClick={() => run('set-default', () => api.setDefault({ key_id: row.key_id, provider_model_selection: !platform }), '已设为默认模型')}>设为默认</Button>}
          {!platform && <>
            <Button size="small" disabled={disabled || !canSelect} onClick={() => open(row)}>编辑</Button>
            <Popconfirm title={'移除「' + row.model_name + '」模型配置？'} onConfirm={() => run('remove-model', () => api.remove({ key_id: row.key_id, provider_model_selection: true }), '模型配置已移除')}>
              <Button size="small" disabled={disabled} danger>移除</Button>
            </Popconfirm>
          </>}
        </Space>
      )
    } },
  ]
  return (
    <Card title="模型选择" style={{ marginBottom: 16 }}
      extra={<Space wrap>
        <Button disabled={disabled || !addProvider} title={!disabled && !addProvider ? '请先选用支持此能力的供应商密钥' : undefined} onClick={() => open(null, true)}>批量添加</Button>
        <Button type="primary" disabled={disabled || !addProvider} title={!disabled && !addProvider ? '请先选用支持此能力的供应商密钥' : undefined} onClick={() => open(null)}>添加模型</Button>
      </Space>}>
      {!loading && !disabled && !credentials.length
        && <Alert showIcon type="info" message="可直接选用 BGSSAI 内置模型；添加供应商模型前，请先在密钥管理中保存密钥。" style={{ marginBottom: 12 }} />}
      <div style={{ maxWidth: '100%', overflowX: 'auto', marginBottom: 12 }}>
        <Segmented options={filters} value={filter} onChange={setFilter} />
      </div>
      <Table size="small" rowKey="key_id" pagination={false} loading={loading} scroll={{ x: 650 }} columns={columns}
        dataSource={modelsForAbility(configured, filter)}
        locale={{ emptyText: '暂无该能力的模型配置' }} />
      <Modal open={!!dialog} title={editing ? '编辑模型' : batch ? '批量添加模型' : '添加模型'} forceRender footer={null}
        onCancel={() => { if (!busy) setDialog(null) }}>
        <Form form={form} layout="vertical" onFinish={save}>
          <Form.Item name="provider_code" label="供应商" rules={required}>
            <Select disabled={editing} options={providers.map((p) => ({ value: p.provider_code, label: p.provider_name }))}
              onChange={(value) => chooseProvider(presets.find((p) => p.provider_code === value), ability, batch)} />
          </Form.Item>
          {batch ? <>
            <Form.Item name="abilities" label="模型能力" rules={[{ type: 'array', required: true, min: 1, message: '请至少选择一种模型能力' }]}>
              <Checkbox.Group options={abilities.map((a) => ({
                value: a.ability_type, label: ABILITY_LABEL[a.ability_type] || a.ability_label,
                disabled: !(a.models || []).some((m) => !hasModel(code, a.ability_type, m.model_code)),
              }))} />
            </Form.Item>
            {abilities.filter((a) => picked.includes(a.ability_type)).map((a) => (
              <Form.Item key={a.ability_type} name={['models_by_ability', a.ability_type]} label={ABILITY_LABEL[a.ability_type] + '模型'} rules={required}>
                <Select options={(a.models || []).filter((m) => !hasModel(code, a.ability_type, m.model_code))
                  .map((m) => ({ value: m.model_code, label: m.model_name + ' (' + m.model_code + ')' }))} />
              </Form.Item>
            ))}
          </> : <>
            <Form.Item name="ability_type" label="模型能力" rules={required}>
              <Select disabled={editing} options={abilities.map((a) => ({ value: a.ability_type, label: ABILITY_LABEL[a.ability_type] || a.ability_label }))}
                onChange={(type) => {
                  const first = abilities.find((a) => a.ability_type === type)?.models?.find((m) => !hasModel(code, type, m.model_code))
                  form.setFieldsValue({ model_code: first?.model_code || '', model_name: first?.model_name || '' })
                }} />
            </Form.Item>
            <Form.Item name="model_code" label="模型编码" rules={required}>
              <AutoComplete placeholder="选择预设模型或输入自定义编码" options={models.map((m) => ({
                value: m.model_code, label: m.model_name + ' (' + m.model_code + ')', disabled: hasModel(code, ability, m.model_code, dialog?.row?.key_id),
              }))} onChange={(value) => form.setFieldValue('model_name', models.find((m) => m.model_code === value)?.model_name || value)} />
            </Form.Item>
            <Form.Item name="model_name" label="模型名称" rules={required}><Input /></Form.Item>
          </>}
          {!editing && <Form.Item name="activate_on_create" valuePropName="checked"><Checkbox>保存后启用</Checkbox></Form.Item>}
          <Button type="primary" htmlType="submit" disabled={!!busy} loading={busy === 'save-model'}>保存</Button>
        </Form>
      </Modal>
    </Card>
  )
}
