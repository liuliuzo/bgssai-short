import { useState, useRef } from 'react'
import { Button, Image, Typography } from 'antd'
import { classifyVideoDuration, shotHasPlayableVideo } from './shotCardMedia'
import { isPlaceholderMediaUrl } from '../../lib/mediaUrl'
import {
  FIRST_FRAME_CAPTION,
  LAST_FRAME_CAPTION,
  LIST_FIRST_FRAME_THUMB_WIDTH,
  SHOT_VIDEO_DIALOG_Z_INDEX,
  enlargePlaybackButtonProps,
  frameCaptionStyle,
  listPlayerFrameStyle,
  listPlayerToggleStyle,
  listPlayerVideoStyle,
  playbackAction,
  thumbPlaceholderStyle,
} from './studioListUx'

const { Text } = Typography

export function ClickThumb({ src, alt, width, height, caption }) {
  const [open, setOpen] = useState(false)
  if (!src) return null
  const placeholder = thumbPlaceholderStyle()
  return (
    <div style={{ width }}>
      <Image
        src={src}
        alt={alt || ''}
        width={width}
        height={height}
        style={{
          objectFit: 'cover',
          borderRadius: placeholder.borderRadius,
          cursor: 'zoom-in',
          background: placeholder.background,
          display: 'block',
        }}
        placeholder={<div style={{ ...placeholder, width: width || '100%', height: height || width || 72 }} />}
        preview={{
          visible: open,
          onVisibleChange: setOpen,
          mask: '查看',
          zIndex: SHOT_VIDEO_DIALOG_Z_INDEX,
        }}
      />
      {caption ? (
        <button
          type="button"
          onClick={() => setOpen(true)}
          style={frameCaptionStyle()}
        >
          {caption}
        </button>
      ) : null}
    </div>
  )
}

export function ListShotVideo({ src, poster, onDuration, onPlayback, label, autoPlay = false }) {
  const ref = useRef(null)
  const [paused, setPaused] = useState(!autoPlay)
  const [verdict, setVerdict] = useState('pending')
  const action = playbackAction(paused)
  const report = (duration, extra) => {
    const next = classifyVideoDuration(duration, extra)
    setVerdict(next)
    if (onPlayback) onPlayback(next)
    if (next === 'ready' && onDuration) onDuration(duration)
  }
  if (verdict === 'broken') {
    return (
      <div style={listPlayerFrameStyle()} data-shot-player="broken">
        <Text>这段视频打不开（没有可播放的时长）。请重新生成这一镜。</Text>
      </div>
    )
  }
  return (
    <div style={listPlayerFrameStyle()} data-shot-player="list">
      <video
        ref={ref}
        src={src}
        poster={poster || undefined}
        controls
        playsInline
        preload="metadata"
        autoPlay={autoPlay}
        style={listPlayerVideoStyle()}
        onLoadedMetadata={(event) => report(event.currentTarget.duration, { hasFrame: false })}
        onLoadedData={(event) => report(event.currentTarget.duration, { hasFrame: true })}
        onError={() => report(Number.NaN, { errored: true })}
        onPlay={() => setPaused(false)}
        onPause={() => setPaused(true)}
      />
      <button
        type="button"
        aria-label={label || (action === 'play' ? '播放' : '暂停')}
        data-shot-player-toggle="true"
        style={listPlayerToggleStyle()}
        onClick={() => {
          const video = ref.current
          if (!video) return
          if (playbackAction(video.paused) === 'play') video.play().catch(() => {})
          else video.pause()
        }}
      />
      {verdict === 'pending' ? <Text type="secondary">正在读取视频时长</Text> : <Text type="secondary">这段视频可以播放。</Text>}
    </div>
  )
}

export function ShotMediaColumn({ row, onEnlarge, onDuration, onPlayback }) {
  const placeholder = isPlaceholderMediaUrl(row && row.video_url)
  const playable = shotHasPlayableVideo(row)
  const enlarge = enlargePlaybackButtonProps()
  if (placeholder) {
    return (
      <div style={{ width: 160, flexShrink: 0 }}>
        <Text strong style={{ display: 'block', fontSize: 12, marginBottom: 4 }}>占位图</Text>
        <ClickThumb alt="占位图" src={row.video_url} width={96} height={160} />
        <Text type="secondary" style={{ fontSize: 12 }}>这是占位图，不是能播放的视频。</Text>
      </div>
    )
  }
  return (
    <div style={{ width: playable ? 200 : 96, flexShrink: 0 }}>
      {playable ? (
        <div>
          <Text strong style={{ display: 'block', fontSize: 12, marginBottom: 4 }}>本镜视频</Text>
          <ListShotVideo
            src={row.video_url}
            onDuration={onDuration}
            onPlayback={onPlayback}
          />
          <Button
            {...enlarge}
            style={{ marginTop: 8 }}
            aria-label="放大播放"
            onClick={() => onEnlarge && onEnlarge(row)}
          >
            放大播放
          </Button>
          {row.first_frame_url ? (
            <div style={{ marginTop: 6 }}>
              <ClickThumb alt="首帧" src={row.first_frame_url} width={LIST_FIRST_FRAME_THUMB_WIDTH} caption={FIRST_FRAME_CAPTION} />
            </div>
          ) : null}
        </div>
      ) : row.first_frame_url ? (
        <div>
          <ClickThumb alt="首帧" src={row.first_frame_url} width={90} />
          {row.video_asset_id ? (
            <Text type="secondary" style={{ fontSize: 11 }}>已绑定视频，播放地址暂时不可用</Text>
          ) : null}
        </div>
      ) : (
        <div style={{ width: 90, height: 160, background: '#f1f5f9', borderRadius: 8, padding: 8, fontSize: 12, color: '#64748b' }}>
          {row.video_asset_id ? '已绑定视频，播放地址暂时不可用' : '还没出图'}
        </div>
      )}
      {row.last_frame_url ? (
        <div style={{ marginTop: 6 }}>
          <ClickThumb alt="尾帧" src={row.last_frame_url} width={90} caption={LAST_FRAME_CAPTION} />
        </div>
      ) : null}
    </div>
  )
}
