import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, Select, Space, Switch, Typography, message } from 'antd'
import productionApi from '../../api/production'

const { Text } = Typography

function readFile(file) {
  return new Promise((resolve, reject) => {
    if (!file) {
      reject(new Error('empty'))
      return
    }
    if (file.size > 6 * 1024 * 1024) {
      reject(new Error('size'))
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const text = String(reader.result || '')
      const comma = text.indexOf(',')
      resolve(comma >= 0 ? text.slice(comma + 1) : text)
    }
    reader.onerror = () => reject(new Error('read'))
    reader.readAsDataURL(file)
  })
}

export default function LookLibraryPage() {
  const [libraries, setLibraries] = useState([])
  const [libraryId, setLibraryId] = useState()
  const [loading, setLoading] = useState(false)
  const fileRef = useRef(null)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    const { data } = await productionApi.listLookLibraries()
    const list = data?.result?.library_list || []
    setLibraries(list)
    setLibraryId((current) => current || (list[0] && list[0].library_id))
  }, [])

  useEffect(() => { load() }, [load])

  const current = libraries.find((item) => item.library_id === libraryId)

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card title="定妆知识库">
        <Text type="secondary">把角色、场景、道具的参考图放在这里。挂到定妆后，生成候选时会按名称和说明把最接近的图交给模型。</Text>
        <Form form={form} layout="inline" style={{ marginTop: 12 }} onFinish={async (values) => {
          setLoading(true)
          try {
            await productionApi.saveLookLibrary({
              library_name: values.library_name,
              description: values.description,
              mount_portrait: values.mount_portrait === false ? 0 : 1,
              status: 'ACTIVE',
            })
            form.resetFields()
            await load()
          } finally {
            setLoading(false)
          }
        }}>
          <Form.Item name="library_name" rules={[{ required: true, message: '填写名称' }]}><Input placeholder="知识库名称" /></Form.Item>
          <Form.Item name="description"><Input placeholder="用途说明" style={{ width: 240 }} /></Form.Item>
          <Form.Item name="mount_portrait" valuePropName="checked" initialValue={true}><Switch checkedChildren="定妆参考" unCheckedChildren="仅存放" /></Form.Item>
          <Form.Item><Button htmlType="submit" type="primary" loading={loading}>新建</Button></Form.Item>
        </Form>
      </Card>
      <Card title="素材" extra={
        <Space>
          <Select
            style={{ width: 220 }}
            placeholder="选择知识库"
            value={libraryId}
            onChange={setLibraryId}
            options={libraries.map((item) => ({ value: item.library_id, label: item.library_name }))}
          />
          <Button disabled={!libraryId} onClick={() => fileRef.current?.click()}>上传图片</Button>
        </Space>
      }>
        <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={async (event) => {
          const file = event.target.files && event.target.files[0]
          event.target.value = ''
          if (!file || !libraryId) return
          try {
            const imageBase64 = await readFile(file)
            const name = file.name.replace(/\.[^.]+$/, '') || '未命名定妆'
            await productionApi.uploadLookMaterial({
              library_id: libraryId,
              material_name: name,
              caption: name,
              material_kind: 'CHARACTER',
              image_base64: imageBase64,
            })
            message.success('已放入知识库')
            await load()
          } catch (err) {
            message.error(err && err.message === 'size' ? '图片不能超过 6MB' : '上传没有成功')
          }
        }} />
        {(current?.material_list || []).map((item) => (
          <Card key={item.material_id} size="small" style={{ marginBottom: 8 }} title={item.material_name} extra={
            <Button size="small" onClick={async () => { await productionApi.deleteLookMaterial(item.material_id); await load() }}>移除</Button>
          }>
            <Space>
              {item.file_url ? <img alt={item.material_name} src={item.file_url} style={{ width: 72, height: 96, objectFit: 'cover' }} /> : null}
              <Text type="secondary">{item.caption || item.material_kind}</Text>
            </Space>
          </Card>
        ))}
        {current && !(current.material_list || []).length ? <Text type="secondary">这个知识库还没有图片。</Text> : null}
      </Card>
    </Space>
  )
}
