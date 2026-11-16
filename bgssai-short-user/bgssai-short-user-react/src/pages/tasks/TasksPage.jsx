import UsageReceipts from '../../components/UsageReceipts'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Select, Space, Table, Tag, Typography } from 'antd'
import productionApi from '../../api/production'
import { labelOf, TARGET_TYPE_LABEL, visibleFailReason } from '../../lib/displayLabels'
import { formatDateTime } from '../../lib/formatTime'
import { hasActiveTasks, TASK_POLL_INTERVAL_MS } from './tasksPoll'

const { Title } = Typography

function taskTime(value) {
  const text = formatDateTime(value)
  return text === '-' ? '—' : text
}

export default function TasksPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [taskType, setTaskType] = useState()
  const [taskStatus, setTaskStatus] = useState()

  const activeRef = useRef(false)
  const pageRef = useRef({ num: 1, size: 20 })
  const inFlightRef = useRef(false)

  const load = useCallback(async (num, size, options = {}) => {
    if (inFlightRef.current && options.silent) return
    inFlightRef.current = true
    if (!options.silent) setLoading(true)
    try {
      const { data } = await productionApi.listTasks({
        page_num: num,
        page_size: size,
        task_type: taskType || undefined,
        task_status: taskStatus || undefined,
      })
      const list = Array.isArray(data?.result) ? data.result : []
      const resolvedNum = Number(data?.page_num) || num
      const resolvedSize = Number(data?.page_size) || size
      activeRef.current = hasActiveTasks(list)
      pageRef.current = { num: resolvedNum, size: resolvedSize }
      setRows(list)
      setTotal(Number(data?.total_size) || 0)
      setPageNum(resolvedNum)
      setPageSize(resolvedSize)
    } catch {
      // The response interceptor already surfaced the failure; keep current rows.
    } finally {
      inFlightRef.current = false
      if (!options.silent) setLoading(false)
    }
  }, [taskType, taskStatus])

  const loadRef = useRef(load)
  loadRef.current = load

  // A filter change restarts from page 1 exactly once (selects only set state).
  useEffect(() => {
    load(1, pageRef.current.size)
  }, [load])

  // Refresh the page the user is on, only while it shows unsettled tasks.
  useEffect(() => {
    const timer = setInterval(() => {
      if (!activeRef.current || document.hidden) return
      loadRef.current(pageRef.current.num, pageRef.current.size, { silent: true })
    }, TASK_POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [])

  return (
    <div>
      <UsageReceipts />
      <Title level={4} style={{ marginTop: 0 }}>任务中心</Title>
      <Card>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select allowClear placeholder="类型" style={{ width: 120 }} value={taskType} onChange={setTaskType}
            options={[{ value: 'TEXT', label: '文本' }, { value: 'IMAGE', label: '图片' }, { value: 'VIDEO', label: '视频' }, { value: 'VOICE', label: '配音' }, { value: 'COMPOSE', label: '合成' }]} />
          <Select allowClear placeholder="状态" style={{ width: 120 }} value={taskStatus} onChange={setTaskStatus}
            options={[{ value: 'QUEUED', label: '排队中' }, { value: 'RUNNING', label: '执行中' }, { value: 'SUCCEEDED', label: '已成功' }, { value: 'FAILED', label: '已失败' }, { value: 'CANCELED', label: '已取消' }]} />
        </Space>
        <Table
          rowKey="task_id"
          size="small"
          loading={loading}
          dataSource={rows}
          scroll={{ x: 1180 }}
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
            { title: '目标', dataIndex: 'target_type_name', width: 110, render: (v, row) => v || labelOf(TARGET_TYPE_LABEL, row.target_type, row.target_type) },
            { title: '进度', dataIndex: 'progress_percent', width: 220, render: (_, row) => row.progress_hint || row.progress_percent },
            { title: '开始时间', dataIndex: 'started_time', width: 170, render: taskTime },
            { title: '结束时间', dataIndex: 'finished_time', width: 170, render: taskTime },
            { title: '费用(元)', dataIndex: 'cost_cent', width: 90, render: (v) => ((Number(v) || 0) / 100).toFixed(2) },
            { title: '失败原因', key: 'fail_reason', render: (_, row) => visibleFailReason(row) || '—' },
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
