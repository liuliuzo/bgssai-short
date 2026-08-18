import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Space, Table, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import productionApi from '../../api/production'

const { Title, Paragraph } = Typography

export default function WorksPage() {
  const navigate = useNavigate()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
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

  const create = async () => {
    const values = await form.validateFields()
    const { data } = await productionApi.createWork(values)
    if (data?.success) {
      message.success('已创建，正在生成大纲')
      setOpen(false)
      form.resetFields()
      navigate(`/works/${data.result.work_id}/script`)
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>我的作品</Title>
      <Paragraph type="secondary">创建短剧后按剧本、角色、分镜、成片、提审走完闭环。本环境 AI 为模拟生成。</Paragraph>
      <Card extra={<Button type="primary" onClick={() => setOpen(true)}>新建作品</Button>}>
        <Table
          rowKey="work_id"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size),
          }}
          columns={[
            { title: '标题', dataIndex: 'work_title' },
            { title: '类型', dataIndex: 'genre_name', width: 100 },
            { title: '状态', dataIndex: 'work_status_name', width: 100 },
            { title: '集数', dataIndex: 'episode_count', width: 80 },
            {
              title: '已花费(元)',
              dataIndex: 'cost_spent_cent',
              width: 110,
              render: (v) => ((Number(v) || 0) / 100).toFixed(2),
            },
            {
              title: '操作',
              width: 160,
              render: (_, row) => (
                <Space>
                  <Button type="link" size="small" onClick={() => navigate(`/works/${row.work_id}/script`)}>进入制作</Button>
                  <Button type="link" size="small" onClick={async () => {
                    await productionApi.archiveWork(row.work_id)
                    load(pageNum, pageSize)
                  }}>归档</Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Modal title="新建作品" open={open} onOk={create} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="work_title" label="作品标题" rules={[{ required: true }]}>
            <Input placeholder="退婚三年后她成了首富" />
          </Form.Item>
          <Form.Item name="one_line_pitch" label="一句话故事" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="被退婚的落魄千金三年后带着资本回归" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
