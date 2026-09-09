import { QRCodeSVG } from 'qrcode.react'
import { Button, Modal, Space, Typography } from 'antd'

const { Paragraph, Text } = Typography

function yuan(cent) {
  return ((Number(cent) || 0) / 100).toFixed(2)
}

function channelLabel(channel) {
  if (channel === 'WECHAT') return '微信'
  if (channel === 'ALIPAY') return '支付宝'
  return channel || '-'
}

/**
 * Native / face-to-face pay QR modal (WeChat code_url or Alipay precreate URL).
 */
export default function NativePayQrModal({
  open,
  order,
  polling,
  onClose,
}) {
  const codeUrl = order?.code_url || ''
  const title = order?.payment_channel === 'ALIPAY' ? '支付宝扫码支付' : '微信扫码支付'

  return (
    <Modal
      open={open}
      title={title}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose}>关闭</Button>,
      ]}
      destroyOnClose
      width={420}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%', alignItems: 'center' }}>
        {codeUrl ? (
          <div style={{ padding: 12, background: '#fff', border: '1px solid #f0f0f0' }}>
            <QRCodeSVG value={codeUrl} size={200} level="M" includeMargin={false} />
          </div>
        ) : (
          <Text type="danger">未返回支付二维码链接</Text>
        )}
        <div style={{ width: '100%' }}>
          <Paragraph style={{ marginBottom: 4 }}>
            <Text type="secondary">订单号 </Text>
            <Text copyable={order?.order_no ? { text: order.order_no } : false}>
              {order?.order_no || '-'}
            </Text>
          </Paragraph>
          <Paragraph style={{ marginBottom: 4 }}>
            <Text type="secondary">金额 </Text>
            <Text>{yuan(order?.amount_cent)} 元</Text>
          </Paragraph>
          <Paragraph style={{ marginBottom: 4 }}>
            <Text type="secondary">渠道 </Text>
            <Text>{channelLabel(order?.payment_channel)}（{order?.payment_channel || '-'}）</Text>
          </Paragraph>
          {codeUrl ? (
            <Paragraph type="secondary" copyable={{ text: codeUrl }} style={{ marginBottom: 0, wordBreak: 'break-all' }}>
              {codeUrl}
            </Paragraph>
          ) : null}
        </div>
        <Text type="secondary">
          {polling ? '等待扫码支付中，请勿关闭…' : '已停止轮询'}
        </Text>
      </Space>
    </Modal>
  )
}
