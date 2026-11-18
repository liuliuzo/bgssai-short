import { useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd'
import api from '../../api/userAiModelKeys'
import { formatKeyBalance } from './keyBalance'
import { ABILITY_LABEL, legacyCredentialAbilities, modelKeyMask } from './modelKeySelection'

function showProbe(result) {
  if (result?.ok) message.success(result.message || '连接成功')
  else message.warning(result?.message || '连接检测未通过')
}

export default function CredentialManagementCard({ credentials, presets, rows, loading, busy, disabled, run }) {
  const [dialog, setDialog] = useState(null)
  const [form] = Form.useForm()
  const providerCode = Form.useWatch('provider_code', form)
  const preset = presets.find((item) => item.provider_code === providerCode)
  const dual = providerCode === 'KLING' || preset?.auth_type === 'AK_SK'
  const legacyUsage = legacyCredentialAbilities(credentials, rows)
  const editing = !!dialog?.credential_id
  const open = (row = {}) => {
    const first = presets[0]
    setDialog(row)
    form.resetFields()
    form.setFieldsValue({
      provider_code: row.provider_code || first?.provider_code,
      key_name: row.key_name || '',
      base_url: row.base_url || (row.provider_code ? presets.find((p) => p.provider_code === row.provider_code) : first)?.default_base_url,
    })
  }

  function close() {
    form.resetFields()
    setDialog(null)
  }

  async function save(values) {
    const fields = {
      key_name: (values.key_name || '').trim(),
      base_url: values.base_url.trim(),
      api_key: values.api_key?.trim() || undefined,
      secret_key: dual ? values.secret_key?.trim() || undefined : undefined,
    }
    const request = editing
      ? () => api.updateCredential({ ...fields, credential_id: dialog.credential_id })
      : () => api.createCredential({ ...fields, provider_code: values.provider_code })
    if (await run('save-credential', request, '密钥已保存')) close()
  }

  async function probe() {
    try {
      const values = await form.validateFields()
      await run('probe-draft', () => api.test({
        provider_code: values.provider_code, base_url: values.base_url,
        api_key: values.api_key, secret_key: dual ? values.secret_key : undefined,
      }), showProbe, false)
    } catch {
      // The form marks missing required fields.
    }
  }

  return (
    <Card title="密钥管理" style={{ marginBottom: 16 }}
      extra={<Button type="primary" disabled={disabled} onClick={() => open()}>新增密钥</Button>}>
      <Table size="small" rowKey="credential_id" pagination={false} loading={loading} scroll={{ x: 810 }}
        dataSource={credentials} locale={{ emptyText: '尚未添加供应商密钥' }}
        columns={[
          { title: '供应商', dataIndex: 'provider_name', render: (name, row) => name || row.provider_code },
          { title: '密钥名称', dataIndex: 'key_name', render: (name) => name || '未命名' },
          { title: '密钥', render: (_, row) => modelKeyMask(row) },
          { title: '余额', render: (_, row) => formatKeyBalance(row) },
          { title: '连接状态', render: (_, row) => {
            const label = { OK: '正常', FAILED: '失败' }[row.last_check_status]
            return label ? <span title={row.last_check_time || undefined}>{label}</span> : '未检测'
          } },
          { title: '当前密钥', render: (_, row) => Number(row.active) === 1 ? <Tag color="blue">当前使用</Tag>
            : <Space size={4} wrap>
                {[...(legacyUsage.get(row.credential_id) || [])].map((type) => (
                  <Tag key={type}>{ABILITY_LABEL[type] || type}使用中</Tag>
                ))}
                <Button size="small" disabled={disabled} loading={busy === 'activate-' + row.credential_id}
                  onClick={() => run('activate-' + row.credential_id, () => api.activateCredential({ credential_id: row.credential_id }), '已切换当前密钥')}>使用此密钥</Button>
              </Space> },
          { title: '操作', render: (_, row) => {
            const inUse = rows.some((model) => model.credential_id === row.credential_id)
            return (
              <Space size={4} wrap>
                <Button size="small" disabled={disabled} loading={busy === 'test-' + row.credential_id}
                  onClick={() => run('test-' + row.credential_id, () => api.testCredential({ credential_id: row.credential_id }), showProbe)}>测试</Button>
                <Button size="small" disabled={disabled} onClick={() => open(row)}>编辑</Button>
                <Popconfirm title={'删除密钥「' + (row.key_name || modelKeyMask(row)) + '」？'}
                  onConfirm={() => run('delete-' + row.credential_id, () => api.deleteCredential({ credential_id: row.credential_id }), '密钥已删除')}>
                  <Button size="small" danger disabled={disabled || inUse}
                    title={inUse ? '请先移除关联的模型配置或切换到另一把密钥' : undefined}>删除</Button>
                </Popconfirm>
              </Space>
            )
          } },
        ]}
      />
      <Modal open={!!dialog} title={editing ? '编辑密钥' : '新增密钥'} forceRender footer={null}
        onCancel={() => { if (!busy) close() }}>
        <Form form={form} layout="vertical" onFinish={save}>
          <Form.Item name="provider_code" label="供应商" rules={[{ required: true }]}>
            <Select disabled={editing} options={presets.map((p) => ({ value: p.provider_code, label: p.provider_name }))}
              onChange={(code) => form.setFieldsValue({ base_url: presets.find((p) => p.provider_code === code)?.default_base_url, api_key: '', secret_key: '' })} />
          </Form.Item>
          <Form.Item name="key_name" label="密钥名称">
            <Input maxLength={64} placeholder="例如 工作账号、测试账号" />
          </Form.Item>
          <Form.Item name="api_key" label={dual ? 'Access Key' : 'API Key'}
            rules={editing ? [] : [{ required: true, whitespace: true, message: dual ? '请输入 Access Key' : '请输入 API Key' }]}>
            <Input.Password autoComplete="new-password" placeholder={editing ? '留空保留原密钥' : undefined} />
          </Form.Item>
          {dual && <Form.Item name="secret_key" label="Secret Key" preserve={false}
            rules={editing ? [] : [{ required: true, whitespace: true, message: '请输入 Secret Key' }]}>
            <Input.Password autoComplete="new-password" placeholder={editing ? '留空保留原密钥' : undefined} />
          </Form.Item>}
          <Form.Item name="base_url" label="接口地址" rules={[{ required: true, whitespace: true, message: '请输入接口地址' }]}>
            <Input placeholder={preset?.default_base_url} />
          </Form.Item>
          <Space>
            {!editing && <Button disabled={!!busy} loading={busy === 'probe-draft'} onClick={probe}>测试连接</Button>}
            <Button type="primary" htmlType="submit" disabled={!!busy} loading={busy === 'save-credential'}>保存</Button>
          </Space>
        </Form>
      </Modal>
    </Card>
  )
}
