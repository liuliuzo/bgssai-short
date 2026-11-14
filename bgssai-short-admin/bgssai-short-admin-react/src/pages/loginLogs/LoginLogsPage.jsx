import { useEffect, useState, useCallback } from 'react'
import { Card, Descriptions, Drawer, Table, Tag, Typography } from 'antd'
import loginLogsApi from '../../api/loginLogs'
import { formatDateTime } from '../../lib/formatTime'

const { Title, Paragraph } = Typography

const METHODS = {
  password: '密码',
  email_otp: '邮箱验证码',
  phone_otp: '手机验证码',
  wechat: '微信',
  alipay: '支付宝',
  douyin: '抖音',
  baidu: '百度',
  bgssai: 'BGSSAI 账号',
  oauth: '第三方账号',
}

const CHANNELS = {
  web: '用户端网页',
  admin: '管理端',
  app: '应用',
}

const show = (value) => (value == null || value === '' ? '未知' : value)
const showTime = (value) => (value ? formatDateTime(value) : '未知')

function resultTag(row) {
  if (row.login_result === 'SUCCESS') return <Tag color="green">成功</Tag>
  if (row.login_result === 'FAILURE') return <Tag color="red">失败</Tag>
  return <Tag>{show(row.action)}</Tag>
}

export default function LoginLogsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(null)

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

  const columns = [
    { title: '时间', dataIndex: 'create_time', width: 180, render: showTime },
    { title: '用户', dataIndex: 'user_id', width: 160, render: show },
    { title: '结果', dataIndex: 'login_result', width: 90, render: (_, row) => resultTag(row) },
    { title: '原因', dataIndex: 'result_reason', width: 180, render: show },
    { title: '方式', dataIndex: 'login_method', width: 120, render: (value) => (value ? METHODS[value] || value : '未知') },
    { title: '账号', dataIndex: 'identifier_masked', width: 180, render: show },
    { title: '设备', dataIndex: 'device_label', width: 240, render: show },
    { title: '登录IP', dataIndex: 'ip_address', width: 150, render: show },
    { title: 'IP归属地（参考）', dataIndex: 'ip_location', width: 200, render: (value) => value || '未知' },
    { title: '对照', dataIndex: 'risk_summary', width: 160, render: (value) => value || '未知' },
  ]

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>登录日志</Title>
      <Paragraph type="secondary">
        全库登录记录（最新在前）。能在登录当时确定的结果、方式、设备和会话会记下来；采集不到的项显示为未知。IP归属地来自离线库，仅供参考，不代表精确位置。
      </Paragraph>
      <Card>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 1660 }}
          locale={{ emptyText: '暂无登录记录' }}
          onRow={(row) => ({ onClick: () => setCurrent(row), style: { cursor: 'pointer' } })}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (num, size) => load(num, size),
          }}
        />
      </Card>
      <Drawer title={current ? `登录记录 ${current.id}` : '登录记录'} open={!!current} width={520} onClose={() => setCurrent(null)}>
        {current ? (
          <>
            <Paragraph type="secondary">没有采集到的项显示为未知。这里不展示密码、验证码或令牌。</Paragraph>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="用户">{show(current.user_id)}</Descriptions.Item>
              <Descriptions.Item label="结果">{resultTag(current)} {show(current.result_reason)}</Descriptions.Item>
              <Descriptions.Item label="方式">{current.login_method ? METHODS[current.login_method] || current.login_method : '未知'}</Descriptions.Item>
              <Descriptions.Item label="账号">{show(current.identifier_masked)}</Descriptions.Item>
              <Descriptions.Item label="客户端">{current.client_channel ? CHANNELS[current.client_channel] || current.client_channel : '未知'}</Descriptions.Item>
              <Descriptions.Item label="浏览器">{show(current.browser_name)}</Descriptions.Item>
              <Descriptions.Item label="系统">{show(current.os_name)}</Descriptions.Item>
              <Descriptions.Item label="设备">{show(current.device_label)}</Descriptions.Item>
              <Descriptions.Item label="登录 IP">{show(current.ip_address)}</Descriptions.Item>
              <Descriptions.Item label="IP归属地（参考）">{current.ip_location || '未知'}</Descriptions.Item>
              <Descriptions.Item label="国家">{show(current.ip_country)}</Descriptions.Item>
              <Descriptions.Item label="省区">{show(current.ip_province)}</Descriptions.Item>
              <Descriptions.Item label="城市">{show(current.ip_city)}</Descriptions.Item>
              <Descriptions.Item label="运营商">{show(current.ip_isp)}</Descriptions.Item>
              <Descriptions.Item label="协议">{show(current.request_scheme)}</Descriptions.Item>
              <Descriptions.Item label="TLS">{show(current.tls_protocol)}</Descriptions.Item>
              <Descriptions.Item label="来源主机">{show(current.referer_host)}</Descriptions.Item>
              <Descriptions.Item label="入口路径">{show(current.entry_path)}</Descriptions.Item>
              <Descriptions.Item label="请求号">{show(current.request_id)}</Descriptions.Item>
              <Descriptions.Item label="语言">{show(current.accept_language)}</Descriptions.Item>
              <Descriptions.Item label="时区">{show(current.client_timezone)}</Descriptions.Item>
              <Descriptions.Item label="会话">{show(current.session_id)}</Descriptions.Item>
              <Descriptions.Item label="签发">{showTime(current.session_issued_at)}</Descriptions.Item>
              <Descriptions.Item label="过期">{showTime(current.session_expires_at)}</Descriptions.Item>
              <Descriptions.Item label="最近活跃">{showTime(current.session_last_seen_at)}</Descriptions.Item>
              <Descriptions.Item label="退出">{current.logout_time ? formatDateTime(current.logout_time) : '尚未记录'}</Descriptions.Item>
              <Descriptions.Item label="对照">{current.risk_summary || '未知'}</Descriptions.Item>
            </Descriptions>
            <Paragraph type="secondary" style={{ marginTop: 12 }}>
              {current.ip_geo_source === 'snapshot'
                ? '国家、省区和城市是登录当时从离线库记下的。'
                : current.ip_geo_source === 'lookup'
                  ? '这条旧记录没有地区快照，国家、省区和城市按当前离线库补全。'
                  : '离线库没有给出国家、省区、城市或运营商。'}
            </Paragraph>
          </>
        ) : null}
      </Drawer>
    </div>
  )
}
