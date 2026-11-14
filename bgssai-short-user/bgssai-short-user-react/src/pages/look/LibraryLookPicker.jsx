import { useEffect, useState } from 'react'
import { Button, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'
import productionApi from '../../api/production'
import { groupLooksOfKind, lookCover, referenceImage, referenceLine } from './lookGroups'

const { Text } = Typography

function shotUrl(item) {
  if (!item) return ''
  if (item.mime_type && item.mime_type.startsWith('video/')) return item.poster_url || ''
  return item.file_url || item.poster_url || ''
}

export function ReferenceNotice({ reference, textOnly }) {
  const image = textOnly ? null : referenceImage(reference)
  return (
    <div>
      {image ? (
        <img
          alt={image.label}
          src={image.url}
          style={{ width: 72, height: 96, objectFit: 'cover', borderRadius: 8, display: 'block', marginBottom: 8 }}
        />
      ) : null}
      <div>{referenceLine(reference, textOnly)}</div>
    </div>
  )
}

export default function LibraryLookPicker({ kind, libraryHref, busy, onUse }) {
  const [open, setOpen] = useState(false)
  const [libraries, setLibraries] = useState([])
  const [libraryId, setLibraryId] = useState()
  const [loading, setLoading] = useState(false)
  const isScene = kind === 'SCENE'
  const isProp = kind === 'PROP'
  const noun = isProp ? '道具' : isScene ? '场景' : '角色'
  const storeLabel = isProp ? '道具库' : isScene ? '场景库' : '定妆库'
  const actionLabel = isProp ? '用这个道具' : isScene ? '用这个场景' : '用这个造型'
  const thumbWidth = isScene ? 136 : isProp ? 100 : 104
  const thumbHeight = isScene ? 90 : isProp ? 100 : 140
  const cardWidth = isScene ? 148 : isProp ? 112 : 120

  useEffect(() => {
    if (!open) return undefined
    let cancelled = false
    setLoading(true)
    productionApi.listLookLibraries().then(({ data }) => {
      if (cancelled) return
      const list = data?.result?.library_list || []
      setLibraries(list)
      const withKind = list.find((item) => groupLooksOfKind(item.material_list, kind).length > 0)
      const preferred = withKind || list.find((item) => item.library_name === '我的定妆') || list[0]
      setLibraryId((current) => {
        if (current && list.some((item) => item.library_id === current)) return current
        return preferred && preferred.library_id
      })
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [open, kind])

  const current = libraries.find((item) => item.library_id === libraryId)
  const looks = groupLooksOfKind(current?.material_list, kind)

  return (
    <div style={{ marginTop: 8 }}>
      <Space wrap>
        <Button size="small" disabled={busy} onClick={() => setOpen((value) => !value)}>
          {open ? `收起${storeLabel}` : `从${storeLabel}选`}
        </Button>
        <Link to={libraryHref}>管理{storeLabel}</Link>
      </Space>
      {open ? (
        <div style={{ marginTop: 10, padding: 12, border: '1px solid #e4e7ed', borderRadius: 8, background: '#fff' }}>
          <div style={{ marginBottom: 8 }}>
            选一个已有{isProp ? '道具参考' : isScene ? '场景参考' : '造型'}，用到这个{noun}。参考图会留在库里，后续剧集和作品也能直接复用。
          </div>
          {loading ? <Text type="secondary">正在读取{storeLabel}…</Text> : null}
          {!loading && !libraries.length ? (
            <Text type="secondary">还没有{storeLabel}内容。直接在卡片上上传照片或点「存入{storeLabel}」，会自动放进「我的定妆」分组。</Text>
          ) : null}
          {libraries.length ? (
            <Space wrap style={{ marginBottom: 10 }}>
              {libraries.map((item) => {
                const count = groupLooksOfKind(item.material_list, kind).length
                return (
                  <Button
                    key={item.library_id}
                    size="small"
                    type={item.library_id === libraryId ? 'primary' : 'default'}
                    onClick={() => setLibraryId(item.library_id)}
                  >
                    {item.library_name}（{count}）{item.status === 'ARCHIVED' ? ' · 已归档' : ''}
                  </Button>
                )
              })}
            </Space>
          ) : null}
          {current && current.status === 'ARCHIVED' ? (
            <div style={{ marginBottom: 8 }}>这个分组已归档，生成时不会自动匹配。点「{actionLabel}」仍会用到这张卡上。要恢复自动参考，去上面的「管理{storeLabel}」点恢复。</div>
          ) : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {looks.map((look) => {
              const cover = lookCover(look)
              const url = shotUrl(cover)
              return (
                <div key={look.name} style={{ width: cardWidth }}>
                  {url ? (
                    <img alt={look.name} src={url} style={{ width: thumbWidth, height: thumbHeight, objectFit: 'cover', borderRadius: 8, display: 'block' }} />
                  ) : (
                    <div style={{ width: thumbWidth, height: thumbHeight, borderRadius: 8, background: '#f6f7f9', border: '1px dashed #d9d9d9' }} />
                  )}
                  <div style={{ margin: '6px 0', fontSize: 13 }}>{look.name}</div>
                  <Button
                    size="small"
                    type="primary"
                    disabled={busy || !cover}
                    onClick={() => onUse(cover.material_id)}
                  >
                    {actionLabel}
                  </Button>
                </div>
              )
            })}
          </div>
          {!loading && current && !looks.length ? (
            <Text type="secondary">这个分组里还没有{noun}参考图。在卡片上上传参考图、锁定后点「存入{storeLabel}」，或去「管理{storeLabel}」上传即可。</Text>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
