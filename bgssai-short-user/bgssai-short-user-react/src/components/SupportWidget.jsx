import { useEffect, useRef, useState } from 'react'
import { message } from 'antd'
import supportApi, { getGuestToken, setGuestToken } from '../api/support'
import { useUserStore } from '../store/user'
import './support-widget.css'

const CODE_OK = new Set([0, 200, '0', '200'])

function isMine(senderType) {
  return senderType === 'USER' || senderType === 'GUEST'
}

export default function SupportWidget() {
  const userInfo = useUserStore((s) => s.userInfo)
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [sessionId, setSessionId] = useState('')
  const [status, setStatus] = useState('')
  const [messages, setMessages] = useState([])
  const [contactName, setContactName] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [draft, setDraft] = useState('')
  const listRef = useRef(null)

  useEffect(() => {
    if (!open || !listRef.current) return
    listRef.current.scrollTop = listRef.current.scrollHeight
  }, [open, messages])

  const ensureSession = async (firstContent) => {
    if (sessionId && status !== 'CLOSED') {
      return sessionId
    }
    const body = {
      guest_token: getGuestToken() || undefined,
      contact_name: contactName || undefined,
      contact_phone: contactPhone || undefined,
      contact_email: contactEmail || undefined,
      content: firstContent || undefined,
    }
    const { data } = await supportApi.createSession(body)
    if (!CODE_OK.has(data?.code)) {
      throw new Error(data?.message || '创建会话失败')
    }
    const result = data.result || {}
    if (result.guest_token) setGuestToken(result.guest_token)
    setSessionId(result.session_id || '')
    setStatus(result.status || '')
    if (result.message) {
      setMessages([result.message])
    } else {
      setMessages([])
    }
    return result.session_id
  }

  const loadDetail = async (sid) => {
    const { data } = await supportApi.detail({
      session_id: sid,
      guest_token: getGuestToken() || undefined,
    })
    if (!CODE_OK.has(data?.code)) {
      throw new Error(data?.message || '加载会话失败')
    }
    const result = data.result || {}
    setStatus(result.session?.status || '')
    setMessages(Array.isArray(result.messages) ? result.messages : [])
    if (result.session?.contact_name) setContactName(result.session.contact_name)
    if (result.session?.contact_phone) setContactPhone(result.session.contact_phone)
    if (result.session?.contact_email) setContactEmail(result.session.contact_email)
  }

  const bootstrap = async () => {
    setBusy(true)
    try {
      const guest = getGuestToken()
      if (!guest && !userInfo?.jwt_token) {
        setSessionId('')
        setStatus('')
        setMessages([])
        return
      }
      const { data } = await supportApi.listMine({
        guest_token: guest || undefined,
        page_num: 1,
        page_size: 1,
      })
      if (CODE_OK.has(data?.code) && Array.isArray(data.result) && data.result.length > 0) {
        const latest = data.result[0]
        if (latest.status !== 'CLOSED') {
          setSessionId(latest.session_id)
          setStatus(latest.status)
          await loadDetail(latest.session_id)
          return
        }
      }
      setSessionId('')
      setStatus('')
      setMessages([])
    } catch {
      /* ignore until first send */
    } finally {
      setBusy(false)
    }
  }

  const handleOpen = () => {
    const next = !open
    setOpen(next)
    if (next) bootstrap()
  }

  const handleSend = async () => {
    const text = draft.trim()
    if (!text || busy) return
    setBusy(true)
    try {
      let sid = sessionId
      if (!sid || status === 'CLOSED') {
        sid = await ensureSession(text)
        setDraft('')
        if (sid) await loadDetail(sid)
        return
      }
      const { data } = await supportApi.send({
        session_id: sid,
        guest_token: getGuestToken() || undefined,
        content: text,
      })
      if (!CODE_OK.has(data?.code)) {
        throw new Error(data?.message || '发送失败')
      }
      setDraft('')
      await loadDetail(sid)
    } catch (e) {
      message.error(e.message || '发送失败')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <button type="button" className="cs-fab" onClick={handleOpen}>
        在线客服
      </button>
      {open && (
        <div className="cs-panel" role="dialog" aria-label="在线客服">
          <div className="cs-panel-header">
            <strong>在线客服{status ? ` · ${status}` : ''}</strong>
            <button type="button" aria-label="关闭" onClick={() => setOpen(false)}>
              ×
            </button>
          </div>
          {(!sessionId || status === 'CLOSED') && (
            <div className="cs-contact">
              <input
                placeholder="联系人姓名（建议填写）"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
              />
              <input
                placeholder="手机（建议填写）"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
              />
              <input
                placeholder="邮箱（建议填写）"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
              />
            </div>
          )}
          {status === 'CLOSED' && (
            <div className="cs-hint">会话已关闭，发送新留言将自动开启新会话。</div>
          )}
          <div className="cs-msgs" ref={listRef}>
            {messages.length === 0 ? (
              <div className="cs-empty">有问题尽管留言，我们会尽快回复。</div>
            ) : (
              messages.map((m) => (
                <div
                  key={m.message_id}
                  className={`cs-bubble ${isMine(m.sender_type) ? 'me' : 'them'}`}
                >
                  {m.content}
                  <span className="cs-meta">{m.create_time || ''}</span>
                </div>
              ))
            )}
          </div>
          <div className="cs-compose">
            <input
              placeholder="输入留言…"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              disabled={busy}
            />
            <button type="button" onClick={handleSend} disabled={busy || !draft.trim()}>
              发送
            </button>
          </div>
        </div>
      )}
    </>
  )
}
