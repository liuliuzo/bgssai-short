import http from '../utils/http'

const submitDubbing = (body) => http.post('/user/dubbings/submit', body)
const listDubbings = (episodeId) => http.get('/user/dubbings/list', { episode_id: episodeId })
const regenerateDubbing = (body) => http.post('/user/dubbings/regenerate', body)
const alignDubbing = (body) => http.post('/user/dubbings/align', body)
const alignBatchDubbing = (episodeId) => http.post('/user/dubbings/align-batch', { episode_id: episodeId })
const applyVoice = (body) => http.post('/user/dubbings/apply-voice-to-character', body)
const listVoices = () => http.get('/user/voices/list')
const subtitleDetail = (episodeId) => http.get('/user/subtitles/detail', { episode_id: episodeId })
const subtitleRegen = (episodeId) => http.post('/user/subtitles/regenerate', { episode_id: episodeId })
const subtitleTranscribe = (body) => http.post('/user/subtitles/transcribe', body)
const subtitleUpdate = (body) => http.post('/user/subtitles/update', body)
const subtitleStyleSave = (body) => http.post('/user/subtitles/style-save', body)

const previzList = (episodeId) => http.get('/user/previz/list', { episode_id: episodeId })
const previzGenerateText = (body) => http.post('/user/previz/generate-text', body)
const previzGenerateDraft = (body) => http.post('/user/previz/generate-draft', body)
const previzSave = (body) => http.post('/user/previz/save', body)
const previzPromptCheck = (body) => http.post('/user/previz/prompt-check', body)
const previzPromptDiff = (body) => http.post('/user/previz/prompt-diff', body)

const storyMapGenerate = (body) => http.post('/user/story-map/generate', body)
const storyMapTension = (episodeId) => http.get('/user/story-map/tension', { episode_id: episodeId })
const storyMapTensionBand = (workId) => http.get('/user/story-map/tension-band', { work_id: workId })
const storyMapRelations = (workId, asOfEpisodeNo) =>
  http.get('/user/story-map/relations', { work_id: workId, as_of_episode_no: asOfEpisodeNo })
const storyMapBlocking = (shotId) => http.get('/user/story-map/blocking', { shot_id: shotId })
const storyMapRelationSave = (body) => http.post('/user/story-map/relation-save', body)
const storyMapBlockingSave = (body) => http.post('/user/story-map/blocking-save', body)
const storyMapSceneBand = (episodeId) => http.get('/user/story-map/scene-band', { episode_id: episodeId })
const storyMapAppearanceHeat = (workId) => http.get('/user/story-map/appearance-heat', { work_id: workId })

const qualityGateRun = (body) => http.post('/user/quality/gate-run', body)
const qualityForcePass = (body) => http.post('/user/quality/force-pass', body)
const qualityIssueHandle = (body) => http.post('/user/quality/issue-handle', body)
const qualityThresholdAdjust = (body) => http.post('/user/quality/threshold-adjust', body)
const qualityVariantsGenerate = (body) => http.post('/user/quality/variants-generate', body)
const qualityVariantSelect = (body) => http.post('/user/quality/variant-select', body)
const qualityGateResult = (gateCode, episodeId) =>
  http.get('/user/quality/gate-result', { gate_code: gateCode, episode_id: episodeId })
const qualityThresholdLogs = (workId) => http.get('/user/quality/threshold-logs', { work_id: workId })
const qualityVariants = (params) => http.get('/user/quality/variants', params)
const qualityProfile = (workId) => http.get('/user/quality/profile', { work_id: workId })
const qualityTemplates = (genre) => http.get('/user/quality/templates', { genre })

const previewSummary = (episodeId) => http.get('/user/preview/summary', { episode_id: episodeId })
const continuityOverview = (workId) => http.get('/user/continuity/overview', { work_id: workId })
const continuityForeshadows = (workId) => http.get('/user/continuity/foreshadows', { work_id: workId })
const continuityForeshadowSave = (body) => http.post('/user/continuity/foreshadow-save', body)
const continuityRecheck = (body) => http.post('/user/continuity/recheck', body)

const filmRebakeSubtitle = (episodeId) => http.post('/user/films/rebake-subtitle', { episode_id: episodeId })
const filmSettingsSave = (body) => http.post('/user/films/settings-save', body)
const filmBgmGenerate = (body) => http.post('/user/films/bgm-generate', body)
const filmExport = (body) => http.post('/user/films/export', body)

export default {
  submitDubbing,
  listDubbings,
  regenerateDubbing,
  alignDubbing,
  alignBatchDubbing,
  applyVoice,
  listVoices,
  subtitleDetail,
  subtitleRegen,
  subtitleTranscribe,
  subtitleUpdate,
  subtitleStyleSave,
  previzList,
  previzGenerateText,
  previzGenerateDraft,
  previzSave,
  previzPromptCheck,
  previzPromptDiff,
  storyMapGenerate,
  storyMapTension,
  storyMapTensionBand,
  storyMapRelations,
  storyMapBlocking,
  storyMapRelationSave,
  storyMapBlockingSave,
  storyMapSceneBand,
  storyMapAppearanceHeat,
  qualityGateRun,
  qualityForcePass,
  qualityIssueHandle,
  qualityThresholdAdjust,
  qualityVariantsGenerate,
  qualityVariantSelect,
  qualityGateResult,
  qualityThresholdLogs,
  qualityVariants,
  qualityProfile,
  qualityTemplates,
  previewSummary,
  continuityOverview,
  continuityForeshadows,
  continuityForeshadowSave,
  continuityRecheck,
  filmRebakeSubtitle,
  filmSettingsSave,
  filmBgmGenerate,
  filmExport,
}
