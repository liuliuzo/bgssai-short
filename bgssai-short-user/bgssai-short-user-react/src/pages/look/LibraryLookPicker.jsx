import { useEffect, useState } from 'react'
import { Button, Input, Space, Typography } from 'antd'
import productionApi from '../../api/production'
import {
  buildLibraryLookOptions,
  matchSelectedLookValue,
  referenceImage,
  referenceLine,
} from './lookGroups'

const { Text } = Typography

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
  const [query, setQuery] = useState('')
  const [internalLibraries, setInternalLibraries] = useState([])
  const [internalLoading, setInternalLoading] = useState(false)
  const [libraryId, setLibraryId] = useState()
  const isScene = kind === 'SCENE'
  const isProp = kind === 'PROP'
  const noun = isProp ? '道具' : isScene ? '场景' : '人物定妆'
  const pickLabel = isProp ? '道具参考' : isScene ? '场景参考' : '人物定妆'
  const itemNoun = isProp ? '道具' : isScene ? '场景' : '人物定妆'
  const actionLabel = isProp ? '用这个道具' : isScene ? '用这个场景' : '用这个人物定妆'
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
  const selectedOption = options.find((option) => option.value === selectedValue)

  const librariesWithKind = libraries.filter((item) => options.some((option) => option.libraryId === item.library_id))
  const effectiveLibraryId = librariesWithKind.some((item) => item.library_id === libraryId)
    ? libraryId
    : (librariesWithKind.some((item) => item.library_id === selectedOption?.libraryId)
      ? selectedOption.libraryId
      : librariesWithKind[0]?.library_id)
  const current = librariesWithKind.find((item) => item.library_id === effectiveLibraryId)
  const trimmedQuery = query.trim().toLowerCase()
  const visibleOptions = trimmedQuery
    ? options.filter((option) => option.searchText.toLowerCase().includes(trimmedQuery))
    : options.filter((option) => option.libraryId === effectiveLibraryId)

  return (
    <div style={{ marginTop: 8 }}>
      <Space wrap align="center">
        <Text type="secondary" style={{ fontSize: 13 }}>{pickLabel}：</Text>
        {loading ? (
          <Text type="secondary">正在读取{pickLabel}…</Text>
        ) : selectedOption ? (
          <Text>{selectedOption.label}</Text>
        ) : (
          <Text type="secondary">{options.length ? '未从库中选择' : `暂无可选${itemNoun}`}</Text>
        )}
        {options.length > 0 ? (
          <Button size="small" disabled={busy} onClick={() => setOpen((value) => !value)}>
            {open ? '收起' : (selectedOption ? `更换${itemNoun}` : `选择${itemNoun}`)}
          </Button>
        ) : null}
      </Space>
      {open && options.length > 0 ? (
        <div style={{ marginTop: 10, padding: 12, border: '1px solid #e4e7ed', borderRadius: 8, background: '#fff' }}>
          <div style={{ marginBottom: 10 }}>
            <Input
              size="small"
              allowClear
              style={{ width: 260, maxWidth: '100%' }}
              placeholder={`搜索${itemNoun}或分组名`}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>
          {!trimmedQuery && librariesWithKind.length > 1 ? (
            <Space wrap style={{ marginBottom: 10 }}>
              {librariesWithKind.map((item) => {
                const count = options.filter((option) => option.libraryId === item.library_id).length
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
          {!trimmedQuery && current && current.status === 'ARCHIVED' ? (
            <div style={{ marginBottom: 8 }}><Text type="secondary">分组已归档</Text></div>
          ) : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {visibleOptions.map((option) => {
              const inUse = option.value === selectedValue
              const detail = [
                trimmedQuery ? option.libraryName : null,
                trimmedQuery && option.archived ? '已归档' : null,
                option.angleSummary || null,
              ].filter(Boolean).join(' · ')
              return (
                <div key={`${option.libraryId || ''}:${option.value}`} style={{ width: cardWidth }}>
                  {option.coverUrl ? (
                    <img
                      alt={option.lookName}
                      src={option.coverUrl}
                      style={{ width: thumbWidth, height: thumbHeight, objectFit: 'cover', borderRadius: 8, display: 'block' }}
                    />
                  ) : (
                    <div style={{ width: thumbWidth, height: thumbHeight, borderRadius: 8, background: '#f6f7f9', border: '1px dashed #d9d9d9' }} />
                  )}
                  <div style={{ marginTop: 6, fontSize: 13 }}>{option.lookName}</div>
                  {detail ? (
                    <div style={{ marginBottom: 6, fontSize: 12, color: '#8c8c8c' }}>{detail}</div>
                  ) : (
                    <div style={{ marginBottom: 6 }} />
                  )}
                  {inUse ? (
                    <Button size="small" disabled>使用中</Button>
                  ) : (
                    <Button
                      size="small"
                      type="primary"
                      disabled={busy}
                      onClick={() => {
                        setOpen(false)
                        onUse(option.materialId)
                      }}
                    >
                      {actionLabel}
                    </Button>
                  )}
                </div>
              )
            })}
          </div>
          {trimmedQuery && !visibleOptions.length ? (
            <Text type="secondary">没有找到「{query.trim()}」</Text>
          ) : null}
          {!trimmedQuery && current && !visibleOptions.length ? (
            <Text type="secondary">暂无{noun}参考图</Text>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
