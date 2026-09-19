import { useCallback, useEffect, useState } from 'react'
import { Button, Input, Select, Space, Typography, message } from 'antd'
import supportApi from '../../api/support'
import './support-inbox.css'

const { Title, Text, Paragraph } = Typography
const CODE_OK = new Set([0, 200, '0', '200'])

export default function SupportInboxPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [status, setStatus] = useState()
  const [keyword, setKeyword] = useState('')
  const [pageNum, setPageNum] = useState(1)
  const [pageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [activeId, setActiveId] = useState('')
  const [session, setSession] = useState(null)
  const [messages, setMessages] = useState([])
  const [replyText, setReplyText] = useState('')
  const [busy, setBusy] = useState(false)

  const loadList = useCallback(async (num = 1) => {
    setLoading(true)
    try {
      const { data } = await supportApi.list({
        page_num: num,
        page_size: pageSize,
        status: status || undefined,
        keyword: keyword.trim() || undefined,
      })
      if (!CODE_OK.has(data?.code)) {
        message.error(data?.message || '加载失败')
        return
      }
      setRows(Array.isArray(data?.result) ? data.result : [])
      setTotal(Number(data?.total_size) || 0)
      setPageNum(Number(data?.page_num) || num)
    } finally {
      setLoading(false)
    }
  }, [keyword, pageSize, status])

  const loadDetail = useCallback(async (sessionId) => {
    if (!sessionId) return
    setBusy(true)
    try {
      const { data } = await supportApi.detail({ session_id: sessionId })
      if (!CODE_OK.has(data?.code)) {
        message.error(data?.message || '加载详情失败')
        return
      }
      setActiveId(sessionId)
      setSession(data.result?.session || null)
      setMessages(Array.isArray(data.result?.messages) ? data.result.messages : [])
      await loadList(pageNum)
    } finally {
      setBusy(false)
    }
  }, [loadList, pageNum])

  useEffect(() => {
    loadList(1)
  }, [loadList])

  const handleReply = async () => {
    const text = replyText.trim()
    if (!text || !activeId) return
    setBusy(true)
    try {
      const { data } = await supportApi.reply({ session_id: activeId, content: text })
      if (!CODE_OK.has(data?.code)) {
        message.error(data?.message || '回复失败')
        return
      }
      setReplyText('')
      await loadDetail(activeId)
      message.success('已回复')
    } finally {
      setBusy(false)
    }
  }

  const handleClose = async () => {
    if (!activeId) return
    setBusy(true)
    try {
      const { data } = await supportApi.close({ session_id: activeId })
      if (!CODE_OK.has(data?.code)) {
        message.error(data?.message || '关闭失败')
        return
      }
      message.success('会话已关闭')
      await loadDetail(activeId)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>在线客服</Title>
      <Paragraph type="secondary">
        处理用户留言会话：列表、详情、回复、关闭。对应 POST /bgssai/admin/support/**
      </Paragraph>

      <Space wrap style={{ marginBottom: 12 }}>
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 140 }}
          value={status}
          onChange={setStatus}
          options={[
            { value: 'PENDING', label: 'PENDING' },
            { value: 'OPEN', label: 'OPEN' },
            { value: 'CLOSED', label: 'CLOSED' },
          ]}
        />
        <Input
          placeholder="搜索联系人 / 会话 ID"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ width: 220 }}
          allowClear
        />
        <Button type="primary" loading={loading} onClick={() => loadList(1)}>查询</Button>
        <Button disabled={pageNum <= 1} onClick={() => loadList(pageNum - 1)}>上一页</Button>
        <Text type="secondary">{pageNum} / {Math.max(1, Math.ceil(total / pageSize))}</Text>
        <Button disabled={pageNum * pageSize >= total} onClick={() => loadList(pageNum + 1)}>下一页</Button>
      </Space>

      <div className="si-grid">
        <div className="si-list">
          {rows.length === 0 && <div className="si-empty">{loading ? '加载中…' : '暂无会话'}</div>}
          {rows.map((row) => (
            <button
              key={row.session_id}
              type="button"
              className={`si-item${activeId === row.session_id ? ' active' : ''}`}
              onClick={() => loadDetail(row.session_id)}
            >
              <div className="si-item-title">
                <strong>{row.contact_name || row.user_id || '访客'}</strong>
                <span className="si-badge">{row.status}</span>
                {Number(row.unread_admin) > 0 && <span className="si-unread">{row.unread_admin}</span>}
              </div>
              <div className="si-item-meta">
                {row.contact_phone || row.contact_email || row.session_id}
              </div>
              <div className="si-item-meta">{row.last_message_at || row.create_time}</div>
            </button>
          ))}
        </div>

        <div className="si-detail">
          {!session ? (
            <div className="si-empty">选择左侧会话查看详情</div>
          ) : (
            <>
              <div className="si-detail-header">
                <div>
                  <strong>{session.session_id}</strong>
                  <div className="si-item-meta">
                    {[session.contact_name, session.contact_phone, session.contact_email]
                      .filter(Boolean)
                      .join(' / ') || '无联系方式'}
                    {session.user_id ? ` · user_id=${session.user_id}` : ''}
                  </div>
                </div>
                <Button
                  danger
                  disabled={busy || session.status === 'CLOSED'}
                  onClick={handleClose}
                >
                  关闭会话
                </Button>
              </div>
              <div className="si-msgs">
                {messages.map((m) => (
                  <div
                    key={m.message_id}
                    className={`si-bubble ${m.sender_type === 'ADMIN' ? 'admin' : 'user'}`}
                  >
                    {m.content}
                    <span className="si-meta">{m.sender_type} · {m.create_time}</span>
                  </div>
                ))}
              </div>
              <div className="si-reply">
                <Input
                  placeholder={session.status === 'CLOSED' ? '会话已关闭' : '回复用户…'}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  disabled={busy || session.status === 'CLOSED'}
                  onPressEnter={handleReply}
                />
                <Button
                  type="primary"
                  loading={busy}
                  disabled={!replyText.trim() || session.status === 'CLOSED'}
                  onClick={handleReply}
                >
                  回复
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
