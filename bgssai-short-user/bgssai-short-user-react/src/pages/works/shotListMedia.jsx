import { useRef, useState } from 'react'
import { Button, Image, Typography } from 'antd'
import { shotHasPlayableVideo } from './shotCardMedia'
import {
  enlargePlaybackButtonProps,
  listPlayerFrameStyle,
  listPlayerToggleStyle,
  listPlayerVideoStyle,
  playbackAction,
} from './studioListUx'

const { Text } = Typography

export function ClickThumb({ src, alt, width, height }) {
  if (!src) return null
  return (
    <Image
      src={src}
      alt={alt || ''}
      width={width}
      height={height}
      style={{ objectFit: 'cover', borderRadius: 8, cursor: 'zoom-in', background: '#111', display: 'block' }}
      preview={{ mask: '查看' }}
    />
  )
}

export function ListShotVideo({ src, poster, onDuration, label, autoPlay = false }) {
  const ref = useRef(null)
  const [paused, setPaused] = useState(true)
  const action = playbackAction(paused)
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
        onLoadedMetadata={(event) => {
          const duration = event.currentTarget.duration
          if (onDuration && Number.isFinite(duration) && duration > 0) onDuration(duration)
        }}
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
    </div>
  )
}

export function ShotMediaColumn({ row, onEnlarge, onDuration }) {
  const playable = shotHasPlayableVideo(row)
  const enlarge = enlargePlaybackButtonProps()
  return (
    <div style={{ width: playable ? 200 : 96, flexShrink: 0 }}>
      {playable ? (
        <div>
          <Text strong style={{ display: 'block', fontSize: 12, marginBottom: 4 }}>本镜视频</Text>
          <ListShotVideo
            src={row.video_url}
            poster={row.first_frame_url}
            onDuration={onDuration}
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
              <ClickThumb alt="首帧" src={row.first_frame_url} width={72} />
              <Text type="secondary" style={{ fontSize: 11 }}>首帧，点击查看</Text>
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
          <ClickThumb alt="尾帧" src={row.last_frame_url} width={90} />
          <Text type="secondary" style={{ fontSize: 11 }}>尾帧，点击查看</Text>
        </div>
      ) : null}
    </div>
  )
}
