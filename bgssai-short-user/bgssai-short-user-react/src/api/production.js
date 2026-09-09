import http from '../utils/http'

const listWorks = (params) => http.get('/user/short-works/list', params)
const createWork = (body) => http.post('/user/short-works/create', body)
const getWork = (workId) => http.get('/user/short-works/detail', { work_id: workId })
const updateWork = (body) => http.post('/user/short-works/update', body)
const archiveWork = (workId) => http.post('/user/short-works/archive', { work_id: workId, archived_flag: 1 })

const getOutline = (workId) => http.get('/user/scripts/outline', { work_id: workId })
const generateOutline = (workId, modelCode) =>
  http.post('/user/scripts/outline-generate', { work_id: workId, model_code: modelCode })
const saveOutline = (body) => http.post('/user/scripts/outline-save', body)
const generateScript = (episodeId, extraPrompt, modelCode) =>
  http.post('/user/scripts/generate', { episode_id: episodeId, extra_prompt: extraPrompt, model_code: modelCode })
const getScript = (episodeId) => http.get('/user/scripts/detail', { episode_id: episodeId })
const saveScript = (body) => http.post('/user/scripts/save', body)
const readyScript = (episodeId, forceReadyFlag) =>
  http.post('/user/scripts/ready', { episode_id: episodeId, force_ready_flag: forceReadyFlag })

const listCharacters = (workId) => http.get('/user/characters/list', { work_id: workId })
const createCharacter = (body) => http.post('/user/characters/create', body)
const portraitCharacter = (characterId, modelCode) =>
  http.post('/user/characters/portrait-generate', { character_id: characterId, model_code: modelCode })
const lockCharacter = (characterId, assetId) =>
  http.post('/user/characters/lock', { character_id: characterId, asset_id: assetId })
const listScenes = (workId) => http.get('/user/scene-cards/list', { work_id: workId })
const createScene = (body) => http.post('/user/scene-cards/create', body)
const portraitScene = (sceneCardId, modelCode) =>
  http.post('/user/scene-cards/portrait-generate', { scene_card_id: sceneCardId, model_code: modelCode })
const lockScene = (sceneCardId, assetId) =>
  http.post('/user/scene-cards/lock', { scene_card_id: sceneCardId, asset_id: assetId })

const splitShots = (episodeId) => http.post('/user/shots/split-from-script', { episode_id: episodeId })
const listShots = (params) => http.get('/user/shots/list', params)
const lockShot = (shotId) => http.post('/user/shots/lock', { shot_id: shotId })
const unlockShot = (shotId) => http.post('/user/shots/unlock', { shot_id: shotId })
const updateShot = (body) => http.post('/user/shots/update', body)
const reorderShots = (body) => http.post('/user/shots/reorder', body)

const listAiModels = (abilityType) => http.get('/user/ai-models/list', { ability_type: abilityType })
const estimateTasks = (body) => http.post('/user/ai-tasks/estimate', body)
const submitTasks = (body) => http.post('/user/ai-tasks/submit', body)
const listTasks = (params) => http.get('/user/ai-tasks/list', params)
const batchProgress = (batchId) => http.get('/user/ai-tasks/batch-progress', { batch_id: batchId })
const retryTask = (taskId) => http.post('/user/ai-tasks/retry', { task_id: taskId })
const cancelTask = (taskId) => http.post('/user/ai-tasks/cancel', { task_id: taskId })

const filmPrecheck = (episodeId) => http.get('/user/films/precheck', { episode_id: episodeId })
const filmDetail = (episodeId) => http.get('/user/films/detail', { episode_id: episodeId })
const composeEstimate = (episodeId) => http.post('/user/films/compose-estimate', { episode_id: episodeId })
const composeFilm = (episodeId) => http.post('/user/films/compose', { episode_id: episodeId })
const submitReview = (episodeId) => http.post('/user/reviews/submit', { episode_id: episodeId })
const withdrawReview = (episodeId) => http.post('/user/reviews/withdraw', { episode_id: episodeId })
const listReviews = (workId) => http.get('/user/reviews/list', { work_id: workId })
const precheck = (body) => http.post('/user/compliance/precheck', body)

export default {
  listWorks,
  createWork,
  getWork,
  updateWork,
  archiveWork,
  getOutline,
  generateOutline,
  saveOutline,
  generateScript,
  getScript,
  saveScript,
  readyScript,
  listCharacters,
  createCharacter,
  portraitCharacter,
  lockCharacter,
  listScenes,
  createScene,
  portraitScene,
  lockScene,
  splitShots,
  listShots,
  lockShot,
  unlockShot,
  updateShot,
  reorderShots,
  listAiModels,
  estimateTasks,
  submitTasks,
  listTasks,
  batchProgress,
  retryTask,
  cancelTask,
  filmPrecheck,
  filmDetail,
  composeEstimate,
  composeFilm,
  submitReview,
  withdrawReview,
  listReviews,
  precheck,
}
