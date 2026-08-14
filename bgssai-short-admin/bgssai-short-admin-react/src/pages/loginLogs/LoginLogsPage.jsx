import { useEffect, useState, useCallback } from 'react'
import { Card, Table, Typography } from 'antd'
import loginLogsApi from '../../api/loginLogs'

const { Title, Paragraph } = Typography

const COLUMNS = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户 ID', dataIndex: 'user_id' },
  { title: '动作', dataIndex: 'action', width: 180 },
  { title: 'IP', dataIndex: 'ip_address', width: 160 },
  { title: '时间', dataIndex: 'create_time', width: 200 },
]

// Sample paged list page. Reads the PageResponse envelope:
// { result: [...], page_num, page_size, total_size }.
export default function LoginLogsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)

  const load = useCallback(async (num, size) => {
    setLoading(true)
    try {
      const { data } = await loginLogsApi.list({ page_num: num, page_size: size })
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

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>登录日志</Title>
      <Paragraph type="secondary">
        示例分页列表，对应后端 GET /bgssai/admin/login-logs（@NeedAop 鉴权 + PageHelper 物理分页）。
      </Paragraph>

      <Card>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={COLUMNS}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size),
          }}
        />
      </Card>
    </div>
  )
}
