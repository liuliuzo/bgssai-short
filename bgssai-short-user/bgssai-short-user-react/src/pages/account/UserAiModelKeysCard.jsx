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
  { value: 'MUSIC', label: '音频模型 - 背景音乐 (MUSIC)', modality: 'AUDIO' },
]

const ABILITY_LABEL = {
  TEXT: '文本模型',
  IMAGE: '图片模型',
  VIDEO: '视频模型',
  VOICE: '音频 · 配音',
  ASR: '音频 · 识别',
  MUSIC: '音频 · 音乐',
}

const QUICK_BUTTONS = [
  { code: 'MINIMAX', label: '接入 MiniMax' },
  { code: 'KLING', label: '接入可灵' },
  { code: 'PIXVERSE', label: '接入 PixVerse' },
  { code: 'DEEPSEEK', label: '接入 DeepSeek' },
  { code: 'XAI', label: '接入 Grok' },
]

const FALLBACK_PRESETS = [
  {
    provider_code: 'MINIMAX',
    provider_name: 'MiniMax (海螺)',
    auth_type: 'BEARER',
    default_base_url: 'https://api.minimaxi.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'MiniMax-M2.5', model_name: 'MiniMax M2.5', recommended: true },
          { model_code: 'MiniMax-M2.5-highspeed', model_name: 'MiniMax M2.5 Highspeed', recommended: false },
          { model_code: 'abab6.5s-chat', model_name: 'MiniMax abab6.5s', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'image-01', model_name: 'MiniMax Image-01', recommended: true },
          { model_code: 'image-01-live', model_name: 'MiniMax Image-01 Live', recommended: false },
        ],
      },
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'MiniMax-Hailuo-02', model_name: 'MiniMax Hailuo 02', recommended: true },
          { model_code: 'MiniMax-Hailuo-2.3', model_name: 'MiniMax Hailuo 2.3', recommended: false },
          { model_code: 'T2V-01-Director', model_name: 'MiniMax T2V-01 Director', recommended: false },
          { model_code: 'I2V-01-Director', model_name: 'MiniMax I2V-01 Director', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'speech-2.8-hd', model_name: 'MiniMax Speech 2.8 HD', recommended: true },
          { model_code: 'speech-2.8-turbo', model_name: 'MiniMax Speech 2.8 Turbo', recommended: false },
          { model_code: 'speech-02-hd', model_name: 'MiniMax Speech 02 HD', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'asr-1.0', model_name: 'MiniMax ASR 1.0', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'MUSIC',
        ability_label: '音频模型（背景音乐）',
        models: [
          { model_code: 'music-2.5', model_name: 'MiniMax Music 2.5', recommended: true },
          { model_code: 'music-2.0', model_name: 'MiniMax Music 2.0', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'KLING',
    provider_name: '可灵 (Kling AI)',
    auth_type: 'AK_SK',
    default_base_url: 'https://api-beijing.klingai.com',
    abilities: [
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'kling-v3', model_name: 'Kling V3', recommended: true },
          { model_code: 'kling-v2-6', model_name: 'Kling V2.6', recommended: false },
          { model_code: 'kling-v2-5-turbo', model_name: 'Kling V2.5 Turbo', recommended: false },
          { model_code: 'kling-v2-1-master', model_name: 'Kling V2.1 Master', recommended: false },
          { model_code: 'kling-v1-6', model_name: 'Kling V1.6', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'kling-v3', model_name: 'Kling Image V3', recommended: true },
          { model_code: 'kling-v2-1', model_name: 'Kling Image V2.1', recommended: false },
          { model_code: 'kling-v1-5', model_name: 'Kling Image V1.5', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'kling-tts', model_name: 'Kling TTS', recommended: true },
        ],
      },
    ],
  },
  {
    provider_code: 'PIXVERSE',
    provider_name: 'PixVerse',
    auth_type: 'API_KEY_HEADER',
    default_base_url: 'https://app-api.pixverse.ai',
    abilities: [
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'v6', model_name: 'PixVerse v6', recommended: true },
          { model_code: 'v6-max', model_name: 'PixVerse v6 Max', recommended: false },
          { model_code: 'v5.6', model_name: 'PixVerse v5.6', recommended: false },
          { model_code: 'v5', model_name: 'PixVerse v5', recommended: false },
          { model_code: 'v4.5', model_name: 'PixVerse v4.5', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'DEEPSEEK',
    provider_name: 'DeepSeek',
    auth_type: 'BEARER',
    default_base_url: 'https://api.deepseek.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'deepseek-chat', model_name: 'DeepSeek Chat', recommended: true },
          { model_code: 'deepseek-reasoner', model_name: 'DeepSeek Reasoner', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'XAI',
    provider_name: 'Grok (xAI)',
    auth_type: 'BEARER',
    default_base_url: 'https://api.x.ai/v1',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'grok-4.7', model_name: 'Grok 4.7', recommended: true },
          { model_code: 'grok-4', model_name: 'Grok 4', recommended: false },
          { model_code: 'grok-3', model_name: 'Grok 3', recommended: false },
          { model_code: 'grok-3-mini', model_name: 'Grok 3 Mini', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'grok-imagine-image-2.0', model_name: 'Grok Imagine Image 2.0', recommended: true },
          { model_code: 'grok-2-image', model_name: 'Grok 2 Image', recommended: false },
        ],
      },
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'grok-imagine-video-1.5', model_name: 'Grok Imagine Video 1.5', recommended: true },
          { model_code: 'grok-imagine-video-1.5-lite', model_name: 'Grok Imagine Video 1.5 Lite', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'grok-tts', model_name: 'Grok TTS', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'grok-voice-transcribe-2.0', model_name: 'Grok Voice Transcribe 2.0', recommended: true },
        ],
      },
    ],
  },
]

function buildQuickValues(preset, currentFormValues = {}) {
  const modelsByAbility = {}
  const abilities = []
  ;(preset?.abilities || []).forEach((ab) => {
    abilities.push(ab.ability_type)
    const first = ab.models?.[0]?.model_code
    if (first) {
      modelsByAbility[ab.ability_type] = first
    }
  })
  return {
    provider_code: preset?.provider_code || 'MINIMAX',
    api_key: currentFormValues.api_key || '',
    secret_key: currentFormValues.secret_key || '',
    base_url: preset?.default_base_url || '',
    abilities,
    models_by_ability: modelsByAbility,
    only_user_key: currentFormValues.only_user_key ?? true,
  }
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

  const [quickOpen, setQuickOpen] = useState(false)
  const [quickSaving, setQuickSaving] = useState(false)
  const [quickTesting, setQuickTesting] = useState(false)
  const [quickForm] = Form.useForm()

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

  const effectivePresets = useMemo(
    () => (presets.length ? presets : FALLBACK_PRESETS),
    [presets],
  )

  const presetMap = useMemo(() => {
    const map = {}
    effectivePresets.forEach((p) => {
      map[p.provider_code] = p
    })
    return map
  }, [effectivePresets])

  const filteredKeys = useMemo(() => {
    if (modalityFilter === 'ALL') return keys
    return keys.filter((k) => k.modality_type === modalityFilter)
  }, [keys, modalityFilter])

  const watchedProviderCode = Form.useWatch('provider_code', editForm) || 'MINIMAX'
  const selectedAbility = Form.useWatch('ability_type', editForm) || 'TEXT'
  const watchedModelCode = Form.useWatch('model_code', editForm)
  const watchedQuickProvider = Form.useWatch('provider_code', quickForm) || 'MINIMAX'
  const watchedQuickAbilities = Form.useWatch('abilities', quickForm) || []

  const editPreset = presetMap[watchedProviderCode] || effectivePresets[0]
  const quickPreset = presetMap[watchedQuickProvider] || effectivePresets[0]

  const editAbilityMap = useMemo(() => {
    const map = {}
    ;(editPreset?.abilities || []).forEach((a) => {
      map[a.ability_type] = a
    })
    return map
  }, [editPreset])

  const currentModelPresets = useMemo(() => {
    const preset = editAbilityMap[selectedAbility]
    return Array.isArray(preset?.models) ? preset.models : []
  }, [editAbilityMap, selectedAbility])

  const providerSelectOptions = useMemo(
    () => effectivePresets.map((p) => ({ value: p.provider_code, label: p.provider_name })),
    [effectivePresets],
  )

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
    const defaultProvider = effectivePresets[0]
    const defaultAbilityCandidate = modalityFilter === 'ALL' ? 'TEXT' : (modalityFilter === 'AUDIO' ? 'VOICE' : modalityFilter)
    const matchingAbility = (defaultProvider?.abilities || []).find((a) => a.ability_type === defaultAbilityCandidate)
      || defaultProvider?.abilities?.[0]
    const firstModel = matchingAbility?.models?.[0]
    editForm.setFieldsValue({
      provider_code: defaultProvider?.provider_code || 'MINIMAX',
      ability_type: matchingAbility?.ability_type || defaultAbilityCandidate,
      model_code: firstModel?.model_code || 'MiniMax-M2.5',
      model_name: firstModel?.model_name || 'MiniMax M2.5',
      base_url: defaultProvider?.default_base_url || 'https://api.minimaxi.com',
      api_key: '',
      secret_key: '',
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
      base_url: row.base_url || presetMap[row.provider_code]?.default_base_url || '',
      api_key: '',
      secret_key: '',
    })
    setEditOpen(true)
  }

  const onEditProviderChange = (providerCode) => {
    const preset = presetMap[providerCode]
    if (!preset || editingRow) return
    const curAbility = editForm.getFieldValue('ability_type')
    const matched = (preset.abilities || []).find((a) => a.ability_type === curAbility) || preset.abilities?.[0]
    const firstModel = matched?.models?.[0]
    editForm.setFieldsValue({
      base_url: preset.default_base_url || '',
      ability_type: matched?.ability_type || curAbility,
      model_code: firstModel?.model_code || '',
      model_name: firstModel?.model_name || '',
      secret_key: '',
    })
  }

  const onEditAbilityChange = (ability) => {
    if (editingRow) return
    const firstModel = editAbilityMap[ability]?.models?.[0]
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
        secret_key: vals.secret_key,
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
          provider_code: values.provider_code,
          modality_type: modality,
          ability_type: values.ability_type,
          model_code: values.model_code,
          model_name: values.model_name,
          base_url: values.base_url,
          api_key: values.api_key || undefined,
          secret_key: values.secret_key || undefined,
        })
        if (data?.success) {
          message.success('已保存')
          setEditOpen(false)
          await load()
        }
      } else {
        const { data } = await userAiModelKeysApi.create({
          provider_code: values.provider_code,
          modality_type: modality,
          ability_type: values.ability_type,
          model_code: values.model_code,
          model_name: values.model_name,
          base_url: values.base_url,
          api_key: values.api_key,
          secret_key: values.secret_key || undefined,
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

  const openQuickProvider = (providerCode) => {
    const preset = presetMap[providerCode] || effectivePresets[0]
    quickForm.setFieldsValue(buildQuickValues(preset))
    setQuickOpen(true)
  }

  const onQuickProviderChange = (providerCode) => {
    const preset = presetMap[providerCode]
    if (!preset) return
    quickForm.setFieldsValue(buildQuickValues(preset, quickForm.getFieldsValue()))
  }

  const onTestQuick = async () => {
    const vals = quickForm.getFieldsValue()
    if (!vals.api_key || !vals.api_key.trim()) {
      message.warning(quickPreset?.auth_type === 'AK_SK' ? '请输入 Access Key' : '请输入 API Key')
      return
    }
    if (quickPreset?.auth_type === 'AK_SK' && (!vals.secret_key || !vals.secret_key.trim())) {
      message.warning('请输入 Secret Key')
      return
    }
    const firstAbility = vals.abilities?.[0] || quickPreset?.abilities?.[0]?.ability_type
    const firstModelCode = (firstAbility && vals.models_by_ability?.[firstAbility]) || ''
    setQuickTesting(true)
    try {
      const { data } = await userAiModelKeysApi.test({
        provider_code: vals.provider_code,
        base_url: vals.base_url,
        api_key: vals.api_key,
        secret_key: vals.secret_key,
        model_code: firstModelCode,
      })
      if (data?.success && data.result?.ok) {
        message.success(data.result.message || '连接成功')
      } else {
        message.warning(data?.result?.message || data?.message || '连接检测未通过')
      }
    } finally {
      setQuickTesting(false)
    }
  }

  const onSaveQuick = async (vals) => {
    const picked = Array.isArray(vals.abilities) ? vals.abilities : []
    if (!picked.length) {
      message.warning('请至少勾选一种模型能力')
      return
    }
    const abilityLookup = {}
    ;(quickPreset?.abilities || []).forEach((a) => {
      abilityLookup[a.ability_type] = a
    })
    const items = picked.map((ability) => {
      const code = vals.models_by_ability?.[ability]
      const found = (abilityLookup[ability]?.models || []).find((m) => m.model_code === code)
      const meta = ABILITY_OPTIONS.find((o) => o.value === ability)
      return {
        modality_type: meta?.modality || 'TEXT',
        ability_type: ability,
        model_code: code,
        model_name: found?.model_name || code,
      }
    })
    setQuickSaving(true)
    try {
      const { data } = await userAiModelKeysApi.create({
        provider_code: vals.provider_code,
        base_url: vals.base_url,
        api_key: vals.api_key,
        secret_key: vals.secret_key || undefined,
        only_user_key: vals.only_user_key ? 1 : 0,
        items,
      })
      if (data?.success) {
        message.success(`已接入 ${data.result?.created_count || items.length} 项 ${quickPreset?.provider_name || ''} 模型配置`)
        setQuickOpen(false)
        await load()
      }
    } finally {
      setQuickSaving(false)
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

  const isEditKling = watchedProviderCode === 'KLING' || editPreset?.auth_type === 'AK_SK'
  const isQuickKling = watchedQuickProvider === 'KLING' || quickPreset?.auth_type === 'AK_SK'

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
          {QUICK_BUTTONS.map((btn) => (
            <Button key={btn.code} onClick={() => openQuickProvider(btn.code)}>{btn.label}</Button>
          ))}
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
            width: 110,
            render: (v) => ABILITY_LABEL[v] || v,
          },
          { title: '供应商', dataIndex: 'provider_name', width: 130 },
          { title: '模型名称', dataIndex: 'model_name', width: 170 },
          { title: '模型编码', dataIndex: 'model_code', width: 170 },
          {
            title: '密钥',
            dataIndex: 'api_key_masked',
            width: 190,
            render: (_, row) => (row.secret_key_masked
              ? `${row.api_key_masked || ''} / ${row.secret_key_masked}`
              : (row.api_key_masked || '-')),
          },
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
            width: 70,
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
            <Select options={providerSelectOptions} onChange={onEditProviderChange} />
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
            <Input placeholder="例如 MiniMax-M2.5 / kling-v3 / v6 / deepseek-chat" />
          </Form.Item>
          <Form.Item name="model_name" label="展示名称" rules={[{ required: true, message: '请输入展示名称' }]}>
            <Input placeholder="例如 MiniMax M2.5" />
          </Form.Item>
          <Form.Item name="base_url" label="接口地址" rules={[{ required: true, message: '请输入接口地址' }]}>
            <Input placeholder={editPreset?.default_base_url || 'https://...'} />
          </Form.Item>
          <Form.Item
            name="api_key"
            label={isEditKling
              ? (editingRow ? 'Access Key（留空则保留原密钥）' : 'Access Key')
              : (editingRow ? 'API Key（留空则保留原密钥）' : 'API Key')}
            rules={editingRow ? [] : [{ required: true, message: isEditKling ? '请输入 Access Key' : '请输入 API Key' }]}
          >
            <Input.Password placeholder={isEditKling ? '可灵 Access Key' : '请输入 API Key'} autoComplete="off" />
          </Form.Item>
          {isEditKling && (
            <Form.Item
              name="secret_key"
              label={editingRow ? 'Secret Key（留空则保留原密钥）' : 'Secret Key'}
              rules={editingRow ? [] : [{ required: true, message: '请输入 Secret Key' }]}
            >
              <Input.Password placeholder="可灵 Secret Key" autoComplete="off" />
            </Form.Item>
          )}
          <Space>
            <Button loading={modalTesting} onClick={onTestSingleInModal}>测试连接</Button>
            <Button type="primary" htmlType="submit" loading={saving}>保存</Button>
          </Space>
        </Form>
      </Modal>

      <Modal
        open={quickOpen}
        title={`一键接入 ${quickPreset?.provider_name || ''}`}
        onCancel={() => setQuickOpen(false)}
        footer={null}
        width={640}
        destroyOnClose
      >
        <Form form={quickForm} layout="vertical" onFinish={onSaveQuick}>
          <Form.Item name="provider_code" label="供应商" rules={[{ required: true }]}>
            <Select options={providerSelectOptions} onChange={onQuickProviderChange} />
          </Form.Item>
          <Form.Item
            name="api_key"
            label={isQuickKling ? 'Access Key' : `${quickPreset?.provider_name || ''} API Key`}
            rules={[{ required: true, message: isQuickKling ? '请输入 Access Key' : '请输入 API Key' }]}
          >
            <Input.Password placeholder={isQuickKling ? '可灵 Access Key' : '请输入 API Key'} autoComplete="off" />
          </Form.Item>
          {isQuickKling && (
            <Form.Item
              name="secret_key"
              label="Secret Key"
              rules={[{ required: true, message: '请输入 Secret Key' }]}
            >
              <Input.Password placeholder="可灵 Secret Key" autoComplete="off" />
            </Form.Item>
          )}
          <Form.Item name="base_url" label="接口地址" rules={[{ required: true, message: '请输入接口地址' }]}>
            <Input placeholder={quickPreset?.default_base_url || 'https://...'} />
          </Form.Item>
          <Form.Item name="abilities" label="启用能力" rules={[{ required: true, message: '请至少选择一项能力' }]}>
            <Checkbox.Group
              options={(quickPreset?.abilities || []).map((ab) => ({
                label: ABILITY_LABEL[ab.ability_type] || ab.ability_label || ab.ability_type,
                value: ab.ability_type,
              }))}
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {(quickPreset?.abilities || []).map((ab) => (
              <Form.Item
                key={ab.ability_type}
                name={['models_by_ability', ab.ability_type]}
                label={ab.ability_label || ABILITY_LABEL[ab.ability_type]}
              >
                <Select
                  disabled={!watchedQuickAbilities.includes(ab.ability_type)}
                  options={(ab.models || []).map((m) => ({
                    value: m.model_code,
                    label: `${m.model_name} (${m.model_code})`,
                  }))}
                />
              </Form.Item>
            ))}
          </div>
          <Form.Item name="only_user_key" valuePropName="checked">
            <Checkbox>仅使用自有密钥模型（停用默认 BGSSAI 模型）</Checkbox>
          </Form.Item>
          <Space>
            <Button loading={quickTesting} onClick={onTestQuick}>测试连接</Button>
            <Button type="primary" htmlType="submit" loading={quickSaving}>保存并启用</Button>
          </Space>
        </Form>
      </Modal>
    </Card>
  )
}
