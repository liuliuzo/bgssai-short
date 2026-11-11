import { Button, Image, Typography } from 'antd'
import { shotHasPlayableVideo } from './shotCardMedia'

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

export function ShotMediaColumn({ row, onEnlarge }) {
  const playable = shotHasPlayableVideo(row)
  return (
    <div style={{ width: playable ? 200 : 96, flexShrink: 0 }}>
      {playable ? (
        <div>
          <Text strong style={{ display: 'block', fontSize: 12, marginBottom: 4 }}>本镜视频</Text>
          <video
            src={row.video_url}
            poster={row.first_frame_url || undefined}
            controls
            playsInline
            preload="metadata"
            style={{ width: '100%', maxHeight: 320, borderRadius: 8, background: '#000', display: 'block' }}
          />
          <Button type="link" size="small" style={{ padding: 0, height: 'auto' }} onClick={() => onEnlarge && onEnlarge(row)}>
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
