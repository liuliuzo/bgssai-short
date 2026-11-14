import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, Card, Input, Select, Space, Statistic, Table, Tag, Typography, message } from 'antd'
import featuresApi from '../../api/features'
import { visibleBaseline } from '../../utils/baselineCards'
import { friendlyFailReason } from '../../lib/displayLabels'

const { Paragraph, Text } = Typography

const FORMAT_LABEL = {
  JIANYING: '剪映草稿',
  FCPXML: 'FCPXML',
  EDL: 'EDL',
}

const STATUS_TAG = {
  QUEUED: <Tag>排队中</Tag>,
  RUNNING: <Tag color="processing">打包中</Tag>,
  READY: <Tag color="success">已就绪</Tag>,
  FAILED: <Tag color="error">失败</Tag>,
}

function readableByte(byte) {
  if (!byte && byte !== 0) return '-'
  if (byte < 1024) return `${byte} B`
  if (byte < 1024 * 1024) return `${(byte / 1024).toFixed(1)} KB`
  if (byte < 1024 * 1024 * 1024) return `${(byte / 1024 / 1024).toFixed(1)} MB`
  return `${(byte / 1024 / 1024 / 1024).toFixed(2)} GB`
}

export default function EditExportPage({ episodeId, initialPrecheck = null }) {
  const [precheck, setPrecheck] = useState(initialPrecheck)
  const [records, setRecords] = useState([])
  const [projectFormat, setProjectFormat] = useState('JIANYING')
  const [jianyingVersion, setJianyingVersion] = useState('V6')
  const [mediaMode, setMediaMode] = useState('WITH_MEDIA')
  const [draftDir, setDraftDir] = useState('')
  const [loading, setLoading] = useState(true)
  const [settled, setSettled] = useState(false)
  const draftDirTouched = useRef(false)

  const load = useCallback(async () => {
    if (!episodeId) return
    setLoading(true)
    try {
      const [pRes, lRes] = await Promise.all([
        featuresApi.editProjectPrecheck(episodeId),
        featuresApi.editProjectList(episodeId),
      ])
      const p = pRes.data?.result
      setPrecheck(p)
      setRecords(lRes.data?.result || [])
      // 只在用户没手动改过时回填上次填写的草稿目录，避免覆盖正在输入的内容
      if (!draftDirTouched.current && p?.last_draft_dir) {
        setDraftDir(p.last_draft_dir)
      }
    } finally {
      setLoading(false)
      setSettled(true)
    }
  }, [episodeId])

  useEffect(() => { load() }, [load])

  // 打包在后台线程跑，没有推送通道，只在确有在途记录时轮询，跑完自动停下
  useEffect(() => {
    const pending = records.some((r) => r.export_status === 'QUEUED' || r.export_status === 'RUNNING')
    if (!pending) return undefined
    const timer = setInterval(load, 3000)
    return () => clearInterval(timer)
  }, [records, load])

  const submit = async () => {
    if (projectFormat === 'JIANYING' && !draftDir.trim()) {
      message.warning('剪映草稿需要填写本机剪映草稿目录')
      return
    }
    const { data } = await featuresApi.editProjectExport({
      episode_id: episodeId,
      project_format: projectFormat,
      jianying_version: jianyingVersion,
      media_mode: mediaMode,
      draft_dir: draftDir.trim(),
    })
    if (data?.success) {
      message.success('导出任务已提交，打包完成后可在下方下载')
      await load()
    }
  }

  const download = async (exportId) => {
    const { data } = await featuresApi.editProjectDetail(exportId)
    const url = data?.result?.download_url
    if (!url) {
      message.warning('下载链接不可用，请稍后重试')
      return
    }
    window.open(url, '_blank', 'noreferrer')
  }

  const exportable = precheck?.exportable_flag === 1
  const missing = precheck?.missing_shot_list || []
  const baseline = visibleBaseline(precheck)
  const timelineShots = baseline.timeline?.shot_list || []

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="支持随时导出剪辑工程草稿"
        description="只要本集至少包含一个已生成视频的分镜即可导出工程包；暂未出片的分镜将在下方清单中标注并在时间轴上自动跳过。导出工程不消耗模型额度，也不会变更成片与审核状态。"
      />

      <Card title="导出基准" size="small" style={{ marginBottom: 12 }}>
        <Paragraph type="secondary" style={{ marginTop: 0 }}>
          已锁定的角色和分镜时间轴才是这次导出对照的版本。已作废的角色版本不会标成与现行时间轴一致。
        </Paragraph>
        {!settled ? (
          <div style={{ minHeight: 72 }} />
        ) : baseline.characters.length ? baseline.characters.map((row) => (
          <Card
            key={row.character_id}
            size="small"
            style={{ marginBottom: 8 }}
            title={<span>角色版本 · {row.character_name}</span>}
            extra={<Tag color="green">已锁定</Tag>}
          >
            <Text strong>角色版本</Text>
            <div>{row.version_text}</div>
          </Card>
        )) : (
          <Paragraph type="secondary">还没有已锁定的角色版本。</Paragraph>
        )}
        {settled && baseline.timeline ? (
          <Card
            size="small"
            style={{ marginBottom: 8 }}
            title={<span>时间轴版本</span>}
            extra={<Tag color="green">已锁定</Tag>}
          >
            <Text strong>时间轴版本</Text>
            <div>{baseline.timeline.version_text}</div>
            {timelineShots.length ? (
              <Table
                style={{ marginTop: 8 }}
                rowKey={(row) => String(row.shot_no)}
                size="small"
                pagination={false}
                dataSource={timelineShots}
                columns={[
                  { title: '镜号', dataIndex: 'shot_no', width: 70 },
                  { title: '时长（秒）', dataIndex: 'shot_duration_sec', width: 100 },
                  { title: '角色', dataIndex: 'character_names' },
                  { title: '字幕', dataIndex: 'dialogue_text' },
                ]}
              />
            ) : null}
          </Card>
        ) : settled ? (
          <Paragraph type="secondary">时间轴尚未锁定，不显示时间轴版本。</Paragraph>
        ) : null}
        {baseline.voided.map((row) => (
          <Alert
            key={row.character_id || row.character_name}
            type="warning"
            showIcon
            style={{ marginBottom: 8 }}
            message="已作废角色版本"
            description={row.version_text}
          />
        ))}
      </Card>

      <Card size="small" style={{ marginBottom: 12 }}>
        <Space size={48} wrap>
          <Statistic
            title="可导镜头"
            value={!settled ? '读取中' : (precheck ? `${precheck.ready_shot_count} / ${precheck.total_shot_count}` : '尚无')}
          />
          <Statistic title="缺片镜头" value={missing.length} />
          <Statistic title="素材总量" value={readableByte(precheck?.estimated_media_byte)} />
          <Statistic title="打包上限" value={readableByte(precheck?.media_limit_byte)} />
        </Space>
      </Card>

      <Card title="导出设置" style={{ marginBottom: 12 }}>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            value={projectFormat}
            onChange={setProjectFormat}
            style={{ width: 220 }}
            options={[
              { value: 'JIANYING', label: '剪映草稿' },
              { value: 'FCPXML', label: 'FCPXML（达芬奇 / Final Cut）' },
              { value: 'EDL', label: 'EDL（Premiere / 达芬奇）' },
            ]}
          />
          {projectFormat === 'JIANYING' ? (
            <Select
              value={jianyingVersion}
              onChange={setJianyingVersion}
              style={{ width: 220 }}
              options={[
                { value: 'V6', label: '剪映 6.0 及以上' },
                { value: 'V5', label: '剪映 5.x' },
              ]}
            />
          ) : null}
          <Select
            value={mediaMode}
            onChange={setMediaMode}
            style={{ width: 260 }}
            options={[
              { value: 'WITH_MEDIA', label: '素材一并打包（解压即用）' },
              { value: 'LINK_ONLY', label: '只出工程文件（附下载清单）' },
            ]}
          />
        </Space>
        {projectFormat === 'JIANYING' ? (
          <div style={{ marginBottom: 12 }}>
            <Input
              addonBefore="本机剪映草稿目录"
              value={draftDir}
              onChange={(e) => { draftDirTouched.current = true; setDraftDir(e.target.value) }}
              placeholder="C:\Users\你的用户名\AppData\Local\JianyingPro\User Data\Projects\com.lveditor.draft"
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              剪映按草稿里写死的绝对路径找素材，这一栏必须是你本机的真实路径；填错了草稿能打开但素材全是离线的。
            </Text>
          </div>
        ) : null}
        <Space>
          <Button type="primary" disabled={!exportable} onClick={submit}>
            导出{FORMAT_LABEL[projectFormat]}
          </Button>
          <Button onClick={load} loading={loading}>刷新</Button>
          {!exportable && precheck ? <Text type="warning">本集尚无已生成的镜头视频，先去创作台补生成</Text> : null}
        </Space>
      </Card>

      {missing.length ? (
        <Card title="缺片镜头" size="small" style={{ marginBottom: 12 }}>
          <Paragraph type="secondary" style={{ fontSize: 12 }}>
            这些镜头没有可用视频，导出包里不会有它们，后续镜头依次前移。不插黑场是有意的：
            在剪辑软件里看到一段黑，你会以为是转场坏了，还得回平台查一遍。
          </Paragraph>
          <Table
            rowKey={(r) => r.shot_no}
            size="small"
            pagination={false}
            dataSource={missing}
            columns={[
              { title: '镜号', dataIndex: 'shot_no', width: 80 },
              { title: '镜头内容', dataIndex: 'shot_desc' },
            ]}
          />
        </Card>
      ) : null}

      <Card title="导出记录" size="small">
        <Table
          rowKey="export_id"
          size="small"
          loading={loading}
          dataSource={records}
          pagination={false}
          columns={[
            { title: '包名', dataIndex: 'package_name' },
            {
              title: '格式',
              width: 120,
              render: (_, r) => `${FORMAT_LABEL[r.project_format] || r.project_format}${r.jianying_version ? ` ${r.jianying_version}` : ''}`,
            },
            {
              title: '素材',
              width: 90,
              render: (_, r) => (r.media_mode === 'WITH_MEDIA' ? '含素材' : '仅工程'),
            },
            { title: '镜头', dataIndex: 'clip_count', width: 70 },
            { title: '大小', width: 90, render: (_, r) => readableByte(r.package_byte) },
            {
              title: '状态',
              width: 100,
              render: (_, r) => STATUS_TAG[r.export_status] || <Tag>{r.export_status}</Tag>,
            },
            {
              title: '操作',
              width: 220,
              render: (_, r) => (r.export_status === 'READY'
                ? <Button type="link" size="small" onClick={() => download(r.export_id)}>下载</Button>
                : <Text type="secondary" style={{ fontSize: 12 }}>{friendlyFailReason(r.fail_reason) || '-'}</Text>),
            },
          ]}
        />
        <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 10, marginBottom: 0 }}>
          下载链接 30 分钟有效，过期再点一次「下载」即可重新取链接，不用重新导出。
          每集每格式保留最近 10 条记录。
          {projectFormat === 'JIANYING'
            ? ' 剪映导入：把包内文件夹整个解压到上面填的草稿目录下（不要多套一层），重启剪映即可在草稿列表看到。'
            : ''}
        </Paragraph>
      </Card>
    </div>
  )
}
