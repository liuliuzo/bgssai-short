import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Checkbox, Form, Input, Modal, Popconfirm, Segmented, Select, Space, Switch, Table, Tag, message } from 'antd'
import userAiModelKeysApi from '../../api/userAiModelKeys'

const MODALITY_FILTER = [
  { label: '全部', value: 'ALL' },
  { label: '文本模型', value: 'TEXT' },
  { label: '图片模型', value: 'IMAGE' },
  { label: '视频模型', value: 'VIDEO' },
  { label: '音频模型', value: 'AUDIO' },
]

const ABILITY_OPTIONS = [
  { value: 'TEXT', label: '文本模型 (TEXT)', modality: 'TEXT' },
  { value: 'IMAGE', label: '图片模型 (IMAGE)', modality: 'IMAGE' },
  { value: 'VIDEO', label: '视频模型 (VIDEO)', modality: 'VIDEO' },
  { value: 'VOICE', label: '音频模型 - 配音合成 (VOICE)', modality: 'AUDIO' },
  { value: 'ASR', label: '音频模型 - 字幕识别 (ASR)', modality: 'AUDIO' },
]

const ABILITY_LABEL = {
  TEXT: '文本模型',
  IMAGE: '图片模型',
  VIDEO: '视频模型',
  VOICE: '音频 · 配音',
  ASR: '音频 · 识别',
  MUSIC: '音频 · 音乐',
}

export default function UserAiModelKeysCard() {
  const [loading, setLoading] = useState(false)
  const [onlyUserKey, setOnlyUserKey] = useState(false)
  const [keys, setKeys] = useState([])
  const [presets, setPresets] = useState([])
  const [modalityFilter, setModalityFilter] = useState('ALL')
  const [modeBusy, setModeBusy] = useState(false)
  const [testingKeyId, setTestingKeyId] = useState(null)

  const [editOpen, setEditOpen] = useState(false)
  const [editingRow, setEditingRow] = useState(null)
  const [saving, setSaving] = useState(false)
  const [modalTesting, setModalTesting] = useState(false)
  const [editForm] = Form.useForm()

  const [grokOpen, setGrokOpen] = useState(false)
  const [grokSaving, setGrokSaving] = useState(false)
  const [grokTesting, setGrokTesting] = useState(false)
  const [grokForm] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await userAiModelKeysApi.list()
      if (data?.success && data.result) {
        setOnlyUserKey(Number(data.result.only_user_key) === 1)
        setKeys(Array.isArray(data.result.key_list) ? data.result.key_list : [])
        setPresets(Array.isArray(data.result.provider_preset_list) ? data.result.provider_preset_list : [])
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const xaiPreset = useMemo(
    () => presets.find((p) => p.provider_code === 'XAI') || presets[0] || {
      provider_code: 'XAI',
      provider_name: 'Grok (xAI)',
      default_base_url: 'https://api.x.ai/v1',
      abilities: [],
    },
    [presets],
  )

  const abilityMap = useMemo(() => {
    const map = {}
    ;(xaiPreset.abilities || []).forEach((a) => {
      map[a.ability_type] = a
    })
    return map
  }, [xaiPreset])

  const filteredKeys = useMemo(() => {
    if (modalityFilter === 'ALL') return keys
    return keys.filter((k) => k.modality_type === modalityFilter)
  }, [keys, modalityFilter])

  const selectedAbility = Form.useWatch('ability_type', editForm) || 'TEXT'
  const watchedModelCode = Form.useWatch('model_code', editForm)

  const currentModelPresets = useMemo(() => {
    const preset = abilityMap[selectedAbility]
    return Array.isArray(preset?.models) ? preset.models : []
  }, [abilityMap, selectedAbility])

  const onModeChange = async (checked) => {
    setModeBusy(true)
    try {
      const { data } = await userAiModelKeysApi.updateMode({ only_user_key: checked ? 1 : 0 })
      if (data?.success) {
        setOnlyUserKey(Number(data.result?.only_user_key) === 1)
        message.success(checked ? '已开启仅使用自有密钥模型' : '已切换为混合模型模式')
      }
    } finally {
      setModeBusy(false)
    }
  }

  const openAdd = () => {
    setEditingRow(null)
    const defaultAbility = modalityFilter === 'ALL' ? 'TEXT' : (modalityFilter === 'AUDIO' ? 'VOICE' : modalityFilter)
    const firstModel = abilityMap[defaultAbility]?.models?.[0]
    editForm.setFieldsValue({
      provider_code: xaiPreset.provider_code || 'XAI',
      ability_type: defaultAbility,
      model_code: firstModel?.model_code || 'grok-4.7',
      model_name: firstModel?.model_name || 'Grok 4.7',
      base_url: xaiPreset.default_base_url || 'https://api.x.ai/v1',
      api_key: '',
    })
    setEditOpen(true)
  }

  const openEdit = (row) => {
    setEditingRow(row)
    editForm.setFieldsValue({
      provider_code: row.provider_code || 'XAI',
      ability_type: row.ability_type || 'TEXT',
      model_code: row.model_code || '',
      model_name: row.model_name || '',
      base_url: row.base_url || 'https://api.x.ai/v1',
      api_key: '',
    })
    setEditOpen(true)
  }

  const onEditAbilityChange = (ability) => {
    if (editingRow) return
    const firstModel = abilityMap[ability]?.models?.[0]
    if (firstModel) {
      editForm.setFieldsValue({
        model_code: firstModel.model_code,
        model_name: firstModel.model_name,
      })
    }
  }

  const onSelectPresetModel = (code) => {
    const found = currentModelPresets.find((m) => m.model_code === code)
    editForm.setFieldsValue({
      model_code: code,
      model_name: found?.model_name || code,
    })
  }

  const onTestSingleInModal = async () => {
    const vals = editForm.getFieldsValue()
    setModalTesting(true)
    try {
      const payload = {
        key_id: editingRow?.key_id,
        provider_code: vals.provider_code || 'XAI',
        base_url: vals.base_url,
        api_key: vals.api_key,
        model_code: vals.model_code,
      }
      const { data } = await userAiModelKeysApi.test(payload)
      if (data?.success && data.result?.ok) {
        message.success(data.result.message || '连接成功')
      } else {
        message.warning(data?.result?.message || data?.message || '连接检测未通过')
      }
    } finally {
      setModalTesting(false)
    }
  }

  const onSaveEdit = async (values) => {
    const abilityMeta = ABILITY_OPTIONS.find((o) => o.value === values.ability_type)
    const modality = abilityMeta?.modality || 'TEXT'
    setSaving(true)
    try {
      if (editingRow) {
        const { data } = await userAiModelKeysApi.update({
          key_id: editingRow.key_id,
          provider_code: values.provider_code || 'XAI',
          modality_type: modality,
          ability_type: values.ability_type,
          model_code: values.model_code,
          model_name: values.model_name,
          base_url: values.base_url,
          api_key: values.api_key || undefined,
        })
        if (data?.success) {
          message.success('已保存')
          setEditOpen(false)
          await load()
        }
      } else {
        const { data } = await userAiModelKeysApi.create({
          provider_code: values.provider_code || 'XAI',
          modality_type: modality,
          ability_type: values.ability_type,
          model_code: values.model_code,
          model_name: values.model_name,
          base_url: values.base_url,
          api_key: values.api_key,
        })
        if (data?.success) {
          message.success('已添加并启用')
          setEditOpen(false)
          await load()
        }
      }
    } finally {
      setSaving(false)
    }
  }

  const openQuickGrok = () => {
    grokForm.setFieldsValue({
      api_key: '',
      base_url: xaiPreset.default_base_url || 'https://api.x.ai/v1',
      abilities: ['TEXT', 'IMAGE', 'VIDEO', 'VOICE', 'ASR'],
      text_model: abilityMap.TEXT?.models?.[0]?.model_code || 'grok-4.7',
      image_model: abilityMap.IMAGE?.models?.[0]?.model_code || 'grok-imagine-image-2.0',
      video_model: abilityMap.VIDEO?.models?.[0]?.model_code || 'grok-imagine-video-1.5',
      voice_model: abilityMap.VOICE?.models?.[0]?.model_code || 'grok-tts',
      asr_model: abilityMap.ASR?.models?.[0]?.model_code || 'grok-voice-transcribe-2.0',
      only_user_key: true,
    })
    setGrokOpen(true)
  }

  const onTestQuickGrok = async () => {
    const vals = grokForm.getFieldsValue()
    if (!vals.api_key || !vals.api_key.trim()) {
      message.warning('请输入 Grok API Key')
      return
    }
    setGrokTesting(true)
    try {
      const { data } = await userAiModelKeysApi.test({
        provider_code: 'XAI',
        base_url: vals.base_url,
        api_key: vals.api_key,
        model_code: vals.text_model,
      })
      if (data?.success && data.result?.ok) {
        message.success(data.result.message || '连接成功')
      } else {
        message.warning(data?.result?.message || data?.message || '连接检测未通过')
      }
    } finally {
      setGrokTesting(false)
    }
  }

  const onSaveQuickGrok = async (vals) => {
    const picked = Array.isArray(vals.abilities) ? vals.abilities : []
    if (!picked.length) {
      message.warning('请至少勾选一种模型能力')
      return
    }
    const modelByAbility = {
      TEXT: vals.text_model,
      IMAGE: vals.image_model,
      VIDEO: vals.video_model,
      VOICE: vals.voice_model,
      ASR: vals.asr_model,
    }
    const items = picked.map((ability) => {
      const code = modelByAbility[ability]
      const found = (abilityMap[ability]?.models || []).find((m) => m.model_code === code)
      const meta = ABILITY_OPTIONS.find((o) => o.value === ability)
      return {
        modality_type: meta?.modality || 'TEXT',
        ability_type: ability,
        model_code: code,
        model_name: found?.model_name || code,
      }
    })
    setGrokSaving(true)
    try {
      const { data } = await userAiModelKeysApi.create({
        provider_code: 'XAI',
        base_url: vals.base_url,
        api_key: vals.api_key,
        only_user_key: vals.only_user_key ? 1 : 0,
        items,
      })
      if (data?.success) {
        message.success(`已接入 ${data.result?.created_count || items.length} 项 Grok 模型配置`)
        setGrokOpen(false)
        await load()
      }
    } finally {
      setGrokSaving(false)
    }
  }

  const onTestRow = async (row) => {
    setTestingKeyId(row.key_id)
    try {
      const { data } = await userAiModelKeysApi.test({ key_id: row.key_id })
      if (data?.success && data.result?.ok) {
        message.success(data.result.message || '连接成功')
      } else {
        message.warning(data?.result?.message || data?.message || '连接检测未通过')
      }
      await load()
    } finally {
      setTestingKeyId(null)
    }
  }

  const onToggleRow = async (row, checked) => {
    const { data } = await userAiModelKeysApi.toggle({ key_id: row.key_id, enabled: checked ? 1 : 0 })
    if (data?.success) {
      message.success(checked ? '已启用' : '已停用')
      await load()
    }
  }

  const onSetDefaultRow = async (row) => {
    const { data } = await userAiModelKeysApi.setDefault({ key_id: row.key_id })
    if (data?.success) {
      message.success('已设为默认模型')
      await load()
    }
  }

  const onDeleteRow = async (row) => {
    const { data } = await userAiModelKeysApi.remove({ key_id: row.key_id })
    if (data?.success) {
      message.success('已删除')
      await load()
    }
  }

  return (
    <Card
      loading={loading}
      title="模型密钥"
      style={{ marginBottom: 16 }}
      extra={(
        <Space wrap>
          <Space size={6}>
            <span>仅使用自有密钥模型</span>
            <Switch checked={onlyUserKey} loading={modeBusy} onChange={onModeChange} />
          </Space>
          <Button onClick={openQuickGrok}>一键接入 Grok</Button>
          <Button type="primary" onClick={openAdd}>新增密钥</Button>
        </Space>
      )}
    >
      <div style={{ marginBottom: 12 }}>
        <Segmented
          options={MODALITY_FILTER}
          value={modalityFilter}
          onChange={setModalityFilter}
        />
      </div>

      <Table
        rowKey="key_id"
        size="small"
        pagination={false}
        dataSource={filteredKeys}
        locale={{ emptyText: '暂无模型密钥' }}
        columns={[
          {
            title: '类型',
            dataIndex: 'ability_type',
            width: 120,
            render: (v) => ABILITY_LABEL[v] || v,
          },
          { title: '供应商', dataIndex: 'provider_name', width: 110 },
          { title: '模型名称', dataIndex: 'model_name', width: 170 },
          { title: '模型编码', dataIndex: 'model_code', width: 180 },
          { title: '密钥', dataIndex: 'api_key_masked', width: 130 },
          {
            title: '状态',
            width: 80,
            render: (_, row) => (
              <Switch
                size="small"
                checked={Number(row.enabled) === 1}
                onChange={(checked) => onToggleRow(row, checked)}
              />
            ),
          },
          {
            title: '默认',
            width: 80,
            render: (_, row) => (Number(row.default_model) === 1 ? <Tag color="blue">默认</Tag> : '-'),
          },
          {
            title: '检测状态',
            dataIndex: 'last_check_status',
            width: 90,
            render: (v) => {
              if (v === 'OK') return <Tag color="success">OK</Tag>
              if (v === 'FAILED') return <Tag color="error">FAILED</Tag>
              return '-'
            },
          },
          {
            title: '操作',
            width: 230,
            render: (_, row) => (
              <Space size={4} wrap>
                <Button
                  size="small"
                  loading={testingKeyId === row.key_id}
                  onClick={() => onTestRow(row)}
                >
                  测试
                </Button>
                {Number(row.default_model) !== 1 && Number(row.enabled) === 1 ? (
                  <Button size="small" onClick={() => onSetDefaultRow(row)}>设为默认</Button>
                ) : null}
                <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
                <Popconfirm title="确认删除该模型密钥？" onConfirm={() => onDeleteRow(row)}>
                  <Button size="small" danger>删除</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        open={editOpen}
        title={editingRow ? '编辑模型密钥' : '新增模型密钥'}
        onCancel={() => setEditOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" onFinish={onSaveEdit}>
          <Form.Item name="provider_code" label="供应商" rules={[{ required: true }]}>
            <Select options={[{ value: 'XAI', label: 'Grok (xAI)' }]} />
          </Form.Item>
          <Form.Item name="ability_type" label="模型类型" rules={[{ required: true }]}>
            <Select options={ABILITY_OPTIONS} onChange={onEditAbilityChange} />
          </Form.Item>
          {currentModelPresets.length > 0 && (
            <Form.Item label="预设模型">
              <Select
                placeholder="选择预设模型"
                value={currentModelPresets.some((m) => m.model_code === watchedModelCode)
                  ? watchedModelCode : undefined}
                onChange={onSelectPresetModel}
                options={currentModelPresets.map((m) => ({
                  value: m.model_code,
                  label: `${m.model_name} (${m.model_code})`,
                }))}
              />
            </Form.Item>
          )}
          <Form.Item name="model_code" label="模型编码" rules={[{ required: true, message: '请输入模型编码' }]}>
            <Input placeholder="例如 grok-4.7" />
          </Form.Item>
          <Form.Item name="model_name" label="展示名称" rules={[{ required: true, message: '请输入展示名称' }]}>
            <Input placeholder="例如 Grok 4.7" />
          </Form.Item>
          <Form.Item name="base_url" label="接口地址" rules={[{ required: true, message: '请输入接口地址' }]}>
            <Input placeholder="https://api.x.ai/v1" />
          </Form.Item>
          <Form.Item
            name="api_key"
            label={editingRow ? 'API Key（留空则保留原密钥）' : 'API Key'}
            rules={editingRow ? [] : [{ required: true, message: '请输入 API Key' }]}
          >
            <Input.Password placeholder="xai-..." autoComplete="off" />
          </Form.Item>
          <Space>
            <Button loading={modalTesting} onClick={onTestSingleInModal}>测试连接</Button>
            <Button type="primary" htmlType="submit" loading={saving}>保存</Button>
          </Space>
        </Form>
      </Modal>

      <Modal
        open={grokOpen}
        title="一键接入 Grok (xAI)"
        onCancel={() => setGrokOpen(false)}
        footer={null}
        width={620}
        destroyOnClose
      >
        <Form form={grokForm} layout="vertical" onFinish={onSaveQuickGrok}>
          <Form.Item name="api_key" label="Grok API Key" rules={[{ required: true, message: '请输入 Grok API Key' }]}>
            <Input.Password placeholder="xai-..." autoComplete="off" />
          </Form.Item>
          <Form.Item name="base_url" label="接口地址" rules={[{ required: true, message: '请输入接口地址' }]}>
            <Input placeholder="https://api.x.ai/v1" />
          </Form.Item>
          <Form.Item name="abilities" label="启用能力" rules={[{ required: true, message: '请至少选择一项能力' }]}>
            <Checkbox.Group
              options={[
                { label: '文本模型', value: 'TEXT' },
                { label: '图片模型', value: 'IMAGE' },
                { label: '视频模型', value: 'VIDEO' },
                { label: '音频 · 配音', value: 'VOICE' },
                { label: '音频 · 识别', value: 'ASR' },
              ]}
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item name="text_model" label="文本模型">
              <Select
                options={(abilityMap.TEXT?.models || []).map((m) => ({ value: m.model_code, label: `${m.model_name} (${m.model_code})` }))}
              />
            </Form.Item>
            <Form.Item name="image_model" label="图片模型">
              <Select
                options={(abilityMap.IMAGE?.models || []).map((m) => ({ value: m.model_code, label: `${m.model_name} (${m.model_code})` }))}
              />
            </Form.Item>
            <Form.Item name="video_model" label="视频模型">
              <Select
                options={(abilityMap.VIDEO?.models || []).map((m) => ({ value: m.model_code, label: `${m.model_name} (${m.model_code})` }))}
              />
            </Form.Item>
            <Form.Item name="voice_model" label="音频配音模型">
              <Select
                options={(abilityMap.VOICE?.models || []).map((m) => ({ value: m.model_code, label: `${m.model_name} (${m.model_code})` }))}
              />
            </Form.Item>
            <Form.Item name="asr_model" label="音频识别模型">
              <Select
                options={(abilityMap.ASR?.models || []).map((m) => ({ value: m.model_code, label: `${m.model_name} (${m.model_code})` }))}
              />
            </Form.Item>
          </div>
          <Form.Item name="only_user_key" valuePropName="checked">
            <Checkbox>仅使用自有密钥模型（停用默认 BGSSAI 模型）</Checkbox>
          </Form.Item>
          <Space>
            <Button loading={grokTesting} onClick={onTestQuickGrok}>测试连接</Button>
            <Button type="primary" htmlType="submit" loading={grokSaving}>保存并启用</Button>
          </Space>
        </Form>
      </Modal>
    </Card>
  )
}
