import { useEffect, useState, useCallback } from 'react'
import { Card, Table, Tag, Typography } from 'antd'
import { formatDateTime } from '../../lib/formatTime'
import loginLogsApi from '../../api/loginLogs'

const { Title, Paragraph } = Typography

const ACTIONS = {
  LOGIN_SUCCESS: { text: '登录成功', color: 'green' },
  LOGIN_FAIL_PASSWORD: { text: '口令错误', color: 'red' },
  LOGIN_FAIL_ROLE: { text: '无权限', color: 'orange' },
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
  { title: '动作', dataIndex: 'action', width: 180, render: formatAction },
  { title: '登录IP', dataIndex: 'ip_address', width: 180 },
  { title: 'IP归属地（参考）', dataIndex: 'ip_location', width: 260, render: (value) => value || '未知' },
  { title: '时间', dataIndex: 'create_time', width: 200, render: formatTime },
]

export default function LoginLogsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
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
    let cancelled = false
    load(1, 20)
    const retry = setTimeout(() => { if (!cancelled) load(1, 20) }, 1500)
    const onVisible = () => { if (document.visibilityState === 'visible') load(1, 20) }
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('focus', onVisible)
    return () => {
      cancelled = true
      clearTimeout(retry)
      document.removeEventListener('visibilitychange', onVisible)
      window.removeEventListener('focus', onVisible)
    }
  }, [load])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>登录日志</Title>
      <Paragraph type="secondary">最近登录与安全相关操作记录，仅展示本人账号。IP归属地显示离线库可识别的国家/地区/城市，仅供参考，不代表精确位置；未覆盖时显示未知。</Paragraph>
      <Card>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={COLUMNS}
          dataSource={rows}
          scroll={{ x: 900 }}
          locale={{ emptyText: '还没有登录记录。' }}
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
