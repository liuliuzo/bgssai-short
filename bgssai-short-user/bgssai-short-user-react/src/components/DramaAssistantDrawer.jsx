import React, { useState } from 'react'
import { Drawer, Radio, Input, Button, Space, Tag, Typography, Divider, message, Spin, Alert, Modal } from 'antd'
import productionApi from '../api/production'

const { Paragraph, Text } = Typography
const { TextArea } = Input

const AGENT_ROLES = [
  {
    key: 'SCRIPT',
    name: '编剧专家',
    desc: '对白张力、反转与集尾钩子；可确认后写回镜头对白',
    tag: '爽点与对白',
    color: 'magenta',
  },
  {
    key: 'DIRECTOR',
    name: '分镜导演',
    desc: '景别节奏、机位与镜头衔接；可确认后写回镜头描述',
    tag: '分镜语言',
    color: 'blue',
  },
  {
    key: 'VISUAL',
    name: '视觉专家',
    desc: '光影构图与提示词参数；可确认后写回额外提示',
    tag: '光影构图',
    color: 'geekblue',
  },
  {
    key: 'EDITOR',
    name: '剪辑顾问',
    desc: '时长卡点、音效与导出建议；默认不直接改镜头字段',
    tag: '剪辑与声音',
    color: 'cyan',
  },
]

const QUICK_ACTIONS = {
  SCRIPT: [
    { label: '增加台词火药味与反转', prompt: '请优化当前对白，让矛盾更尖锐，结尾带一句反转回击。' },
    { label: '设计集尾留存钩子', prompt: '请为当前集设计一个抓人的卡点悬念。' },
    { label: '精炼对白（去废话）', prompt: '请精简当前对话，只保留核心戏剧动机。' },
  ],
  DIRECTOR: [
    { label: '拆解为三镜头递进组合', prompt: '请将这段戏拆成全景入场、中景对话、特写爆发的分镜组合。' },
    { label: '优化相邻镜头首尾衔接', prompt: '请检查相邻镜头走位与视线，避免跳轴。' },
    { label: '给出四机位构图建议', prompt: '请给出正拍、反拍、特写、俯拍四个构图建议。' },
  ],
  VISUAL: [
    { label: '强化电影感光影', prompt: '请为当前镜头补充电影感光影与质感关键词。' },
    { label: '转为悬疑冷调', prompt: '请把当前画面基调改成悬疑冷调。' },
    { label: '转为暖金奢华基调', prompt: '请把当前画面基调改成暖金奢华。' },
  ],
  EDITOR: [
    { label: '建议BGM与音效点', prompt: '请推荐背景音乐风格与关键重音打点位置。' },
    { label: '剪辑节奏卡点建议', prompt: '请建议镜头保留时长与切点。' },
  ],
}

function applyLabel(target) {
  if (target === 'dialogue_text') return '确认写回对白'
  if (target === 'shot_desc') return '确认写回镜头描述'
  if (target === 'extra_prompt') return '确认写回额外提示'
  return null
}

export default function DramaAssistantDrawer({
  visible,
  onClose,
  workId,
  episodeId,
  currentShot,
  onApplied,
}) {
  const [activeRole, setActiveRole] = useState('DIRECTOR')
  const [inputVal, setInputVal] = useState('')
  const [sending, setSending] = useState(false)
  const [messages, setMessages] = useState([
    {
      sender: 'assistant',
      role: 'DIRECTOR',
      text: '请选择编剧、分镜导演、视觉或剪辑专家角色后提问。所有写回分镜字段的操作均需您二次确认后才会生效。',
    },
  ])

  const currentRoleObj = AGENT_ROLES.find((r) => r.key === activeRole)

  const handleSend = async () => {
    if (!inputVal.trim() || sending) return
    if (!workId) {
      message.error('缺少作品上下文，无法请求专家建议')
      return
    }
    const question = inputVal.trim()
    const userMsg = { sender: 'user', text: question }
    setMessages((prev) => [...prev, userMsg])
    setInputVal('')
    setSending(true)
    try {
      const { data } = await productionApi.dramaAssistAdvise({
        work_id: workId,
        episode_id: episodeId || undefined,
        shot_id: currentShot?.shot_id || undefined,
        role: activeRole,
        question,
      })
      if (!data?.success) {
        const errText = data?.message || '专家建议失败，请稍后重试'
        setMessages((prev) => [...prev, { sender: 'assistant', role: activeRole, text: errText, error: true }])
        message.error(errText)
        return
      }
      const result = data.result || {}
      const actions = Array.isArray(result.action_list) ? result.action_list : []
      const actionBlock = actions.length ? `\n可执行动作：\n- ${actions.join('\n- ')}` : ''
      const replyText = `【${result.role_name || currentRoleObj?.name || activeRole}】${result.summary ? `\n${result.summary}\n` : '\n'}${result.advice_text || ''}${actionBlock}`
      setMessages((prev) => [...prev, {
        sender: 'assistant',
        role: activeRole,
        text: replyText,
        apply_target: result.apply_target,
        apply_text: result.apply_text,
        context_used: result.context_used,
      }])
    } catch (err) {
      const errText = err?.response?.data?.message || err?.message || '模型暂时不可用，请稍后重试'
      setMessages((prev) => [...prev, { sender: 'assistant', role: activeRole, text: errText, error: true }])
      message.error(errText)
    } finally {
      setSending(false)
    }
  }

  const handleQuickAction = (qa) => {
    setInputVal(qa.prompt)
  }

  const resolveShotId = async () => {
    if (currentShot?.shot_id) return { shot_id: currentShot.shot_id, shot_no: currentShot.shot_no }
    if (!episodeId) return null
    const { data } = await productionApi.listShots({ episode_id: episodeId, page_num: 1, page_size: 20 })
    const list = Array.isArray(data?.result) ? data.result : (data?.result?.shot_list || [])
    const first = list[0]
    if (!first?.shot_id) return null
    return { shot_id: first.shot_id, shot_no: first.shot_no }
  }

  const handleConfirmApply = async (msg) => {
    if (!msg?.apply_text || !msg?.apply_target || msg.apply_target === 'none') return
    let targetShot
    try {
      targetShot = await resolveShotId()
    } catch (err) {
      message.error(err?.response?.data?.message || '无法读取镜头列表')
      return
    }
    if (!targetShot?.shot_id) {
      message.warning('当前剧集没有可写回的镜头')
      return
    }
    Modal.confirm({
      title: `确认写回镜头 ${targetShot.shot_no || ''}？`,
      content: currentShot?.shot_id
        ? '只会更新你确认的字段，不会自动生成画面或视频。'
        : '未显式选中镜头，将写回本集第一个镜头；只会更新确认字段。',
      okText: '确认写回',
      cancelText: '取消',
      onOk: async () => {
        const { data } = await productionApi.dramaAssistApply({
          work_id: workId,
          shot_id: targetShot.shot_id,
          apply_target: msg.apply_target,
          apply_text: msg.apply_text,
        })
        if (!data?.success) {
          message.error(data?.message || '写回失败')
          throw new Error('apply failed')
        }
        message.success('已写回镜头')
        if (onApplied) onApplied()
      },
    })
  }

  return (
    <Drawer
      title="AI 短剧多专家协同工作台"
      placement="right"
      width={460}
      open={visible}
      onClose={onClose}
      extra={<Tag color={currentRoleObj?.color}>{currentRoleObj?.tag}</Tag>}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="结合剧集与分镜上下文协同创作"
        description={
          currentShot?.shot_id
            ? `已关联当前作品与第 ${currentShot.shot_no || ''} 镜；采纳建议写回字段前需经您二次确认。`
            : '已关联当前作品；未选中具体分镜时仅提供创作建议，不会直接修改分镜。'
        }
      />

      <div style={{ marginBottom: 16 }}>
        <Text strong>切换专家协同角色：</Text>
        <Radio.Group
          value={activeRole}
          onChange={(e) => setActiveRole(e.target.value)}
          buttonStyle="solid"
          size="small"
          style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 6 }}
        >
          {AGENT_ROLES.map((role) => (
            <Radio.Button key={role.key} value={role.key}>
              {role.name}
            </Radio.Button>
          ))}
        </Radio.Group>
        <Paragraph type="secondary" style={{ marginTop: 6, fontSize: 12 }}>
          {currentRoleObj?.desc}
        </Paragraph>
      </div>

      <Divider style={{ margin: '12px 0' }} />

      <div style={{ marginBottom: 12 }}>
        <Text strong style={{ fontSize: 12 }}>常用快捷指令：</Text>
        <Space wrap style={{ marginTop: 6 }}>
          {(QUICK_ACTIONS[activeRole] || []).map((qa, idx) => (
            <Button key={idx} size="small" type="dashed" onClick={() => handleQuickAction(qa)}>
              {qa.label}
            </Button>
          ))}
        </Space>
      </div>

      <div
        style={{
          height: 320,
          overflowY: 'auto',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          padding: 12,
          backgroundColor: '#fafafa',
          marginBottom: 16,
        }}
      >
        {messages.map((m, i) => (
          <div
            key={i}
            style={{
              marginBottom: 12,
              textAlign: m.sender === 'user' ? 'right' : 'left',
            }}
          >
            <div
              style={{
                display: 'inline-block',
                maxWidth: '88%',
                padding: '8px 12px',
                borderRadius: 8,
                backgroundColor: m.sender === 'user' ? '#1677ff' : '#ffffff',
                color: m.error ? '#a8071a' : (m.sender === 'user' ? '#ffffff' : '#333333'),
                boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                textAlign: 'left',
                whiteSpace: 'pre-wrap',
                fontSize: 13,
                lineHeight: 1.5,
              }}
            >
              {m.text}
              {m.sender === 'assistant' && applyLabel(m.apply_target) && m.apply_text ? (
                <div style={{ marginTop: 6, borderTop: '1px dashed #e8e8e8', paddingTop: 4 }}>
                  <Button
                    size="small"
                    type="link"
                    style={{ padding: 0, fontSize: 12 }}
                    onClick={() => handleConfirmApply(m)}
                  >
                    {applyLabel(m.apply_target)}
                  </Button>
                </div>
              ) : null}
            </div>
          </div>
        ))}
        {sending ? <div style={{ textAlign: 'center', padding: 8 }}><Spin size="small" /></div> : null}
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <TextArea
          rows={2}
          value={inputVal}
          onChange={(e) => setInputVal(e.target.value)}
          placeholder={`向${currentRoleObj?.name || '专家'}提问`}
          disabled={sending}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
        />
        <Button type="primary" style={{ height: 'auto' }} loading={sending} onClick={handleSend}>
          发送
        </Button>
      </div>
    </Drawer>
  )
}
