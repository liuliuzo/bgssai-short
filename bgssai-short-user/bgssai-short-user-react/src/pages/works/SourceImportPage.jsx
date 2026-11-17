import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, Card, Checkbox, Input, InputNumber, Select, Space, Table, Tag, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import productionApi from '../../api/production'
import { modelPricingCompactLabel, modelPricingLabel } from '../../modelPricing'
import { canMergeChapterWithNext, mergeChapterWithNext, updateChapterTitle } from './sourceChapters'

const { Paragraph, Text } = Typography
const { TextArea } = Input

const TYPE_LABEL = {
  NOVEL: '小说全文',
  SCRIPT: '成品剧本',
  OUTLINE: '分集大纲',
}

export default function SourceImportPage({ work }) {
  const [sources, setSources] = useState([])
  const [activeSourceId, setActiveSourceId] = useState()
  const [chapters, setChapters] = useState([])
  const [sourceType, setSourceType] = useState('NOVEL')
  const [sourceTitle, setSourceTitle] = useState('')
  const [sourceContent, setSourceContent] = useState('')
  const [rightsConfirm, setRightsConfirm] = useState(false)
  const [chapterFrom, setChapterFrom] = useState(1)
  const [chapterTo, setChapterTo] = useState(1)
  const [targetCount, setTargetCount] = useState(6)
  const [estimate, setEstimate] = useState(null)
  const [adapting, setAdapting] = useState(false)
  const [loading, setLoading] = useState(false)
  const [textModels, setTextModels] = useState([])
  const [textModelCode, setTextModelCode] = useState()
  const [fileLabel, setFileLabel] = useState('暂未选择本地文件')
  const fileRef = useRef(null)

  const workId = work?.work_id

  const loadSources = useCallback(async () => {
    if (!workId) return
    const { data } = await featuresApi.sourceList(workId)
    const list = data?.result || []
    setSources(list)
    if (!activeSourceId && list.length) {
      setActiveSourceId(list[0].source_id)
    }
  }, [workId, activeSourceId])

  const loadChapters = useCallback(async () => {
    if (!activeSourceId) {
      setChapters([])
      return
    }
    setLoading(true)
    try {
      const { data } = await featuresApi.sourceDetail(activeSourceId, 1, 200)
      const list = data?.result || []
      setChapters(list)
      if (list.length) {
        setChapterFrom(list[0].chapter_no)
        setChapterTo(list[list.length - 1].chapter_no)
      }
    } finally {
      setLoading(false)
    }
  }, [activeSourceId])

  useEffect(() => { loadSources() }, [loadSources])
  useEffect(() => { loadChapters() }, [loadChapters])
  useEffect(() => {
    let active = true
    productionApi.listAiModels('TEXT').then(({ data }) => {
      if (!active) return
      const list = data?.result?.model_list || []
      setTextModels(list)
      setTextModelCode((cur) => cur || list.find((m) => m.default_flag === 1)?.model_code || list[0]?.model_code)
    }).catch(() => {})
    return () => { active = false }
  }, [])
  // An estimate only prices the range it was computed for; any change voids it.
  useEffect(() => { setEstimate(null) }, [activeSourceId, chapterFrom, chapterTo, targetCount, textModelCode])

  const doImport = async () => {
    if (!rightsConfirm) {
      message.warning('请先勾选权利确认')
      return
    }
    if (!sourceContent.trim()) {
      message.warning('请粘贴原文，或选择一个 txt / md 文件')
      return
    }
    const { data } = await featuresApi.sourceImport({
      work_id: workId,
      source_type: sourceType,
      source_title: sourceTitle.trim() || work?.work_title,
      source_content: sourceContent,
      rights_confirm_flag: 1,
    })
    if (data?.success) {
      message.success(`已导入，切出 ${data.result.chapter_count} 章`)
      setSourceContent('')
      setRightsConfirm(false)
      setActiveSourceId(data.result.source_id)
      await loadSources()
    }
  }

  // 浏览器端读文件转文本再提交，后端不必为一段文字多维护一条 multipart 通道
  const pickFile = (event) => {
    const file = event.target.files?.[0]
    if (!file) {
      setFileLabel('暂未选择本地文件')
      return
    }
    setFileLabel(file.name)
    const reader = new FileReader()
    reader.onload = () => {
      setSourceContent(String(reader.result || ''))
      if (!sourceTitle) setSourceTitle(file.name.replace(/\.(txt|md)$/i, ''))
    }
    reader.readAsText(file, 'UTF-8')
    event.target.value = ''
  }

  const saveChapters = async () => {
    const { data } = await featuresApi.sourceChaptersSave({
      source_id: activeSourceId,
      chapter_list: chapters.map((c, index) => ({
        chapter_no: index + 1,
        chapter_title: c.chapter_title,
        chapter_text: c.chapter_text,
      })),
    })
    if (data?.success) {
      message.success('分章已保存')
      await loadChapters()
      await loadSources()
    }
  }

  const runEstimate = async () => {
    const { data } = await featuresApi.sourceAdaptEstimate({
      source_id: activeSourceId,
      chapter_no_from: chapterFrom,
      chapter_no_to: chapterTo,
      target_episode_count: targetCount,
      model_code: textModelCode,
    })
    if (data?.success) setEstimate(data.result)
  }

  const runAdapt = async () => {
    if (adapting) return
    setAdapting(true)
    try {
      const { data } = await featuresApi.sourceAdapt({
        source_id: activeSourceId,
        chapter_no_from: chapterFrom,
        chapter_no_to: chapterTo,
        target_episode_count: targetCount,
        model_code: textModelCode,
      })
      if (data?.success) {
        message.success('改编任务已提交')
        setEstimate(null)
      }
    } finally {
      setAdapting(false)
    }
  }

  return (
    <div>
      <Card title="导入原文" style={{ marginBottom: 12 }}>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            value={sourceType}
            onChange={setSourceType}
            style={{ width: 200 }}
            options={[
              { value: 'NOVEL', label: '小说全文' },
              { value: 'SCRIPT', label: '成品剧本' },
              { value: 'OUTLINE', label: '分集大纲' },
            ]}
          />
          <Input
            style={{ width: 280 }}
            placeholder="原著名（留空用作品名）"
            value={sourceTitle}
            onChange={(e) => setSourceTitle(e.target.value)}
          />
          <Button onClick={() => fileRef.current?.click()}>选择 txt 或 md 文件</Button>
          <Text>{fileLabel}</Text>
          <input ref={fileRef} type="file" accept=".txt,.md" hidden onChange={pickFile} />
        </Space>
        <TextArea
          rows={8}
          value={sourceContent}
          onChange={(e) => setSourceContent(e.target.value)}
          placeholder="粘贴原文或选择 txt / md 文件"
          style={{ marginBottom: 8 }}
        />
        <Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 8 }}>
          已输入 {sourceContent.length} / 1,000,000 字符
        </Paragraph>
        <div style={{ marginBottom: 12 }}>
          <Checkbox checked={rightsConfirm} onChange={(e) => setRightsConfirm(e.target.checked)}>
            我确认对本内容拥有合法使用权或已获授权
          </Checkbox>
        </div>
        <Button type="primary" onClick={doImport}>导入并自动分章</Button>
      </Card>

      <Card
        title="已导入的源"
        size="small"
        style={{ marginBottom: 12 }}
        extra={<Button size="small" onClick={loadSources}>刷新</Button>}
      >
        <Table
          rowKey="source_id"
          size="small"
          pagination={false}
          dataSource={sources}
          rowClassName={(r) => (r.source_id === activeSourceId ? 'ant-table-row-selected' : '')}
          columns={[
            { title: '标题', dataIndex: 'source_title' },
            { title: '类型', width: 110, render: (_, r) => TYPE_LABEL[r.source_type] || r.source_type },
            { title: '字数', dataIndex: 'char_count', width: 100 },
            { title: '章节', dataIndex: 'chapter_count', width: 80 },
            {
              title: '状态',
              width: 100,
              render: (_, r) => (r.adapted_flag === 1 ? <Tag color="success">已改编</Tag> : <Tag>未改编</Tag>),
            },
            {
              title: '操作',
              width: 150,
              render: (_, r) => (
                <Space size={4}>
                  <Button type="link" size="small" onClick={() => setActiveSourceId(r.source_id)}>查看</Button>
                  <Button
                    type="link"
                    size="small"
                    danger
                    onClick={async () => {
                      await featuresApi.sourceDelete(r.source_id)
                      message.success('已删除')
                      if (activeSourceId === r.source_id) setActiveSourceId(undefined)
                      await loadSources()
                    }}
                  >删除</Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      {activeSourceId ? (
        <>
          <Card
            title="分章结果"
            size="small"
            style={{ marginBottom: 12 }}
            extra={<Button type="primary" size="small" onClick={saveChapters}>保存分章</Button>}
          >
            <Table
              rowKey="chapter_id"
              size="small"
              loading={loading}
              dataSource={chapters}
              pagination={{ pageSize: 20 }}
              columns={[
                { title: '章号', dataIndex: 'chapter_no', width: 70 },
                {
                  title: '章节标题',
                  width: 240,
                  render: (_, r) => (
                    <Input
                      size="small"
                      value={r.chapter_title}
                      onChange={(e) => setChapters((cur) => updateChapterTitle(cur, r, e.target.value))}
                    />
                  ),
                },
                {
                  title: '开头',
                  render: (_, r) => (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {(r.chapter_text || '').slice(0, 40)}
                    </Text>
                  ),
                },
                { title: '字数', dataIndex: 'char_count', width: 80 },
                {
                  title: '操作',
                  width: 110,
                  render: (_, r) => (
                    <Button
                      type="link"
                      size="small"
                      disabled={!canMergeChapterWithNext(chapters, r)}
                      onClick={() => setChapters((cur) => mergeChapterWithNext(cur, r))}
                    >与下章合并</Button>
                  ),
                },
              ]}
            />
          </Card>

          <Card title="改编成分集大纲" size="small">
            <Space wrap style={{ marginBottom: 12 }}>
              <span>起始章</span>
              <InputNumber min={1} value={chapterFrom} onChange={setChapterFrom} />
              <span>结束章</span>
              <InputNumber min={1} value={chapterTo} onChange={setChapterTo} />
              <span>改成几集</span>
              <InputNumber min={1} max={200} value={targetCount} onChange={setTargetCount} />
              <Select
                style={{ minWidth: 180 }}
                placeholder="文本模型"
                value={textModelCode}
                onChange={setTextModelCode}
                options={textModels.map((m) => ({
                  value: m.model_code,
                  label: modelPricingCompactLabel(m),
                  title: modelPricingLabel(m),
                }))}
              />
              <Button onClick={runEstimate}>算一下费用</Button>
            </Space>
            {estimate ? (
              <Alert
                type="warning"
                style={{ marginBottom: 12 }}
                message={`涉及 ${estimate.char_count} 字（${estimate.chapter_count} 章），改编 ${targetCount} 集，预计 ${(estimate.estimated_cost_cent / 100).toFixed(2)} 元`}
              />
            ) : null}
            <Button type="primary" disabled={!estimate || adapting} loading={adapting} onClick={runAdapt}>确认改编</Button>
          </Card>
        </>
      ) : null}
    </div>
  )
}
