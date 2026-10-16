import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd'
import { formatDateTime } from '../../lib/formatTime'
import reviewsApi from '../../api/reviews'

const { Title, Paragraph } = Typography

export default function ReviewsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [status, setStatus] = useState()
  const [keyword, setKeyword] = useState('')
  const [current, setCurrent] = useState(null)
  const [form] = Form.useForm()

  const load = useCallback(async (num, size, reviewStatus, kw) => {
    setLoading(true)
    try {
      const { data } = await reviewsApi.list({
        page_num: num,
        page_size: size,
        review_status: reviewStatus,
        keyword: kw,
      })
      setRows(Array.isArray(data?.result) ? data.result : [])
      setTotal(Number(data?.total_size) || 0)
      setPageNum(Number(data?.page_num) || num)
      setPageSize(Number(data?.page_size) || size)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(1, 20) }, [load])

  const openDetail = async (reviewId) => {
    const { data } = await reviewsApi.detail(reviewId)
    setCurrent(data?.result)
    form.resetFields()
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>内容审核</Title>
      <Paragraph type="secondary">通过后剧集 APPROVED；全部通过则作品 COMPLETED。驳回必须填写驳回项。</Paragraph>
      <Card>
        <Space style={{ marginBottom: 12 }}>
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 160 }}
            value={status}
            onChange={setStatus}
            options={['PENDING', 'APPROVED', 'REJECTED'].map((v) => ({ value: v, label: v }))}
          />
          <Input placeholder="作品标题或审核单号" value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 240 }} />
          <Button onClick={() => load(1, pageSize, status, keyword)}>查询</Button>
        </Space>
        <Table
          rowKey="review_id"
          size="small"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size, status, keyword),
          }}
          columns={[
            { title: '作品', dataIndex: 'work_title' },
            { title: '集', dataIndex: 'episode_no', width: 70 },
            { title: '状态', dataIndex: 'review_status', width: 110 },
            { title: '风险', dataIndex: 'risk_level', width: 90 },
            { title: '创作者', dataIndex: 'creator_user_id' },
            { title: '提交时间', dataIndex: 'submitted_time', width: 180, render: (v) => formatDateTime(v) },
            {
              title: '操作',
              width: 90,
              render: (_, row) => <Button type="link" size="small" onClick={() => openDetail(row.review_id)}>处理</Button>,
            },
          ]}
        />
      </Card>
      <Modal
        title="审核处理"
        open={!!current}
        onCancel={() => setCurrent(null)}
        footer={current?.review_status === 'PENDING' ? [
          <Button key="reject" danger onClick={async () => {
            const values = await form.validateFields()
            const { data } = await reviewsApi.reject({
              review_id: current.review_id,
              review_comment: values.review_comment,
              reject_item_list: [{
                risk_category: values.risk_category,
                reject_desc: values.reject_desc,
                suggestion: values.suggestion,
              }],
            })
            if (data?.success) {
              message.success('已驳回')
              setCurrent(null)
              load(pageNum, pageSize, status, keyword)
            }
          }}>驳回</Button>,
          <Button key="ok" type="primary" onClick={async () => {
            const values = form.getFieldsValue()
            const { data } = await reviewsApi.approve({
              review_id: current.review_id,
              review_comment: values.review_comment,
            })
            if (data?.success) {
              message.success('已通过')
              setCurrent(null)
              load(pageNum, pageSize, status, keyword)
            }
          }}>通过</Button>,
        ] : [<Button key="close" onClick={() => setCurrent(null)}>关闭</Button>]}
      >
        <Paragraph>作品 {current?.work_title} / 第 {current?.episode_no} 集 / {current?.review_status}</Paragraph>
        <Form form={form} layout="vertical">
          <Form.Item name="review_comment" label="审核意见"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="risk_category" label="驳回类目" initialValue="VIOLENCE"><Input /></Form.Item>
          <Form.Item name="reject_desc" label="驳回说明"><Input /></Form.Item>
          <Form.Item name="suggestion" label="修改建议"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
