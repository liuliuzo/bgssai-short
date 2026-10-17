import UsageReceipts from '../../components/UsageReceipts'
import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Select, Space, Table, Tag, Typography } from 'antd'
import productionApi from '../../api/production'

const { Title, Paragraph } = Typography

export default function TasksPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [taskType, setTaskType] = useState()
  const [taskStatus, setTaskStatus] = useState()

  const load = useCallback(async (num, size) => {
    setLoading(true)
    try {
      const { data } = await productionApi.listTasks({
        page_num: num,
        page_size: size,
        task_type: taskType || undefined,
        task_status: taskStatus || undefined,
      })
      setRows(Array.isArray(data?.result) ? data.result : [])
      setTotal(Number(data?.total_size) || 0)
      setPageNum(Number(data?.page_num) || num)
      setPageSize(Number(data?.page_size) || size)
    } finally {
      setLoading(false)
    }
  }, [taskType, taskStatus])

  useEffect(() => {
    load(1, pageSize)
    const timer = setInterval(() => load(pageNum, pageSize), 3000)
    return () => clearInterval(timer)
  }, [load, pageNum, pageSize])

  return (
    <div>
      <UsageReceipts />
      <Title level={4} style={{ marginTop: 0 }}>任务中心</Title>
      <Paragraph type="secondary">文本 / 图片 / 视频同一套状态机。失败或取消可恢复排队，排队中可取消。成片播放在 Media。</Paragraph>
      <Card>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select allowClear placeholder="类型" style={{ width: 120 }} value={taskType} onChange={(v) => { setTaskType(v); load(1, pageSize) }}
            options={[{ value: 'TEXT', label: '文本' }, { value: 'IMAGE', label: '图片' }, { value: 'VIDEO', label: '视频' }, { value: 'VOICE', label: '配音' }, { value: 'COMPOSE', label: '合成' }]} />
          <Select allowClear placeholder="状态" style={{ width: 120 }} value={taskStatus} onChange={(v) => { setTaskStatus(v); load(1, pageSize) }}
            options={[{ value: 'QUEUED', label: '排队中' }, { value: 'RUNNING', label: '执行中' }, { value: 'SUCCEEDED', label: '已成功' }, { value: 'FAILED', label: '已失败' }, { value: 'CANCELED', label: '已取消' }]} />
        </Space>
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
            { title: '类型', dataIndex: 'task_type_name', width: 90, render: (v, row) => v || row.task_type },
            { title: '状态', dataIndex: 'task_status_name', width: 110, render: (v, row) => <Tag>{v || row.task_status}</Tag> },
            { title: '目标', dataIndex: 'target_type_name', width: 110, render: (v, row) => v || row.target_type },
            { title: '进度', dataIndex: 'progress_percent', width: 80 },
            { title: '费用(元)', dataIndex: 'cost_cent', width: 90, render: (v) => ((Number(v) || 0) / 100).toFixed(2) },
            { title: '失败原因', dataIndex: 'fail_reason' },
            {
              title: '操作',
              width: 180,
              render: (_, row) => (
                <Space>
                  {row.recoverable_flag === 1 || row.task_status === 'FAILED' || row.task_status === 'CANCELED'
                    ? <Button size="small" onClick={async () => { await productionApi.recoverTask(row.task_id); load(pageNum, pageSize) }}>恢复</Button>
                    : null}
                  {row.cancellable_flag === 1 || row.task_status === 'QUEUED' || row.task_status === 'RUNNING'
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
