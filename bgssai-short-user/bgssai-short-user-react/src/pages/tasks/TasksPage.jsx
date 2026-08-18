import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Space, Table, Tag, Typography } from 'antd'
import productionApi from '../../api/production'

const { Title, Paragraph } = Typography

export default function TasksPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)

  const load = useCallback(async (num, size) => {
    setLoading(true)
    try {
      const { data } = await productionApi.listTasks({ page_num: num, page_size: size })
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
    const timer = setInterval(() => load(pageNum, pageSize), 3000)
    return () => clearInterval(timer)
  }, [load, pageNum, pageSize])

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>任务中心</Title>
      <Paragraph type="secondary">轮询 Mock 生成队列。失败可重试，排队中可取消。</Paragraph>
      <Card>
        <Table
          rowKey="task_id"
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
            { title: '类型', dataIndex: 'task_type', width: 90 },
            { title: '状态', dataIndex: 'task_status', width: 110, render: (v) => <Tag>{v}</Tag> },
            { title: '目标', dataIndex: 'target_type', width: 110 },
            { title: '进度', dataIndex: 'progress_percent', width: 80 },
            { title: '费用(分)', dataIndex: 'cost_cent', width: 90 },
            { title: '失败原因', dataIndex: 'fail_reason' },
            {
              title: '操作',
              width: 160,
              render: (_, row) => (
                <Space>
                  {row.task_status === 'FAILED' ? <Button size="small" onClick={async () => { await productionApi.retryTask(row.task_id); load(pageNum, pageSize) }}>重试</Button> : null}
                  {row.task_status === 'QUEUED' || row.task_status === 'RUNNING'
                    ? <Button size="small" onClick={async () => { await productionApi.cancelTask(row.task_id); load(pageNum, pageSize) }}>取消</Button>
                    : null}
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}
