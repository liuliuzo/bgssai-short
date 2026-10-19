import React, { useState } from 'react'
import { Drawer, Radio, Input, Button, Space, Tag, Typography, Card, Divider, message } from 'antd'
import { buildEnhancedPrompt, DRAMA_STYLES, CAMERA_MOVEMENTS, SHOT_TYPES } from '../utils/CinematographyHelper'

const { Paragraph, Text, Title } = Typography
const { TextArea } = Input

const AGENT_ROLES = [
  {
    key: 'SCRIPT',
    name: '编剧专家 (Script Doctor)',
    desc: '专注短剧快节奏、矛盾冲突、情绪爆发点与反转对白设计',
    tag: '爽点与对白',
    color: 'magenta',
  },
  {
    key: 'DIRECTOR',
    name: '分镜导演 (Storyboard Director)',
    desc: '规划镜头组接、景别节奏过渡（全景->特写）与首尾帧衔接',
    tag: '分镜语言',
    color: 'blue',
  },
  {
    key: 'VISUAL',
    name: '视觉专家 (Visual Artist)',
    desc: '强化光影氛围、构图质感、色彩基调与提示词参数调优',
    tag: '8K光影构图',
    color: 'purple',
  },
  {
    key: 'EDITOR',
    name: '剪辑顾问 (Editor & Sound)',
    desc: '建议背景音乐进出点、音效重音、节奏卡点与工程导出规范',
    tag: '剪辑与声音',
    color: 'cyan',
  },
]

const QUICK_ACTIONS = {
  SCRIPT: [
    { label: '增加台词火药味与反转', prompt: '请帮我优化这段台词，让双方矛盾更加尖锐，结尾带有一句出人意料的反转回击。' },
    { label: '设计集尾留存钩子 (Cliffhanger)', prompt: '请帮我为当前集设计一个抓人的卡点悬念，让观众有强烈的欲望立刻点开下一集。' },
    { label: '精炼对白（去废话）', prompt: '请精简当前对话，去除不必要的客套废话，只保留核心戏剧动机。' },
  ],
  DIRECTOR: [
    { label: '拆解为三镜头递进组合', prompt: '请将这段戏拆解为【全景入场 -> 中景对话 -> 特写爆发】的经典分镜组合。' },
    { label: '优化相邻镜头首尾衔接', prompt: '请检查当前两个镜头的人物走位与视线方向，确保镜头切接时不发生跳轴。' },
    { label: '转换为四宫格机位预览', prompt: '请给出这一关键情节的4个不同视角的构图建议（正拍、反拍、特写、俯拍）。' },
  ],
  VISUAL: [
    { label: '一键注入电影感光影关键词', prompt: 'cinematic lighting, dramatic volumetric light, 8k uhd, photorealistic, sharp focus' },
    { label: '转换为悬疑冷调光影', prompt: 'moody dramatic shadows, cool blue undertones, neon reflections, cinematic film grain' },
    { label: '转换为豪门奢华暖金基调', prompt: 'high-end penthouse interior, luxury gold and marble accents, warm rim light, elegant bokeh' },
  ],
  EDITOR: [
    { label: '建议BGM情绪与音效点', prompt: '请为这组镜头推荐背景音乐风格（如激昂交响、低沉重音鼓点）及关键重音打点位置。' },
    { label: '剪辑节奏卡点建议', prompt: '建议每个镜头的保留时长，以及在对话句尾还是动作高潮处切镜头。' },
  ],
}

export default function DramaAssistantDrawer({ visible, onClose, currentShot, onApplyPrompt, onApplyDialogue }) {
  const [activeRole, setActiveRole] = useState('DIRECTOR')
  const [inputVal, setInputVal] = useState('')
  const [messages, setMessages] = useState([
    {
      sender: 'assistant',
      role: 'DIRECTOR',
      text: '你好！我是AI戏剧多专家协同助理。你可以选择上方不同的专家角色（编剧/分镜/视觉/剪辑），我将为你提供专业建议或一键优化提示词与台词。',
    },
  ])

  const handleSend = () => {
    if (!inputVal.trim()) return
    const userMsg = { sender: 'user', text: inputVal }
    let replyText = ''

    if (activeRole === 'SCRIPT') {
      replyText = `【编剧专家建议】针对你的需求：「${inputVal}」\n建议优化对白：\n“你以为你赢了？睁大眼睛看清楚，这盘棋从一开始就是我布的局！”\n（戏剧动机：反客为主，在第3秒制造听觉冲击，强化观众留存率。）`
    } else if (activeRole === 'DIRECTOR') {
      replyText = `【分镜导演建议】针对构思：「${inputVal}」\n推荐分镜机位配置：\n1. 镜1：过肩中景（OTS），交代对峙关系与空间位置；\n2. 镜2：缓慢推镜头至眼部特写（Push In），捕捉情绪波动；\n3. 镜3：仰拍反打（Low Angle），凸显气势反超。`
    } else if (activeRole === 'VISUAL') {
      replyText = `【视觉专家建议】推荐融合专业渲染关键词：\n${buildEnhancedPrompt(inputVal, { styleKey: 'URBAN_REVENGE', qualityBoost: true })}`
    } else {
      replyText = `【剪辑顾问建议】针对剪辑节奏：「${inputVal}」\n建议将镜头时长压缩至2.5秒以内，重音点卡在人物转身的瞬间；输出工程时选择导出剪映草稿或FCPXML，以便继续微调音效轨道。`
    }

    setMessages((prev) => [...prev, userMsg, { sender: 'assistant', role: activeRole, text: replyText }])
    setInputVal('')
  }

  const handleQuickAction = (qa) => {
    setInputVal(qa.prompt)
  }

  const handleApplyToPrompt = (text) => {
    if (onApplyPrompt) {
      onApplyPrompt(text)
      message.success('已应用至当前镜头视觉描述')
    }
  }

  const currentRoleObj = AGENT_ROLES.find((r) => r.key === activeRole)

  return (
    <Drawer
      title="AI戏剧多专家协同工作台 (Multi-Agent Drama Copilot)"
      placement="right"
      width={460}
      open={visible}
      onClose={onClose}
      extra={
        <Tag color={currentRoleObj?.color}>{currentRoleObj?.tag}</Tag>
      }
    >
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
              {role.name.split(' ')[0]}
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
            <Button
              key={idx}
              size="small"
              type="dashed"
              onClick={() => handleQuickAction(qa)}
            >
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
                color: m.sender === 'user' ? '#ffffff' : '#333333',
                boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                textAlign: 'left',
                whiteSpace: 'pre-wrap',
                fontSize: 13,
                lineHeight: 1.5,
              }}
            >
              {m.text}
              {m.sender === 'assistant' && onApplyPrompt && (
                <div style={{ marginTop: 6, borderTop: '1px dashed #e8e8e8', paddingTop: 4 }}>
                  <Button
                    size="small"
                    type="link"
                    style={{ padding: 0, fontSize: 12 }}
                    onClick={() => handleApplyToPrompt(m.text)}
                  >
                    应用到提示词
                  </Button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <TextArea
          rows={2}
          value={inputVal}
          onChange={(e) => setInputVal(e.target.value)}
          placeholder={`向${currentRoleObj?.name.split(' ')[0]}提问，如：“优化这段分镜对白与机位”`}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
        />
        <Button type="primary" style={{ height: 'auto' }} onClick={handleSend}>
          发送
        </Button>
      </div>
    </Drawer>
  )
}
