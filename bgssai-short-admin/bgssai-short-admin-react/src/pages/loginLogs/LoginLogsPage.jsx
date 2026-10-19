import { useEffect, useState, useCallback } from 'react'
import { Card, Table, Tag, Typography } from 'antd'
import loginLogsApi from '../../api/loginLogs'
import { formatDateTime } from '../../lib/formatTime'

const { Title, Paragraph } = Typography

const ACTIONS = {
  LOGIN_SUCCESS: { text: '登录成功', color: 'green' },
  LOGIN_FAIL_PASSWORD: { text: '口令错误', color: 'red' },
  LOGIN_FAIL_ROLE: { text: '无管理端权限', color: 'orange' },
  LOGIN_FAIL_NO_ROLE: { text: '无角色', color: 'orange' },
  LOGOUT: { text: '退出登录', color: 'default' },
}

const formatTime = (v) => formatDateTime(v)
const formatAction = (value) => {
  const known = ACTIONS[value]
  return known ? <Tag color={known.color}>{known.text}</Tag> : value || '-'
}

const COLUMNS = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户 ID', dataIndex: 'user_id' },
  { title: '动作', dataIndex: 'action', width: 180, render: formatAction },
  { title: 'IP', dataIndex: 'ip_address', width: 160 },
  { title: '时间', dataIndex: 'create_time', width: 200, render: formatTime },
]

export default function LoginLogsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)

  const load = useCallback(async (num, size) => {
    setLoading(true)
    try {
      const { data } = await loginLogsApi.list({ page_num: num, page_size: size, _t: Date.now() })
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
      <Paragraph type="secondary">全库登录记录（最新在前）。</Paragraph>
      <Card>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={COLUMNS}
          dataSource={rows}
          locale={{ emptyText: '暂无登录记录' }}
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
