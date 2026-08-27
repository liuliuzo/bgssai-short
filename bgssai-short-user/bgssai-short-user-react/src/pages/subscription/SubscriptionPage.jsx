import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Space, Table, Typography, message } from 'antd'
import subscriptionApi from '../../api/subscription'

const { Title, Paragraph, Text } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

export default function SubscriptionPage() {
  const [me, setMe] = useState(null)
  const [plans, setPlans] = useState([])
  const [packs, setPacks] = useState([])
  const [loading, setLoading] = useState(false)
  const [codeUrl, setCodeUrl] = useState('')
  const [pendingOrderNo, setPendingOrderNo] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [meRes, planRes, packRes] = await Promise.all([
        subscriptionApi.me(),
        subscriptionApi.listPlans(),
        subscriptionApi.listPacks(),
      ])
      setMe(meRes.data?.result || null)
      setPlans(planRes.data?.result?.plan_list || [])
      setPacks(packRes.data?.result?.pack_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!pendingOrderNo) return undefined
    const poll = setInterval(async () => {
      try {
        const { data } = await subscriptionApi.orderStatus(pendingOrderNo)
        if (data?.success && data.result?.status === 'paid') {
          message.success('支付成功，已开通')
          setPendingOrderNo('')
          setCodeUrl('')
          load()
        }
      } catch {
        // 轮询失败不打断扫码
      }
    }, 2500)
    return () => clearInterval(poll)
  }, [pendingOrderNo, load])

  const buy = async (fn) => {
    try {
      const { data } = await fn()
      if (data?.success && data.result) {
        setCodeUrl(data.result.code_url || '')
        if (data.result.status === 'paid') {
          message.success('已开通')
          setPendingOrderNo('')
          load()
        } else {
          setPendingOrderNo(data.result.order_no || '')
          message.success('请使用微信 / 支付宝扫描 code_url')
        }
      }
    } catch (e) {
      message.error(e?.message || '下单失败')
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>套餐与加量</Title>
      <Paragraph type="secondary">
        套餐发放周期余额 balance_cent；加量包进入 extra_balance_cent。金额为分，页面展示为元。
      </Paragraph>
      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Space size="large">
          <Text>当前套餐 {me?.plan_code || '-'}</Text>
          <Text>可用 {yuan(me?.available_cent)} 元</Text>
          <Text>周期 {yuan(me?.balance_cent)} 元</Text>
          <Text>加量 {yuan(me?.extra_balance_cent)} 元</Text>
        </Space>
      </Card>
      <Card title="订阅套餐" style={{ marginBottom: 16 }}>
        <Table
          rowKey="plan_code"
          size="small"
          pagination={false}
          dataSource={plans}
          columns={[
            { title: '档位', dataIndex: 'plan_code' },
            { title: '名称', dataIndex: 'plan_name' },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            { title: '月度发放(元)', dataIndex: 'monthly_balance_cent', render: yuan },
            {
              title: '操作',
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPlanOrder(row.plan_code, 'WECHAT'))}>微信</Button>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPlanOrder(row.plan_code, 'ALIPAY'))}>支付宝</Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Card title="加量包">
        <Table
          rowKey="pack_code"
          size="small"
          pagination={false}
          dataSource={packs}
          columns={[
            { title: '包', dataIndex: 'name' },
            { title: '到账(元)', dataIndex: 'credit_cent', render: yuan },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            {
              title: '操作',
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPackOrder(row.pack_code, 'WECHAT'))}>微信</Button>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPackOrder(row.pack_code, 'ALIPAY'))}>支付宝</Button>
                </Space>
              ),
            },
          ]}
        />
        {codeUrl ? <Paragraph style={{ marginTop: 12 }} copyable>{codeUrl}</Paragraph> : null}
      </Card>
    </div>
  )
}
