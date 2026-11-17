import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import accountApi from '../../api/account'
import subscriptionApi from '../../api/subscription'
import http from '../../utils/http'
import { useUserStore } from '../../store/user'
import { formatTokenAmount, formatTokenExact } from '../../utils/formatToken'
import { labelOf, LOGIN_METHOD_LABEL } from '../../lib/displayLabels'
import PhoneDialField from '../../components/PhoneDialField'
import { composeApiPhone, DEFAULT_DIAL_CODE, validatePhoneParts } from '../../lib/phoneDial'
import UserAiModelKeysCard from './UserAiModelKeysCard'

const { Title, Text } = Typography

export default function AccountSettingsPage() {
  const navigate = useNavigate()
  const clearUserInfo = useUserStore((s) => s.clearUserInfo)
  const [methods, setMethods] = useState([])
  const [preview, setPreview] = useState(null)
  const [me, setMe] = useState(null)
  const [loading, setLoading] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [unbindRow, setUnbindRow] = useState(null)
  const [deleteForm] = Form.useForm()
  const deleteVerifyMethod = Form.useWatch('verify_method', deleteForm) || 'password'
  const [delDial, setDelDial] = useState(DEFAULT_DIAL_CODE)
  const [delPhone, setDelPhone] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [m, p, sub] = await Promise.all([
        accountApi.loginMethods(),
        accountApi.deletionPreview(),
        subscriptionApi.me(),
      ])
      setMethods(m.data?.result?.method_list || [])
      setPreview(p.data?.result || null)
      setMe(sub.data?.result || null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const sendCode = async (channel, target) => {
    try {
      if (channel === 'sms') {
        await http.post('/user/sms/send', { phone: target })
      } else {
        await http.post('/user/email/send', { email: target })
      }
      message.success('验证码已发送')
    } catch (e) {
      message.error(e?.message || '发送失败，请稍后重试')
    }
  }

  const onUnbind = async (values) => {
    try {
      const { data } = await accountApi.unbind({
        method_type: unbindRow.method_type,
        platform: unbindRow.platform,
        verify_method: values.verify_method,
        password: values.password,
        code: values.code,
      })
      if (!data?.success) return
      message.success('已解绑')
      setUnbindRow(null)
      load()
    } catch (e) {
      message.error(e?.message || '解绑失败，请稍后重试')
    }
  }

  const onDelete = async (values) => {
    let resolvedPhone = values.phone
    if (values.verify_method === 'sms' && delPhone) {
      const err = validatePhoneParts(delDial, delPhone)
      if (err) {
        message.error(err)
        return
      }
      resolvedPhone = composeApiPhone(delDial, delPhone)
    }
    try {
      const { data } = await accountApi.deleteAccount({
        verify_method: values.verify_method,
        password: values.password,
        phone: resolvedPhone,
        email: values.email,
        code: values.code,
        acknowledge_consequences: true,
        force_despite_open_items: values.force_despite_open_items || false,
        reason: values.reason,
      })
      if (!data?.success) return
      message.success('账号已注销')
      clearUserInfo()
      navigate('/login', { replace: true })
    } catch (e) {
      message.error(e?.message || '注销失败，请稍后重试')
    }
  }

  const sendDeleteSms = () => {
    const err = validatePhoneParts(delDial, delPhone)
    if (err) {
      message.error(err)
      return
    }
    sendCode('sms', composeApiPhone(delDial, delPhone))
  }

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>账号设置</Title>

      <Card loading={loading} title="额度概览" style={{ marginBottom: 16 }}>
        <Space size="large" wrap>
          <Text title={formatTokenExact(me?.available_tokens)}>
            剩余额度 {formatTokenAmount(me?.available_tokens)}
          </Text>
          <Text>到期 {me?.current_period_end || '-'}</Text>
          <Button type="link" onClick={() => navigate('/subscription')}>开通 / 续费 / 加量</Button>
        </Space>
      </Card>

      <UserAiModelKeysCard />

      <Card loading={loading} title="已绑定登录方式" style={{ marginBottom: 16 }}>
        <Table
          rowKey={(r) => `${r.method_type}-${r.platform}`}
          size="small"
          pagination={false}
          dataSource={methods}
          columns={[
            { title: '类型', dataIndex: 'method_type', render: (v) => labelOf(LOGIN_METHOD_LABEL, v, v) },
            { title: '平台', dataIndex: 'platform', render: (v) => labelOf(LOGIN_METHOD_LABEL, v, v) },
            { title: '标识', dataIndex: 'label' },
            {
              title: '操作',
              render: (_, row) => (
                <Button size="small" danger disabled={methods.length <= 1} onClick={() => setUnbindRow(row)}>
                  解绑
                </Button>
              ),
            },
          ]}
        />
        <BindContactForm onDone={load} sendCode={sendCode} />
      </Card>

      <Card title="账号注销" type="inner">
        {preview && (
          <ul>
            {(preview.consequences || []).map((c) => (
              <li key={c}>{c}</li>
            ))}
            <li>未结订单：{preview.open_payment_orders ?? 0}</li>
            <li>进行中任务：{preview.running_tasks ?? 0}</li>
          </ul>
        )}
        <Button danger onClick={() => setDeleteOpen(true)}>注销账号</Button>
      </Card>

      <Modal
        open={!!unbindRow}
        title="解绑登录方式"
        onCancel={() => setUnbindRow(null)}
        footer={null}
        destroyOnClose
      >
        <Form layout="vertical" onFinish={onUnbind}>
          <Form.Item name="verify_method" label="验证方式" rules={[{ required: true }]} initialValue="password">
            <Select options={[
              { value: 'password', label: '密码' },
              { value: 'sms', label: '手机验证码' },
              { value: 'email', label: '邮箱验证码' },
            ]} />
          </Form.Item>
          <Form.Item name="password" label="密码">
            <Input.Password />
          </Form.Item>
          <Form.Item name="code" label="验证码">
            <Input />
          </Form.Item>
          <Button type="primary" htmlType="submit" danger>确认解绑</Button>
        </Form>
      </Modal>

      <Modal
        open={deleteOpen}
        title="确认注销账号"
        onCancel={() => setDeleteOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={deleteForm} layout="vertical" onFinish={onDelete}>
          <Form.Item name="verify_method" label="身份验证" rules={[{ required: true }]} initialValue="password">
            <Select options={[
              { value: 'password', label: '密码' },
              { value: 'sms', label: '手机验证码' },
              { value: 'email', label: '邮箱验证码' },
            ]} />
          </Form.Item>
          {deleteVerifyMethod === 'password' && (
            <Form.Item name="password" label="密码">
              <Input.Password />
            </Form.Item>
          )}
          {deleteVerifyMethod === 'sms' && (
            <Form.Item label="手机号">
              <Space>
                <PhoneDialField
                  id="account-delete-phone"
                  dialCode={delDial}
                  nationalNumber={delPhone}
                  onDialCodeChange={setDelDial}
                  onNationalNumberChange={setDelPhone}
                  wrapperClassName="phone-dial-field phone-dial-field--compact"
                />
                <Button onClick={sendDeleteSms}>获取验证码</Button>
              </Space>
            </Form.Item>
          )}
          {deleteVerifyMethod === 'email' && (
            <Form.Item label="邮箱">
              <Space>
                <Form.Item name="email" noStyle>
                  <Input placeholder="请输入邮箱" />
                </Form.Item>
                <Button onClick={() => sendCode('email', deleteForm.getFieldValue('email'))}>获取验证码</Button>
              </Space>
            </Form.Item>
          )}
          {(deleteVerifyMethod === 'sms' || deleteVerifyMethod === 'email') && (
            <Form.Item name="code" label="验证码" rules={[{ required: true, message: '请输入验证码' }]}>
              <Input placeholder="6 位验证码" />
            </Form.Item>
          )}
          <Form.Item name="reason" label="原因（可选）">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="force_despite_open_items" valuePropName="checked">
            <label>
              <input type="checkbox" style={{ marginRight: 8 }} />
              我已处理未结事项，仍要注销
            </label>
          </Form.Item>
          <Button type="primary" danger htmlType="submit">确认注销</Button>
        </Form>
      </Modal>
    </div>
  )
}

function BindContactForm({ onDone, sendCode }) {
  const [form] = Form.useForm()
  const methodType = Form.useWatch('method_type', form) || 'phone'
  const [dial, setDial] = useState(DEFAULT_DIAL_CODE)
  const [national, setNational] = useState('')

  const submit = async (values) => {
    const payload = { ...values }
    if (methodType === 'phone') {
      const err = validatePhoneParts(dial, national)
      if (err) {
        message.error(err)
        return
      }
      payload.phone = composeApiPhone(dial, national)
    }
    try {
      const { data } = await accountApi.bindContact(payload)
      if (!data?.success) return
      message.success('绑定成功')
      form.resetFields()
      setNational('')
      onDone()
    } catch (e) {
      message.error(e?.message || '绑定失败，请稍后重试')
    }
  }

  return (
    <Form form={form} layout="inline" onFinish={submit} style={{ marginTop: 8, gap: 8 }}>
      <Form.Item name="method_type" initialValue="phone" rules={[{ required: true }]}>
        <Select style={{ width: 100 }} options={[
          { value: 'phone', label: '手机' },
          { value: 'email', label: '邮箱' },
        ]} />
      </Form.Item>
      {methodType === 'phone' ? (
        <Form.Item>
          <PhoneDialField
            id="account-bind-phone"
            dialCode={dial}
            nationalNumber={national}
            onDialCodeChange={setDial}
            onNationalNumberChange={setNational}
            wrapperClassName="phone-dial-field phone-dial-field--compact"
          />
        </Form.Item>
      ) : (
        <Form.Item name="email"><Input placeholder="邮箱" /></Form.Item>
      )}
      <Form.Item name="code" rules={[{ required: true, message: '请输入验证码' }]}>
        <Input placeholder="验证码" />
      </Form.Item>
      <Button onClick={() => {
        if (methodType === 'email') {
          sendCode('email', form.getFieldValue('email'))
          return
        }
        const err = validatePhoneParts(dial, national)
        if (err) {
          message.error(err)
          return
        }
        sendCode('sms', composeApiPhone(dial, national))
      }}>发送验证码</Button>
      <Button type="primary" htmlType="submit">绑定</Button>
    </Form>
  )
}
