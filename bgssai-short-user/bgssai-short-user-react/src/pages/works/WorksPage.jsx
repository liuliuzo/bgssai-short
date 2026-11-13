import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Collapse, Form, Input, InputNumber, Modal, Radio, Select, Space, Table, Typography, message } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import productionApi from '../../api/production'
import { CREATE_WORK_DEFAULTS, createWorkSettingLine } from './workCreateDefaults'

const { Title, Paragraph } = Typography

// 取值对齐原型 docs/demo-static/web/user/work-create.html 与后端 ShortLabels / PromptComposer。
const GENRE_OPTIONS = [
  { value: 'REVENGE', label: '复仇逆袭' },
  { value: 'CEO', label: '豪门总裁' },
  { value: 'TIME_TRAVEL', label: '穿越重生' },
  { value: 'SWEET', label: '甜宠' },
  { value: 'SUSPENSE', label: '悬疑' },
  { value: 'FANTASY', label: '玄幻' },
]

const STYLE_OPTIONS = [
  { value: 'MODERN_URBAN', label: '都市写实' },
  { value: 'ANCIENT_CHINESE', label: '古装' },
  { value: 'ANIME', label: '动漫' },
  { value: 'CINEMATIC', label: '电影感' },
]

// 单集时长只允许 60 / 90 / 120，后端同样校验（超出范围不予创建）。
const DURATION_OPTIONS = [60, 90, 120]

const DEFAULTS = CREATE_WORK_DEFAULTS

function CreateWorkSettingLine() {
  const genre = Form.useWatch('genre')
  const visualStyle = Form.useWatch('visual_style')
  const episodeCount = Form.useWatch('episode_count')
  const episodeSeconds = Form.useWatch('episode_duration_sec')
  return (
    <Paragraph style={{ color: '#334155' }}>
      {createWorkSettingLine({
        genre: genre || DEFAULTS.genre,
        visual_style: visualStyle || DEFAULTS.visual_style,
        episode_count: episodeCount || DEFAULTS.episode_count,
        episode_duration_sec: episodeSeconds || DEFAULTS.episode_duration_sec,
      })}
    </Paragraph>
  )
}

export default function WorksPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async (num, size) => {
    setLoading(true)
    try {
      const { data } = await productionApi.listWorks({ page_num: num, page_size: size })
      setRows(Array.isArray(data?.result) ? data.result : [])
      setTotal(Number(data?.total_size) || 0)
      setPageNum(Number(data?.page_num) || num)
      setPageSize(Number(data?.page_size) || size)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(1, 20)
  }, [load])

  useEffect(() => {
    if (searchParams.get('create') !== '1') return
    setOpen(true)
    const next = new URLSearchParams(searchParams)
    next.delete('create')
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  const create = async () => {
    const values = await form.validateFields()
    const { data } = await productionApi.createWork({ ...DEFAULTS, ...values })
    if (data?.success) {
      message.success('已创建，正在生成大纲')
      setOpen(false)
      form.resetFields()
      navigate(`/works/${data.result.work_id}/script`, {
        state: { outlineTaskId: data.result.task_id },
      })
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>我的作品</Title>
      <Paragraph style={{ color: '#334155' }}>
        新建后进入作品，按顶部步骤做：大纲、这一集剧本、定稿、定妆、拆镜、首帧、生成视频。配音和质量检查不挡住第一条视频。只有剧名和一句话梗概是必填，其余按当前的试做设置。
      </Paragraph>
      <Card extra={<Button type="primary" onClick={() => setOpen(true)}>新建作品</Button>}>
        <Table
          rowKey="work_id"
          size="small"
          loading={loading}
          locale={{ emptyText: loading ? ' ' : '还没有作品，点击右上角「新建作品」开始' }}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size),
          }}
          tableLayout="fixed"
          columns={[
            {
              title: '标题',
              dataIndex: 'work_title',
              width: '28%',
              ellipsis: false,
              render: (value) => <span style={{ overflowWrap: 'anywhere' }}>{value}</span>,
            },
            { title: '类型', dataIndex: 'genre_name', width: '14%' },
            { title: '状态', dataIndex: 'work_status_name', width: '12%', render: (value) => <span style={{ whiteSpace: 'nowrap' }}>{value}</span> },
            { title: '集数', dataIndex: 'episode_count', width: '8%' },
            {
              title: '已花费(元)',
              dataIndex: 'cost_spent_cent',
              width: '14%',
              render: (v) => ((Number(v) || 0) / 100).toFixed(2),
            },
            {
              title: '操作',
              width: '24%',
              render: (_, row) => {
                const archived = row.work_status === 'ARCHIVED'
                return (
                  <Space>
                    <Button type="link" size="small" onClick={() => navigate(`/works/${row.work_id}/script`)}>进入制作</Button>
                    <Button type="link" size="small" onClick={async () => {
                      await productionApi.archiveWork(row.work_id, archived ? 0 : 1)
                      load(pageNum, pageSize)
                    }}>{archived ? '恢复' : '归档'}</Button>
                  </Space>
                )
              },
            },
          ]}
        />
      </Card>
      <Modal
        title="新建作品"
        open={open}
        onOk={create}
        onCancel={() => setOpen(false)}
        destroyOnClose
        width={620}
        okText="创建并生成大纲"
      >
        <Form form={form} layout="vertical" initialValues={DEFAULTS}>
          <Form.Item name="work_title" label="作品标题" rules={[{ required: true, message: '请输入作品标题' }]}>
            <Input placeholder="例如：女帝归来" />
          </Form.Item>
          <Form.Item
            name="one_line_pitch"
            label="一句话梗概"
            rules={[{ required: true, message: '请输入一句话梗概' }]}
            extra="灰字只是例子，不是已经填好的内容。提交后立刻按这句话生成全剧分集大纲。"
          >
            <Input.TextArea rows={3} placeholder="例如：落魄千金三年后带着资本回来" />
          </Form.Item>
          <CreateWorkSettingLine />
          <Collapse
            ghost
            items={[{
              key: 'advanced',
              label: '高级设置（不打开也会按上面这句话创建）',
              children: (
                <>
                  <Space size="large" style={{ display: 'flex' }}>
                    <Form.Item name="genre" label="题材" style={{ flex: 1 }}>
                      <Select options={GENRE_OPTIONS} />
                    </Form.Item>
                    <Form.Item name="visual_style" label="画风" style={{ flex: 1 }}>
                      <Select options={STYLE_OPTIONS} />
                    </Form.Item>
                  </Space>
                  <Space size="large" style={{ display: 'flex' }}>
                    <Form.Item
                      name="episode_count"
                      label="集数"
                      style={{ flex: 1 }}
                      rules={[{ type: 'number', min: 1, max: 80, message: '集数需在 1-80 之间' }]}
                      extra="创建后生成等量剧集占位"
                    >
                      <InputNumber min={1} max={80} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item
                      name="episode_duration_sec"
                      label="单集时长"
                      style={{ flex: 1 }}
                      extra="时长决定节奏，剧本体检按此校验"
                    >
                      <Radio.Group
                        optionType="button"
                        options={DURATION_OPTIONS.map((v) => ({ value: v, label: `${v} 秒` }))}
                      />
                    </Form.Item>
                  </Space>
                  <Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 12 }}>
                    画幅固定竖屏 9:16（1080x1920 / 30fps）。预算与质量阈值不在这里填，创建后在作品设置里调整。
                  </Paragraph>
                </>
              ),
            }]}
          />
        </Form>
      </Modal>
    </div>
  )
}
