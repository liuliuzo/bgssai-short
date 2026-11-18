import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, message } from 'antd'
import api from '../../api/userAiModelKeys'
import CredentialManagementCard from './CredentialManagementCard'
import ModelSelectionCard from './ModelSelectionCard'
import { FALLBACK_PRESETS } from './modelProviderPresets'

export default function UserAiModelKeysCard() {
  const [snapshot, setSnapshot] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const pending = useRef(false)
  const sequence = useRef(0)
  const load = useCallback(async () => {
    const request = ++sequence.current
    setLoading(true)
    try {
      const { data } = await api.list()
      if (sequence.current !== request) return
      const result = data?.result
      setSnapshot(data?.success && Array.isArray(result?.key_list) && Array.isArray(result?.credential_list) ? result : null)
    } catch {
      if (sequence.current === request) setSnapshot(null)
    } finally {
      if (sequence.current === request) setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    return () => { sequence.current++ }
  }, [load])

  async function run(action, request, success, refresh = true) {
    if (pending.current) return false
    pending.current = true
    setBusy(action)
    try {
      const { data } = await request()
      if (!data?.success) return false
      if (typeof success === 'function') success(data.result)
      else message.success(success)
      if (refresh) await load()
      return true
    } catch {
      // The HTTP client reports request failures. Dialogs stay open for retry.
      return false
    } finally {
      pending.current = false
      setBusy('')
    }
  }

  const common = {
    credentials: snapshot?.credential_list || [],
    rows: snapshot?.key_list || [],
    presets: snapshot?.provider_preset_list?.length ? snapshot.provider_preset_list : FALLBACK_PRESETS,
    loading, busy, disabled: !snapshot || loading || !!busy, run,
  }
  return (
    <>
      {!snapshot && !loading && <Alert type="error" showIcon message="无法加载账户模型配置"
        action={<Button onClick={load}>重新加载</Button>} style={{ marginBottom: 16 }} />}
      <CredentialManagementCard {...common} />
      <ModelSelectionCard {...common} />
    </>
  )
}
