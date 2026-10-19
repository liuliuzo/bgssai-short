import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Space, Table, Typography, message } from 'antd'
import subscriptionApi from '../../api/subscription'
import NativePayQrModal from './NativePayQrModal'
import {
  PAY_ORDER_POLL_INTERVAL_MS,
  resolvePollOutcome,
} from './payOrderPoll'
import { formatTokenAmount, formatTokenExact } from '../../utils/formatToken'

const { Title, Paragraph, Text } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

function TokenText({ value }) {
  return (
    <span title={formatTokenExact(value)}>{formatTokenAmount(value)}</span>
  )
}

export default function SubscriptionPage() {
  const [me, setMe] = useState(null)
  const [plans, setPlans] = useState([])
  const [packs, setPacks] = useState([])
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)
  const [payOrder, setPayOrder] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [polling, setPolling] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [meRes, planRes, packRes, orderRes] = await Promise.all([
        subscriptionApi.me(),
        subscriptionApi.listPlans(),
        subscriptionApi.listPacks(),
        subscriptionApi.listOrders(),
      ])
      setMe(meRes.data?.result || null)
      setPlans(planRes.data?.result?.plan_list || [])
      setPacks(packRes.data?.result?.pack_list || [])
      setOrders(orderRes.data?.result?.order_list || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const closePayModal = useCallback(() => {
    setModalOpen(false)
    setPolling(false)
    setPayOrder(null)
  }, [])

  useEffect(() => {
    if (!modalOpen || !polling || !payOrder?.order_no) return undefined

    let cancelled = false
    const tick = async () => {
      const outcomeFromExpiry = resolvePollOutcome('created', payOrder.expires_at)
      if (outcomeFromExpiry === 'timeout') {
        if (!cancelled) {
          message.warning('订单已超时，请重新下单')
          closePayModal()
        }
        return
      }
      try {
        const { data } = await subscriptionApi.orderStatus(payOrder.order_no)
        if (cancelled) return
        const status = data?.result?.status
        const outcome = resolvePollOutcome(status, payOrder.expires_at)
        if (outcome === 'paid') {
          message.success('支付成功，额度已按回调入账')
          closePayModal()
          load()
          return
        }
        if (outcome === 'closed') {
          message.warning('订单已关闭')
          closePayModal()
          return
        }
        if (outcome === 'timeout') {
          message.warning('订单已超时，请重新下单')
          closePayModal()
        }
      } catch {
        // keep polling
      }
    }

    tick()
    const timer = setInterval(tick, PAY_ORDER_POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [modalOpen, polling, payOrder, closePayModal, load])

  const buy = async (fn) => {
    try {
      const { data } = await fn()
      if (!data?.success || !data.result) return
      const result = data.result
      if (result.status === 'paid') {
        message.success('已开通')
        closePayModal()
        load()
        return
      }
      if (!result.code_url) {
        message.error('下单成功但未返回支付链接')
        return
      }
      setPayOrder({
        order_no: result.order_no,
        amount_cent: result.amount_cent,
        payment_channel: result.payment_channel,
        code_url: result.code_url,
        expires_at: result.expires_at,
        order_type: result.order_type,
      })
      setModalOpen(true)
      setPolling(true)
      message.info('请使用手机扫码完成支付；额度以支付回调入账为准')
    } catch (e) {
      message.error(e?.message || '下单失败')
    }
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>套餐与加量</Title>
      <Paragraph type="secondary">
        查看 BGSSAI 额度套餐、剩余额度与到期时间；开通、续费或加量后，额度以支付成功回调入账（重复回调只入账一次）。
      </Paragraph>
      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Space size="large" wrap>
          <Text>当前套餐 {me?.plan_name || me?.plan_code || '-'}</Text>
          <Text>状态 {me?.status || '-'}</Text>
          <Text>到期时间 {me?.current_period_end || '-'}</Text>
          <Text title={formatTokenExact(me?.available_tokens ?? me?.available_cent)}>
            剩余额度 <TokenText value={me?.available_tokens ?? me?.available_cent} />
          </Text>
          <Text title={formatTokenExact(me?.balance_tokens ?? me?.balance_cent)}>
            周期额度 <TokenText value={me?.balance_tokens ?? me?.balance_cent} />
          </Text>
          <Text title={formatTokenExact(me?.extra_tokens ?? me?.extra_balance_cent)}>
            加量余额 <TokenText value={me?.extra_tokens ?? me?.extra_balance_cent} />
          </Text>
        </Space>
      </Card>
      <Card title="开通 / 续费" style={{ marginBottom: 16 }}>
        <Table
          rowKey="plan_code"
          size="small"
          pagination={false}
          dataSource={plans}
          columns={[
            { title: '档位', dataIndex: 'plan_code' },
            { title: '名称', dataIndex: 'plan_name' },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            {
              title: '月度发放',
              dataIndex: 'monthly_balance_cent',
              render: (v) => <TokenText value={v} />,
            },
            {
              title: '操作',
              render: (_, row) => (
                Number(row.price_cent) === 0 ? <Text type="secondary">免费档无需支付</Text> : (
                <Space>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPlanOrder(row.plan_code, 'WECHAT'))}>微信开通/续费</Button>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPlanOrder(row.plan_code, 'ALIPAY'))}>支付宝开通/续费</Button>
                </Space>
                )
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
            {
              title: '到账额度',
              dataIndex: 'credit_cent',
              render: (v) => <TokenText value={v} />,
            },
            { title: '售价(元)', dataIndex: 'price_cent', render: yuan },
            {
              title: '操作',
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPackOrder(row.pack_code, 'WECHAT'))}>微信加量</Button>
                  <Button size="small" onClick={() => buy(() => subscriptionApi.createPackOrder(row.pack_code, 'ALIPAY'))}>支付宝加量</Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <Card title="订单记录" style={{ marginTop: 16 }}>
        <Table
          rowKey="order_no"
          size="small"
          pagination={false}
          dataSource={orders}
          columns={[
            { title: '订单号', dataIndex: 'order_no' },
            { title: '名称', dataIndex: 'biz_name' },
            { title: '类型', dataIndex: 'order_type' },
            { title: '金额(元)', dataIndex: 'amount_cent', render: yuan },
            {
              title: '发放额度',
              dataIndex: 'grant_cent',
              render: (v) => (v == null ? '-' : <TokenText value={v} />),
            },
            { title: '状态', dataIndex: 'status' },
            { title: '时间', dataIndex: 'created_at' },
          ]}
        />
      </Card>
      <NativePayQrModal
        open={modalOpen}
        order={payOrder}
        polling={polling}
        onClose={closePayModal}
      />
    </div>
  )
}
