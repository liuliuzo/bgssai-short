import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Input, Modal, Space, Switch, Typography, message } from 'antd'
import { Link, useSearchParams } from 'react-router-dom'
import productionApi from '../../api/production'
import { safeReturnPath } from '../../lib/safeReturn'
import LookSlot from './LookSlot'
import { LOOK_ANGLES, groupLooks } from './lookGroups'

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

function posterOf(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'auto'
    video.muted = true
    video.playsInline = true
    video.src = url
    const finish = () => {
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth || 720
      canvas.height = video.videoHeight || 1280
      canvas.getContext('2d').drawImage(video, 0, 0)
      URL.revokeObjectURL(url)
      const data = canvas.toDataURL('image/jpeg', 0.85)
      const comma = data.indexOf(',')
      resolve(comma >= 0 ? data.slice(comma + 1) : data)
    }
    video.onloadeddata = () => {
      const target = Number.isFinite(video.duration) ? Math.min(0.4, video.duration / 2) : 0
      if (target > 0) video.currentTime = target
      else finish()
    }
    video.onseeked = finish
    video.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('poster'))
    }
  })
}

function MaterialView({ item, onRemove }) {
  const video = item.mime_type && item.mime_type.startsWith('video/')
  return (
    <div style={{ marginBottom: 6 }}>
      {video && item.file_url
        ? <video src={item.file_url} poster={item.poster_url || undefined} style={{ width: 104, height: 140, objectFit: 'cover', borderRadius: 8, display: 'block' }} muted />
        : (item.file_url ? <img alt={item.material_name} src={item.file_url} style={{ width: 104, height: 140, objectFit: 'cover', borderRadius: 8, display: 'block' }} /> : null)}
      <Button type="link" size="small" style={{ padding: 0 }} onClick={onRemove}>移除</Button>
    </div>
  )
}

export default function LookLibraryPage() {
  const [params] = useSearchParams()
  const back = safeReturnPath(params.get('return'))
  const backLabel = back && back.includes('/characters') ? '返回角色定妆' : '返回'
  const [libraries, setLibraries] = useState([])
  const [libraryId, setLibraryId] = useState()
  const [creating, setCreating] = useState(false)
  const [libraryName, setLibraryName] = useState('')
  const [lookName, setLookName] = useState('')
  const [drafts, setDrafts] = useState([])
  const [busy, setBusy] = useState(false)
  const fileRef = useRef(null)
  const uploadTarget = useRef(null)

  const load = useCallback(async () => {
    const { data } = await productionApi.listLookLibraries()
    const list = data?.result?.library_list || []
    setLibraries(list)
    setLibraryId((current) => current || (list[0] && list[0].library_id))
    return list
  }, [])

  useEffect(() => { load() }, [load])

  const current = libraries.find((item) => item.library_id === libraryId)
  const looks = groupLooks(current?.material_list)
  const named = new Set(looks.map((look) => look.name))
  const visibleDrafts = drafts.filter((name) => !named.has(name))

  async function createLibrary() {
    const name = libraryName.trim()
    if (!name) {
      message.error('先写知识库名称')
      return
    }
    setBusy(true)
    try {
      const { data } = await productionApi.saveLookLibrary({
        library_name: name,
        description: '',
        mount_portrait: 1,
        status: 'ACTIVE',
      })
      const id = data?.result?.library_id
      setCreating(false)
      setLibraryName('')
      if (id) setLibraryId(id)
      await load()
    } catch {
      message.error('知识库没有建成')
    } finally {
      setBusy(false)
    }
  }

  async function restoreLibrary() {
    if (!current) return
    setBusy(true)
    try {
      await productionApi.saveLookLibrary({
        library_id: current.library_id,
        library_name: current.library_name,
        description: current.description || '',
        mount_portrait: 1,
        status: 'ACTIVE',
      })
      await load()
    } catch {
      message.error('没有恢复成功')
    } finally {
      setBusy(false)
    }
  }

  async function toggleMount(checked) {
    if (!current) return
    setBusy(true)
    try {
      await productionApi.saveLookLibrary({
        library_id: current.library_id,
        library_name: current.library_name,
        description: current.description || '',
        mount_portrait: checked ? 1 : 0,
        status: current.status || 'ACTIVE',
      })
      await load()
    } catch {
      message.error('没有改成功')
    } finally {
      setBusy(false)
    }
  }

  function addLook() {
    const name = lookName.trim()
    if (!name) {
      message.error('先写造型名称')
      return
    }
    if (!named.has(name) && !drafts.includes(name)) setDrafts((rows) => [name, ...rows])
    setLookName('')
  }

  function openUpload(name, angle) {
    uploadTarget.current = { name, angle }
    fileRef.current?.click()
  }

  async function onFile(event) {
    const file = event.target.files && event.target.files[0]
    event.target.value = ''
    const target = uploadTarget.current
    if (!file || !target || !libraryId) return
    setBusy(true)
    try {
      const imageBase64 = await readFile(file)
      const video = (file.type || '').startsWith('video/') || /\.mp4$/i.test(file.name)
      const posterBase64 = video ? await posterOf(file) : undefined
      const angleName = LOOK_ANGLES.find(([value]) => value === target.angle)?.[1] || '参考'
      await productionApi.uploadLookMaterial({
        library_id: libraryId,
        material_name: target.name,
        caption: `${target.name} ${angleName}`,
        material_kind: 'CHARACTER',
        view_angle: target.angle,
        image_base64: imageBase64,
        poster_base64: posterBase64,
      })
      message.success(`已放入「${target.name}」的${angleName}`)
      await load()
    } catch (err) {
      message.error(err && err.message === 'size' ? '文件不能超过 6MB' : '上传没有成功')
    } finally {
      setBusy(false)
    }
  }

  async function removeMaterial(item) {
    await productionApi.deleteLookMaterial(item.material_id)
    await load()
  }

  function removeLook(look) {
    const items = [
      ...LOOK_ANGLES.flatMap(([angle]) => look.slots[angle]),
      ...look.extra,
    ]
    Modal.confirm({
      title: `删除「${look.name}」`,
      content: '这个造型里的照片都会从库里拿掉。',
      okText: '删除',
      cancelText: '取消',
      onOk: async () => {
        for (const item of items) {
          await productionApi.deleteLookMaterial(item.material_id)
        }
        setDrafts((rows) => rows.filter((name) => name !== look.name))
        await load()
      },
    })
  }

  function renderLook(look, draft) {
    return (
      <Card
        key={look.name}
        size="small"
        style={{ marginBottom: 12 }}
        title={look.name}
        extra={draft ? (
          <Button type="link" size="small" onClick={() => setDrafts((rows) => rows.filter((name) => name !== look.name))}>取消</Button>
        ) : (
          <Button type="link" size="small" onClick={() => removeLook(look)}>删除造型</Button>
        )}
      >
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto' }}>
          {LOOK_ANGLES.map(([angle, label]) => {
            const items = look.slots[angle]
            return (
              <LookSlot
                key={angle}
                label={label}
                note={angle === 'FRONT_FACE' ? '生成优先用' : ''}
                action={items.length ? '再传一张' : '上传'}
                disabled={busy}
                onAction={() => openUpload(look.name, angle)}
              >
                {items.length ? items.map((item) => (
                  <MaterialView key={item.material_id} item={item} onRemove={() => removeMaterial(item)} />
                )) : null}
              </LookSlot>
            )
          })}
        </div>
        {look.extra.length ? (
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">参考图</Text>
            <Space wrap>
              {look.extra.map((item) => (
                <MaterialView key={item.material_id} item={item} onRemove={() => removeMaterial(item)} />
              ))}
            </Space>
          </div>
        ) : null}
      </Card>
    )
  }

  return (
    <Card
      title="定妆知识库"
      extra={(
        <Space>
          {back ? <Link to={back}>{backLabel}</Link> : <Link to="/works">去我的作品</Link>}
          <Button type="primary" onClick={() => setCreating(true)}>新建知识库</Button>
        </Space>
      )}
    >
      <Text type="secondary">这里管理可以反复使用的造型。给角色定妆时，在角色卡上就能直接选，不用先跑来这里。</Text>
      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', marginTop: 16 }}>
        <div style={{ width: 200, flexShrink: 0 }}>
          {libraries.map((item) => (
            <button
              key={item.library_id}
              type="button"
              onClick={() => setLibraryId(item.library_id)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                marginBottom: 8,
                padding: '10px 12px',
                borderRadius: 8,
                cursor: 'pointer',
                border: item.library_id === libraryId ? '1px solid #d48806' : '1px solid #f0f0f0',
                background: item.library_id === libraryId ? '#fff7e6' : '#fff',
              }}
            >
              <div>{item.library_name}</div>
              <Text type="secondary">
                {groupLooks(item.material_list).length} 个造型
                {item.mount_portrait === 1 ? ' · 生成时参考' : ' · 先放着'}
              </Text>
            </button>
          ))}
          {!libraries.length ? <Text type="secondary">还没有知识库也没关系。在角色卡上上传照片，会自动放进「我的定妆」。</Text> : null}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          {current ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {current.status === 'ARCHIVED' ? (
                <Space wrap>
                  <Text>这个库已归档，生成时不会自动用。</Text>
                  <Button size="small" disabled={busy} onClick={restoreLibrary}>恢复</Button>
                </Space>
              ) : (
                <Space wrap>
                  <Text>生成定妆时参考这个库</Text>
                  <Switch checked={current.mount_portrait === 1} disabled={busy} onChange={toggleMount} />
                </Space>
              )}
              <Space wrap>
                <Input
                  value={lookName}
                  placeholder="造型名称，例如白纱花瓣"
                  style={{ width: 240 }}
                  onChange={(event) => setLookName(event.target.value)}
                  onPressEnter={addLook}
                />
                <Button onClick={addLook}>添加造型</Button>
              </Space>
              {visibleDrafts.map((name) => renderLook({ name, slots: { FRONT_FACE: [], FRONT: [], BACK: [], SIDE: [] }, extra: [] }, true))}
              {looks.map((look) => renderLook(look, false))}
              {!looks.length && !visibleDrafts.length ? <Text type="secondary">这个库还没有造型。写一个名称，再往四面里放照片。</Text> : null}
            </Space>
          ) : <Text type="secondary">先在左边选一个知识库。</Text>}
        </div>
      </div>
      <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp,video/mp4" hidden onChange={onFile} />
      <Modal
        title="新建知识库"
        open={creating}
        okText="创建"
        cancelText="取消"
        confirmLoading={busy}
        onCancel={() => setCreating(false)}
        onOk={createLibrary}
      >
        <Input
          value={libraryName}
          placeholder="例如礼服装"
          onChange={(event) => setLibraryName(event.target.value)}
          onPressEnter={createLibrary}
        />
        <div style={{ marginTop: 8 }}><Text type="secondary">建好后就可以往里面加造型。生成定妆时会用到这个库。</Text></div>
      </Modal>
    </Card>
  )
}
