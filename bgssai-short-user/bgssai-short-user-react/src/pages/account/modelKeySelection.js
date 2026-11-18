export const ABILITY_LABEL = {
  TEXT: '文本', IMAGE: '图片', VIDEO: '视频', VOICE: '配音合成', ASR: '字幕识别', MUSIC: '背景音乐',
}
export function modelKeyMask(row) {
  return [row.api_key_masked, row.secret_key_masked].filter(Boolean).join(' / ') || '-'
}

export function modelOptions(rows) {
  const groups = new Map()
  for (const row of rows) {
    const id = JSON.stringify([Number(row.platform_model) === 1, row.provider_code, row.ability_type, row.model_code])
    const current = groups.get(id)
    const enabled = Number(row.enabled) === 1
    if (!current || (enabled && (Number(current.enabled) !== 1 || Number(row.default_model) === 1))) {
      groups.set(id, row)
    }
  }
  return [...groups.values()]
}

export function modelSelectionFields(ability, code, name) {
  return {
    modality_type: ['VOICE', 'ASR', 'MUSIC'].includes(ability) ? 'AUDIO' : ability,
    ability_type: ability,
    model_code: code.trim(),
    model_name: name.trim(),
  }
}

export function modelsForAbility(options, ability) {
  return ability === 'ALL' ? options : options.filter((model) => model.ability_type === ability)
}

export function legacyCredentialAbilities(credentials, models) {
  const currentProviders = new Set(credentials.filter((key) => Number(key.active) === 1).map((key) => key.provider_code))
  const usage = new Map()
  for (const model of models) {
    if (!model.credential_id || !model.ability_type || Number(model.enabled) !== 1 ||
        Number(model.platform_model) === 1 || currentProviders.has(model.provider_code)) continue
    if (!usage.has(model.credential_id)) usage.set(model.credential_id, new Set())
    usage.get(model.credential_id).add(model.ability_type)
  }
  return usage
}
