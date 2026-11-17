import { useEffect, useState } from 'react'
import { Button, Select, Space, Typography } from 'antd'
import productionApi from '../../api/production'
import {
  buildLibraryLookOptions,
  groupLooksOfKind,
  lookCover,
  matchSelectedLookValue,
  referenceImage,
  referenceLine,
} from './lookGroups'

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

export default function LibraryLookPicker({
  kind,
  libraries: externalLibraries,
  librariesLoading = false,
  currentReference,
  busy,
  onUse,
}) {
  const [open, setOpen] = useState(false)
  const [internalLibraries, setInternalLibraries] = useState([])
  const [internalLoading, setInternalLoading] = useState(false)
  const [libraryId, setLibraryId] = useState()
  const isScene = kind === 'SCENE'
  const isProp = kind === 'PROP'
  const noun = isProp ? '道具' : isScene ? '场景' : '角色'
  const pickLabel = isProp ? '道具参考' : isScene ? '场景参考' : '人物定妆'
  const actionLabel = isProp ? '用这个道具' : isScene ? '用这个场景' : '用这个造型'
  const optThumbWidth = isScene ? 52 : isProp ? 40 : 36
  const optThumbHeight = isScene ? 36 : isProp ? 40 : 48
  const thumbWidth = isScene ? 136 : isProp ? 100 : 104
  const thumbHeight = isScene ? 90 : isProp ? 100 : 140
  const cardWidth = isScene ? 148 : isProp ? 112 : 120

  useEffect(() => {
    if (externalLibraries !== undefined) return undefined
    let cancelled = false
    setInternalLoading(true)
    productionApi.listLookLibraries().then(({ data }) => {
      if (cancelled) return
      setInternalLibraries(data?.result?.library_list || [])
    }).finally(() => {
      if (!cancelled) setInternalLoading(false)
    })
    return () => { cancelled = true }
  }, [externalLibraries])

  const libraries = externalLibraries !== undefined
    ? (Array.isArray(externalLibraries) ? externalLibraries : [])
    : internalLibraries
  const loading = externalLibraries !== undefined ? !!librariesLoading : internalLoading
  const options = buildLibraryLookOptions(libraries, kind)
  const selectedValue = matchSelectedLookValue(options, currentReference)

  const librariesWithKind = libraries.filter((item) => groupLooksOfKind(item.material_list, kind).length > 0)
  const effectiveLibraryId = librariesWithKind.some((item) => item.library_id === libraryId)
    ? libraryId
    : librariesWithKind[0]?.library_id
  const current = librariesWithKind.find((item) => item.library_id === effectiveLibraryId)
  const looks = groupLooksOfKind(current?.material_list, kind)

  return (
    <div style={{ marginTop: 8 }}>
      <Space wrap align="center">
        <Text type="secondary" style={{ fontSize: 13 }}>{pickLabel}：</Text>
        <Select
          aria-label={pickLabel}
          style={{ minWidth: 240, maxWidth: 340 }}
          value={selectedValue}
          placeholder={loading ? `正在读取${pickLabel}…` : (options.length ? `直接选择${pickLabel}` : `暂无可选${pickLabel}`)}
          loading={loading}
          disabled={busy || loading || !options.length}
          showSearch
          filterOption={(input, option) => !input || String(option?.searchText || '').toLowerCase().includes(input.trim().toLowerCase())}
          popupMatchSelectWidth={false}
          dropdownStyle={{ minWidth: 280 }}
          listHeight={320}
          options={options}
          onSelect={(materialId) => {
            setOpen(false)
            onUse(materialId)
          }}
          optionRender={(oriOption) => {
            const d = oriOption.data
            return (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '2px 0' }}>
                {d.coverUrl ? (
                  <img
                    alt={d.lookName}
                    src={d.coverUrl}
                    style={{
                      width: optThumbWidth,
                      height: optThumbHeight,
                      objectFit: 'cover',
                      borderRadius: 4,
                      flexShrink: 0,
                      display: 'block',
                    }}
                  />
                ) : (
                  <div
                    style={{
                      width: optThumbWidth,
                      height: optThumbHeight,
                      borderRadius: 4,
                      background: '#f6f7f9',
                      border: '1px dashed #d9d9d9',
                      flexShrink: 0,
                    }}
                  />
                )}
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ fontSize: 13, fontWeight: 500, color: '#1f1f1f', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {d.lookName}
                  </div>
                  <div style={{ fontSize: 12, color: '#8c8c8c', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {d.libraryName}{d.archived ? ' · 已归档' : ''}{d.angleSummary ? ` · ${d.angleSummary}` : ''}
                  </div>
                </div>
              </div>
            )
          }}
        />
        {options.length > 0 ? (
          <Button size="small" disabled={busy} onClick={() => setOpen((value) => !value)}>
            {open ? '收起大图' : '大图点选'}
          </Button>
        ) : null}
      </Space>
      {open && options.length > 0 ? (
        <div style={{ marginTop: 10, padding: 12, border: '1px solid #e4e7ed', borderRadius: 8, background: '#fff' }}>
          {librariesWithKind.length > 1 ? (
            <Space wrap style={{ marginBottom: 10 }}>
              {librariesWithKind.map((item) => {
                const count = groupLooksOfKind(item.material_list, kind).length
                return (
                  <Button
                    key={item.library_id}
                    size="small"
                    type={item.library_id === effectiveLibraryId ? 'primary' : 'default'}
                    onClick={() => setLibraryId(item.library_id)}
                  >
                    {item.library_name}（{count}）{item.status === 'ARCHIVED' ? ' · 已归档' : ''}
                  </Button>
                )
              })}
            </Space>
          ) : null}
          {current && current.status === 'ARCHIVED' ? (
            <div style={{ marginBottom: 8 }}><Text type="secondary">分组已归档</Text></div>
          ) : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {looks.map((look) => {
              const cover = lookCover(look)
              const url = shotUrl(cover)
              return (
                <div key={look.name} style={{ width: cardWidth }}>
                  {url ? (
                    <img
                      alt={look.name}
                      src={url}
                      onClick={() => {
                        if (!busy && cover) {
                          setOpen(false)
                          onUse(cover.material_id)
                        }
                      }}
                      style={{
                        width: thumbWidth,
                        height: thumbHeight,
                        objectFit: 'cover',
                        borderRadius: 8,
                        display: 'block',
                        cursor: busy ? 'not-allowed' : 'pointer',
                      }}
                    />
                  ) : (
                    <div style={{ width: thumbWidth, height: thumbHeight, borderRadius: 8, background: '#f6f7f9', border: '1px dashed #d9d9d9' }} />
                  )}
                  <div style={{ margin: '6px 0', fontSize: 13 }}>{look.name}</div>
                  <Button
                    size="small"
                    type="primary"
                    disabled={busy || !cover}
                    onClick={() => {
                      setOpen(false)
                      onUse(cover.material_id)
                    }}
                  >
                    {actionLabel}
                  </Button>
                </div>
              )
            })}
          </div>
          {!loading && current && !looks.length ? (
            <Text type="secondary">暂无{noun}参考图</Text>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
