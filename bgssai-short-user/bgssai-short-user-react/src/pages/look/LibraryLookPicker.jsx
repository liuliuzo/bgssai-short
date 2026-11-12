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
  const noun = kind === 'PROP' ? '道具' : kind === 'SCENE' ? '场景' : '角色'

  useEffect(() => {
    if (!open) return undefined
    let cancelled = false
    setLoading(true)
    productionApi.listLookLibraries().then(({ data }) => {
      if (cancelled) return
      const list = data?.result?.library_list || []
      setLibraries(list)
      const preferred = list.find((item) => item.library_name === '我的定妆') || list[0]
      setLibraryId((current) => {
        if (current && list.some((item) => item.library_id === current)) return current
        return preferred && preferred.library_id
      })
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [open])

  const current = libraries.find((item) => item.library_id === libraryId)
  const looks = groupLooksOfKind(current?.material_list, kind)

  return (
    <div style={{ marginTop: 8 }}>
      <Space wrap>
        <Button size="small" disabled={busy} onClick={() => setOpen((value) => !value)}>
          {open ? '收起知识库' : '从知识库选'}
        </Button>
        <Link to={libraryHref}>管理知识库</Link>
      </Space>
      {open ? (
        <div style={{ marginTop: 10, padding: 12, border: '1px solid #e4e7ed', borderRadius: 8, background: '#fff' }}>
          <div style={{ marginBottom: 8 }}>选一个已有造型，用到这个{noun}。照片会留在知识库里，以后别的角色也能用。</div>
          {loading ? <Text type="secondary">正在读取知识库…</Text> : null}
          {!loading && !libraries.length ? (
            <Text type="secondary">还没有知识库。直接在上面上传照片即可，会自动放进「我的定妆」。</Text>
          ) : null}
          {libraries.length ? (
            <Space wrap style={{ marginBottom: 10 }}>
              {libraries.map((item) => (
                <Button
                  key={item.library_id}
                  size="small"
                  type={item.library_id === libraryId ? 'primary' : 'default'}
                  onClick={() => setLibraryId(item.library_id)}
                >
                  {item.library_name}{item.status === 'ARCHIVED' ? '（已归档）' : ''}
                </Button>
              ))}
            </Space>
          ) : null}
          {current && current.status === 'ARCHIVED' ? (
            <div style={{ marginBottom: 8 }}>这个库已归档，生成时不会自动用。点「用这个造型」仍会用到这张卡上。要恢复自动参考，去上面的「管理知识库」点恢复。</div>
          ) : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {looks.map((look) => {
              const cover = lookCover(look)
              const url = shotUrl(cover)
              return (
                <div key={look.name} style={{ width: 120 }}>
                  {url ? (
                    <img alt={look.name} src={url} style={{ width: 104, height: 140, objectFit: 'cover', borderRadius: 8, display: 'block' }} />
                  ) : (
                    <div style={{ width: 104, height: 140, borderRadius: 8, background: '#f6f7f9', border: '1px dashed #d9d9d9' }} />
                  )}
                  <div style={{ margin: '6px 0', fontSize: 13 }}>{look.name}</div>
                  <Button
                    size="small"
                    type="primary"
                    disabled={busy || !cover}
                    onClick={() => onUse(cover.material_id)}
                  >
                    用这个造型
                  </Button>
                </div>
              )
            })}
          </div>
          {!loading && current && !looks.length ? (
            <Text type="secondary">这个库里还没有{noun}照片。上传后会出现在这里，不用先离开作品。</Text>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
