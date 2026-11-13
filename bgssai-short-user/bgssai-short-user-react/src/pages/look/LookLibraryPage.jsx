import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Image, Input, Modal, Skeleton, Space, Switch, Typography, message } from 'antd'
import { Link, useSearchParams } from 'react-router-dom'
import productionApi from '../../api/production'
import { safeReturnPath } from '../../lib/safeReturn'
import {
  LOOK_ANGLES,
  LOOK_KINDS,
  anglesForKind,
  arrangeAngle,
  emptySlotsForKind,
  groupLooksOfKind,
  librarySummaryCounts,
  librarySummaryText,
  normalizeLookKind,
  primaryAngleForKind,
} from './lookGroups'
import { laplacianScore, posterTimes } from './lookPoster'

const { Paragraph, Text } = Typography

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

const PREVIEW_Z = 2000

function seekTo(video, time) {
  return new Promise((resolve, reject) => {
    if (Math.abs((video.currentTime || 0) - time) < 0.03) {
      resolve()
      return
    }
    const timer = setTimeout(() => {
      video.removeEventListener('seeked', onSeek)
      reject(new Error('seek'))
    }, 1200)
    const onSeek = () => {
      clearTimeout(timer)
      resolve()
    }
    video.addEventListener('seeked', onSeek, { once: true })
    try {
      video.currentTime = time
    } catch (err) {
      clearTimeout(timer)
      video.removeEventListener('seeked', onSeek)
      reject(err)
    }
  })
}

function captureFrame(video) {
  const sourceWidth = video.videoWidth || 720
  const sourceHeight = video.videoHeight || 1280
  const scale = Math.min(1, 720 / sourceWidth)
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(sourceWidth * scale))
  canvas.height = Math.max(1, Math.round(sourceHeight * scale))
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  const sampleW = 48
  const sampleH = Math.max(3, Math.round(canvas.height * (sampleW / canvas.width)))
  const sample = document.createElement('canvas')
  sample.width = sampleW
  sample.height = sampleH
  sample.getContext('2d').drawImage(canvas, 0, 0, sampleW, sampleH)
  const score = laplacianScore(sample.getContext('2d').getImageData(0, 0, sampleW, sampleH))
  return { score, url: canvas.toDataURL('image/jpeg', 0.85) }
}

function posterOf(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'auto'
    video.muted = true
    video.playsInline = true
    video.src = url
    const fail = () => {
      URL.revokeObjectURL(url)
      reject(new Error('poster'))
    }
    video.onerror = fail
    video.onloadeddata = async () => {
      let bestScore = -1
      let bestUrl = ''
      for (const time of posterTimes(video.duration)) {
        try {
          await seekTo(video, time)
          const frame = captureFrame(video)
          if (frame.score > bestScore) {
            bestScore = frame.score
            bestUrl = frame.url
          }
        } catch {
          // try the next moment
        }
      }
      URL.revokeObjectURL(url)
      if (!bestUrl) {
        reject(new Error('poster'))
        return
      }
      const comma = bestUrl.indexOf(',')
      resolve(comma >= 0 ? bestUrl.slice(comma + 1) : bestUrl)
    }
  })
}

function PhotoThumb({ item, width, height, alt }) {
  const [broken, setBroken] = useState(false)
  if (!item.file_url || broken) {
    return (
      <div style={{ width, height, boxSizing: 'border-box', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', color: '#8c8c8c', fontSize: 12, lineHeight: 1.45, background: '#f6f7f9' }}>
        <div style={{ whiteSpace: 'nowrap' }}>这张照片</div>
        <div style={{ whiteSpace: 'nowrap' }}>没有加载出来</div>
      </div>
    )
  }
  return (
    <Image
      src={item.file_url}
      alt={alt || item.material_name || '定妆照片'}
      width={width}
      height={height}
      style={{ objectFit: 'contain', background: '#f6f7f9', cursor: 'zoom-in', display: 'block' }}
      preview={{ mask: '查看', zIndex: PREVIEW_Z }}
      onError={() => setBroken(true)}
    />
  )
}

function uploadButtonStyle(disabled) {
  return {
    width: '100%',
    height: 32,
    border: '1px dashed #d9d9d9',
    borderRadius: 8,
    background: '#fafafa',
    cursor: disabled ? 'not-allowed' : 'pointer',
    color: '#595959',
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitBatch(batchId) {
  const deadline = Date.now() + 16 * 60 * 1000
  while (Date.now() < deadline) {
    const { data } = await productionApi.listTasks({ batch_id: batchId, page_num: 1, page_size: 20 })
    const rows = Array.isArray(data?.result) ? data.result : []
    if (rows.length && rows.every((row) => row.task_status === 'SUCCEEDED' || row.task_status === 'FAILED' || row.task_status === 'CANCELED')) {
      return rows[0]
    }
    await sleep(2000)
  }
  return null
}

function sideMedia(look, kind) {
  const angles = anglesForKind(kind)
  const unslottedLabel = kind === 'SCENE' ? '未分机位' : kind === 'PROP' ? '未分视角' : '未分面'
  const extras = []
  const videos = []
  const broken = []
  let duplicated = false
  const pools = [
    ...angles.map(([angle, label]) => ({ label, items: (look.slots && look.slots[angle]) || [] })),
    { label: unslottedLabel, items: look.extra || [] },
  ]
  const noteDup = (item) => {
    if (item && item.same_material_ids && item.same_material_ids.length > 1) duplicated = true
  }
  for (const pool of pools) {
    const view = arrangeAngle(pool.items)
    noteDup(view.primary)
    for (const item of view.extras) {
      noteDup(item)
      extras.push({ ...item, angleLabel: pool.label })
    }
    for (const item of view.videos) {
      noteDup(item)
      videos.push({ ...item, angleLabel: pool.label })
    }
    if (pool.label === unslottedLabel && view.primary) {
      noteDup(view.primary)
      extras.push({ ...view.primary, angleLabel: pool.label })
    }
    const rest = (pool.label === unslottedLabel || view.primary) ? view.missing : view.missing.slice(1)
    for (const item of rest) broken.push({ ...item, angleLabel: pool.label })
  }
  return { extras, videos, broken, duplicated }
}

const KIND_COPY = {
  CHARACTER: {
    itemNoun: '造型',
    slotNoun: '面',
    namePlaceholder: '造型名称，例如白纱花瓣',
    descPlaceholder: '外观特征（可选，例如黑长直、珍珠耳环）',
    emptyText: '这个库里还没有人物造型。写一个造型名称，再往四面里放照片。',
    uploadEmpty: '上传这一面',
    subHint: '每个造型按正脸、前面、背面、侧面排好；先放好正脸，还可以按正脸补齐其他面。点照片可看大图。',
    slotWidth: 112,
    slotHeight: 148,
    extraWidth: 84,
    extraHeight: 112,
  },
  SCENE: {
    itemNoun: '场景',
    slotNoun: '机位',
    namePlaceholder: '场景名称，例如总裁办公室、雨夜便利店',
    descPlaceholder: '布景与光影特征（可选，例如落地窗夜景、暖光吧台）',
    emptyText: '这个库里还没有场景参考。可以先添加一个场景名称上传全景主图，也可以在作品场景卡上点「存入场景库」。',
    uploadEmpty: '上传参考图',
    subHint: '每个场景按全景主图、中景机位、局部陈设排好；全景主图会在生成场景与空镜时优先参考。点照片可看大图。',
    slotWidth: 164,
    slotHeight: 108,
    extraWidth: 112,
    extraHeight: 74,
  },
  PROP: {
    itemNoun: '道具',
    slotNoun: '视角',
    namePlaceholder: '道具名称，例如退婚书、祖传玉佩',
    descPlaceholder: '材质与外观特征（可选，例如烫金红封、青白玉）',
    emptyText: '这个库里还没有道具参考。可以先添加一个道具名称上传主视图，也可以在作品道具卡上点「存入道具库」。',
    uploadEmpty: '上传参考图',
    subHint: '每个道具按主视图、特写细节排好；主视图会在生成道具参考图时优先参考。点照片可看大图。',
    slotWidth: 120,
    slotHeight: 120,
    extraWidth: 84,
    extraHeight: 84,
  },
}

export default function LookLibraryPage() {
  const [params, setParams] = useSearchParams()
  const activeKind = normalizeLookKind(params.get('kind'))
  const kindUi = KIND_COPY[activeKind] || KIND_COPY.CHARACTER
  const activeAngles = anglesForKind(activeKind)
  const primaryAngle = primaryAngleForKind(activeKind)
  const back = safeReturnPath(params.get('return'))
  const backLabel = back && back.includes('/characters') ? '返回角色与场景' : '返回'
  const [libraries, setLibraries] = useState([])
  const [librariesReady, setLibrariesReady] = useState(false)
  const [libraryId, setLibraryId] = useState()
  const [creating, setCreating] = useState(false)
  const [libraryName, setLibraryName] = useState('')
  const [lookName, setLookName] = useState('')
  const [lookDesc, setLookDesc] = useState('')
  const [drafts, setDrafts] = useState([])
  const [busy, setBusy] = useState(false)
  const [playing, setPlaying] = useState(null)
  const fileRef = useRef(null)
  const uploadTarget = useRef(null)

  const switchKind = (nextKind) => {
    const normalized = normalizeLookKind(nextKind)
    const next = new URLSearchParams(params)
    if (normalized === 'CHARACTER') next.delete('kind')
    else next.set('kind', normalized)
    setParams(next, { replace: true })
    setLookName('')
    setLookDesc('')
  }

  const load = useCallback(async () => {
    try {
      const { data } = await productionApi.listLookLibraries()
      const list = data?.result?.library_list || []
      setLibraries(list)
      setLibraryId((current) => current || (list[0] && list[0].library_id))
      return list
    } finally {
      setLibrariesReady(true)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const current = libraries.find((item) => item.library_id === libraryId)
  const currentCounts = librarySummaryCounts(current?.material_list)
  const looks = groupLooksOfKind(current?.material_list, activeKind)
  const named = new Set(looks.map((look) => look.name))
  const visibleDrafts = drafts.filter((draft) => draft.kind === activeKind && !named.has(draft.name))

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
    const desc = lookDesc.trim()
    if (!name) {
      message.error(`先写${kindUi.itemNoun}名称`)
      return
    }
    if (!named.has(name) && !drafts.some((row) => row.kind === activeKind && row.name === name)) {
      setDrafts((rows) => [{ kind: activeKind, name, desc }, ...rows])
    }
    setLookName('')
    setLookDesc('')
  }

  function openUpload(name, angle, desc = '') {
    uploadTarget.current = { kind: activeKind, name, angle, desc }
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
      const angles = anglesForKind(target.kind)
      const angleName = angles.find(([value]) => value === target.angle)?.[1] || '参考'
      const captionParts = [target.name, target.desc, angleName].filter(Boolean)
      await productionApi.uploadLookMaterial({
        library_id: libraryId,
        material_name: target.name,
        caption: captionParts.join(' '),
        material_kind: target.kind,
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

  async function removeIds(ids) {
    for (const id of ids) {
      await productionApi.deleteLookMaterial(id)
    }
    await load()
  }

  function removeMaterial(item) {
    const ids = (item && item.same_material_ids && item.same_material_ids.length)
      ? item.same_material_ids
      : [item.material_id]
    if (ids.length > 1) {
      Modal.confirm({
        title: '移除这张照片',
        content: '库里这张照片有重复的一份，会一起拿掉。其他不同的照片还留着。',
        okText: '移除',
        cancelText: '取消',
        onOk: () => removeIds(ids),
      })
      return
    }
    removeIds(ids)
  }

  function faceStill(look) {
    return arrangeAngle(look.slots.FRONT_FACE).primary
  }

  function missingBody(look) {
    return LOOK_ANGLES.filter(([angle]) => angle !== 'FRONT_FACE' && !arrangeAngle(look.slots[angle]).primary)
  }

  async function generateAngles(look, angle, replace) {
    if (!libraryId || !look || busy) return
    const body = {
      library_id: libraryId,
      material_name: look.name,
      replace_existing: replace ? 1 : 0,
      confirm_cost: 0,
    }
    if (angle) body.view_angle = angle
    setBusy(true)
    let quote
    try {
      const { data } = await productionApi.completeLookAngles(body)
      if (!data?.success) {
        if (!data?.message) message.error('没有排上生成')
        return
      }
      quote = data.result || {}
    } catch {
      message.error('没有排上生成')
      return
    } finally {
      setBusy(false)
    }
    const names = (quote.angle_name_list || []).join('、') || '这一面'
    const cost = Number(quote.estimated_cost_cent) || 0
    const price = cost > 0 ? `预计 ${(cost / 100).toFixed(2)} 元。` : '费用按这次出图对账。'
    Modal.confirm({
      title: replace ? `按正脸重做${names}` : `按正脸生成${names}`,
      content: `会把「${look.name}」的正脸交给模型，要求还是同一个人、同一套衣服。${price}模型有时仍会画走样，不满意可以再重做。`,
      okText: '开始生成',
      cancelText: '取消',
      onOk: async () => {
        setBusy(true)
        try {
          const { data } = await productionApi.completeLookAngles({ ...body, confirm_cost: 1 })
          if (!data?.success || !data.result?.batch_id) {
            if (!data?.message) message.error('没有排上生成')
            return
          }
          const done = await waitBatch(data.result.batch_id)
          if (!done) message.info('还在生成，可以到任务中心看结果')
          else if (done.task_status !== 'SUCCEEDED') message.error(done.fail_reason || '这一面没有生成出来')
          else message.success('已按正脸画好')
          await load()
        } catch {
          message.error('没有排上生成')
        } finally {
          setBusy(false)
        }
      },
    })
  }

  function removeLook(look) {
    const items = [
      ...activeAngles.flatMap(([angle]) => look.slots[angle] || []),
      ...(look.extra || []),
    ]
    Modal.confirm({
      title: `删除「${look.name}」`,
      content: `这个${kindUi.itemNoun}里的参考图都会从库里拿掉。`,
      okText: '删除',
      cancelText: '取消',
      onOk: async () => {
        for (const item of items) {
          await productionApi.deleteLookMaterial(item.material_id)
        }
        setDrafts((rows) => rows.filter((row) => !(row.kind === activeKind && row.name === look.name)))
        await load()
      },
    })
  }

  function renderLook(look, draft, draftDesc = '') {
    const side = sideMedia(look, activeKind)
    const isCharacter = activeKind === 'CHARACTER'
    const face = isCharacter ? faceStill(look) : null
    const missing = isCharacter ? missingBody(look) : []
    const firstCaption = [
      ...activeAngles.flatMap(([angle]) => look.slots[angle] || []),
      ...(look.extra || []),
    ].find((item) => item && item.caption)?.caption || draftDesc
    return (
      <Card
        key={`${activeKind}-${look.name}`}
        size="small"
        style={{ marginBottom: 12 }}
        title={look.name}
        extra={draft ? (
          <Button type="link" size="small" onClick={() => setDrafts((rows) => rows.filter((row) => !(row.kind === activeKind && row.name === look.name)))}>取消</Button>
        ) : (
          <Button type="link" size="small" onClick={() => removeLook(look)}>删除{kindUi.itemNoun}</Button>
        )}
      >
        {firstCaption && firstCaption !== look.name ? (
          <div style={{ marginBottom: 8 }}>
            <Text type="secondary">匹配特征：{firstCaption}</Text>
          </div>
        ) : null}
        <Image.PreviewGroup preview={{ zIndex: PREVIEW_Z }}>
          <div style={{ display: 'flex', gap: 12, overflowX: 'auto', alignItems: 'flex-start' }}>
            {activeAngles.map(([angle, label]) => {
              const view = arrangeAngle(look.slots[angle])
              const shown = view.primary || view.missing[0] || null
              return (
                <div key={angle} data-angle={angle} style={{ width: kindUi.slotWidth, flex: `0 0 ${kindUi.slotWidth}px` }}>
                  <div style={{ minHeight: 34, marginBottom: 6, fontSize: 12, lineHeight: '17px', color: '#595959' }}>
                    <div>{label}</div>
                    <div style={{ minHeight: 17, color: '#d48806' }}>{angle === primaryAngle ? '生成时优先用' : ''}</div>
                  </div>
                  <div style={{
                    width: kindUi.slotWidth,
                    height: kindUi.slotHeight,
                    boxSizing: 'border-box',
                    borderRadius: 8,
                    overflow: 'hidden',
                    border: shown ? '1px solid #f0f0f0' : '1px dashed #d9d9d9',
                    background: '#f6f7f9',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                  >
                    {view.primary ? <PhotoThumb item={view.primary} width="100%" height="100%" alt={`${look.name} ${label}`} /> : (
                      <div data-empty={shown ? undefined : 'true'} style={{ fontSize: 12, color: '#8c8c8c', textAlign: 'center', padding: '0 8px', lineHeight: 1.5 }}>
                        {shown ? (
                          <>
                            <div>这张照片</div>
                            <div>没有加载出来</div>
                          </>
                        ) : view.videos.length ? (
                          <>
                            <div>还没有照片</div>
                            <div>参考视频在下面</div>
                          </>
                        ) : (
                          <>
                            <div>这一{kindUi.slotNoun}</div>
                            <div>还没有参考图</div>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                  <div style={{ minHeight: 24 }}>
                    {shown ? <Button type="link" size="small" style={{ padding: 0, height: 24 }} onClick={() => removeMaterial(shown)}>移除</Button> : null}
                  </div>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => openUpload(look.name, angle, draftDesc)}
                    style={uploadButtonStyle(busy)}
                  >
                    {shown ? '再传一张' : kindUi.uploadEmpty}
                  </button>
                  {isCharacter && face && angle !== 'FRONT_FACE' ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => generateAngles(look, angle, !!shown)}
                      style={{ ...uploadButtonStyle(busy), marginTop: 6 }}
                    >
                      {shown ? '按正脸重做' : '按正脸生成'}
                    </button>
                  ) : null}
                </div>
              )
            })}
          </div>
          {isCharacter && face ? (
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">缺的面可以按正脸生成。侧面和背面会跟着这张正脸的人和衣服来画，模型有时仍会走样。</Text>
              {missing.length >= 2 ? (
                <div>
                  <Button type="link" size="small" disabled={busy} style={{ padding: 0 }} onClick={() => generateAngles(look, null, false)}>一次补齐缺的面</Button>
                </div>
              ) : null}
            </div>
          ) : null}
          {side.duplicated ? <div style={{ marginTop: 8 }}><Text type="secondary">有的照片在多个卡片上是同一张，这里只显示一次。</Text></div> : null}
          {side.extras.length || side.broken.length ? (
            <div style={{ marginTop: 14 }}>
              <div style={{ fontSize: 13, marginBottom: 4 }}>其他参考图</div>
              <Text type="secondary">上面是每个{kindUi.slotNoun}最新的一张。更早上传或未分{kindUi.slotNoun}的放在这里，点开可以看大图，不需要就移除。</Text>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 8 }}>
                {side.extras.map((item) => (
                  <div key={item.material_id} style={{ width: kindUi.extraWidth }}>
                    <div style={{ fontSize: 12, color: '#595959', marginBottom: 4 }}>{item.angleLabel}</div>
                    <PhotoThumb item={item} width={kindUi.extraWidth} height={kindUi.extraHeight} alt={`${look.name} ${item.angleLabel}`} />
                    <Button type="link" size="small" style={{ padding: 0 }} onClick={() => removeMaterial(item)}>移除</Button>
                  </div>
                ))}
                {side.broken.map((item) => (
                  <div key={item.material_id} style={{ width: kindUi.extraWidth }}>
                    <div style={{ fontSize: 12, color: '#595959', marginBottom: 4 }}>{item.angleLabel}</div>
                    <div style={{ width: kindUi.extraWidth, height: kindUi.extraHeight, borderRadius: 8, background: '#f6f7f9', color: '#8c8c8c', fontSize: 12, lineHeight: 1.45, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', boxSizing: 'border-box' }}>
                      <div style={{ whiteSpace: 'nowrap' }}>这张照片</div>
                      <div style={{ whiteSpace: 'nowrap' }}>没有加载出来</div>
                    </div>
                    <Button type="link" size="small" style={{ padding: 0 }} onClick={() => removeMaterial(item)}>移除</Button>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
          {side.videos.length ? (
            <div style={{ marginTop: 14 }}>
              <div style={{ fontSize: 13, marginBottom: 4 }}>参考视频</div>
              <Text type="secondary">这是视频，不拿来当静态参考图。封面只是其中一帧，点开可以播放。</Text>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 8 }}>
                {side.videos.map((item) => (
                  <div key={item.material_id} data-video="true" style={{ width: kindUi.extraWidth }}>
                    <div style={{ fontSize: 12, color: '#595959', marginBottom: 4 }}>{item.angleLabel}</div>
                    <button
                      type="button"
                      onClick={() => setPlaying(item)}
                      style={{ width: kindUi.extraWidth, height: kindUi.extraHeight, padding: 0, border: '1px solid #f0f0f0', borderRadius: 8, background: '#f6f7f9', cursor: 'pointer', position: 'relative', overflow: 'hidden' }}
                    >
                      {item.poster_url ? (
                        <img alt={`${look.name} ${item.angleLabel} 参考视频`} src={item.poster_url} style={{ width: '100%', height: '100%', objectFit: 'contain', display: 'block' }} />
                      ) : (
                        <span style={{ fontSize: 12, color: '#8c8c8c' }}>点此播放</span>
                      )}
                      <span style={{ position: 'absolute', left: 0, right: 0, bottom: 0, fontSize: 12, lineHeight: '22px', background: 'rgba(0,0,0,0.55)', color: '#fff' }}>播放</span>
                    </button>
                    <Button type="link" size="small" style={{ padding: 0 }} onClick={() => removeMaterial(item)}>移除</Button>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </Image.PreviewGroup>
      </Card>
    )
  }

  const kindCountOf = (kindKey) => {
    if (kindKey === 'SCENE') return currentCounts.sceneCount
    if (kindKey === 'PROP') return currentCounts.propCount
    return currentCounts.characterCount
  }

  return (
    <Card
      title="定妆与场景参考库"
      extra={(
        <Space>
          {back ? <Link to={back}>{backLabel}</Link> : <Link to="/works">去我的作品</Link>}
          <Button type="primary" onClick={() => setCreating(true)}>新建参考库</Button>
        </Space>
      )}
    >
      <Paragraph style={{ marginBottom: 8, color: '#334155', lineHeight: 1.7 }}>
        这里放可以跨剧集、跨作品反复使用的人物造型、场景参考和道具参考。在作品的角色与场景卡上可以直接选用，也可以把已锁定的参考图一键存回库里。
      </Paragraph>
      <Paragraph style={{ marginBottom: 0, color: '#334155', lineHeight: 1.7 }}>
        {kindUi.subHint}
      </Paragraph>
      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', marginTop: 16 }}>
        <div style={{ width: 236, flexShrink: 0 }}>
          {!librariesReady ? <Skeleton active title={false} paragraph={{ rows: 4 }} /> : null}
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
              <div style={{ fontWeight: item.library_id === libraryId ? 600 : 400 }}>{item.library_name}</div>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {librarySummaryText(item.material_list)}
                {item.mount_portrait === 1 ? ' · 生成时参考' : ' · 先放着'}
              </Text>
            </button>
          ))}
          {librariesReady && !libraries.length ? <Text>还没有参考库也没关系。在角色卡、场景卡或道具卡上上传照片，会自动放进「我的定妆」。</Text> : null}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          {current ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                <Space wrap>
                  {LOOK_KINDS.map((kindItem) => (
                    <Button
                      key={kindItem.key}
                      type={kindItem.key === activeKind ? 'primary' : 'default'}
                      onClick={() => switchKind(kindItem.key)}
                    >
                      {kindItem.label}（{kindCountOf(kindItem.key)}）
                    </Button>
                  ))}
                </Space>
                {current.status === 'ARCHIVED' ? (
                  <Space wrap>
                    <Text>这个库已归档，生成时不会自动匹配。</Text>
                    <Button size="small" disabled={busy} onClick={restoreLibrary}>恢复</Button>
                  </Space>
                ) : (
                  <Space wrap>
                    <Text>生成参考图时自动匹配这个库</Text>
                    <Switch checked={current.mount_portrait === 1} disabled={busy} onChange={toggleMount} />
                  </Space>
                )}
              </div>
              <Space wrap>
                <Input
                  value={lookName}
                  placeholder={kindUi.namePlaceholder}
                  style={{ width: 260 }}
                  onChange={(event) => setLookName(event.target.value)}
                  onPressEnter={addLook}
                />
                <Input
                  value={lookDesc}
                  placeholder={kindUi.descPlaceholder}
                  style={{ width: 300 }}
                  onChange={(event) => setLookDesc(event.target.value)}
                  onPressEnter={addLook}
                />
                <Button onClick={addLook}>添加{kindUi.itemNoun}</Button>
              </Space>
              {visibleDrafts.map((draft) => renderLook({ name: draft.name, slots: emptySlotsForKind(activeKind), extra: [] }, true, draft.desc))}
              {looks.map((look) => renderLook(look, false))}
              {!looks.length && !visibleDrafts.length ? <Text type="secondary">{kindUi.emptyText}</Text> : null}
            </Space>
          ) : librariesReady ? (
            <Text>还没有打开参考库。左边列出库之后会自动打开第一个；也可以点「新建参考库」。</Text>
          ) : <Skeleton active paragraph={{ rows: 6 }} />}
        </div>
      </div>
      <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp,video/mp4" hidden onChange={onFile} />
      <Modal
        title={playing ? `参考视频 · ${playing.angleLabel || ''}` : '参考视频'}
        open={!!playing}
        footer={null}
        zIndex={PREVIEW_Z}
        destroyOnHidden
        onCancel={() => setPlaying(null)}
        width={420}
      >
        {playing ? (
          <div>
            <video
              src={playing.file_url}
              poster={playing.poster_url || undefined}
              controls
              autoPlay
              playsInline
              style={{ width: '100%', maxHeight: 520, background: '#111', borderRadius: 8 }}
            />
            <div style={{ marginTop: 8 }}><Text type="secondary">封面只是视频里的一帧。发糊或切得很近时，不会和静态参考图摆在一起。</Text></div>
          </div>
        ) : null}
      </Modal>
      <Modal
        title="新建参考库"
        open={creating}
        okText="创建"
        cancelText="取消"
        confirmLoading={busy}
        onCancel={() => setCreating(false)}
        onOk={createLibrary}
      >
        <Input
          value={libraryName}
          placeholder="例如现代都市剧资产库、古装王府场景库"
          onChange={(event) => setLibraryName(event.target.value)}
          onPressEnter={createLibrary}
        />
        <div style={{ marginTop: 8 }}><Text type="secondary">同一个参考库里可以分类存放人物定妆、场景参考和道具参考，整部剧集可直接共用。</Text></div>
      </Modal>
    </Card>
  )
}
