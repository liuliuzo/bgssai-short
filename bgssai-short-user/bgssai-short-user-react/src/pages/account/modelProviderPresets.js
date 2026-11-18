export const FALLBACK_PRESETS = [
  {
    provider_code: 'MINIMAX',
    provider_name: 'MiniMax (海螺)',
    auth_type: 'BEARER',
    default_base_url: 'https://api.minimaxi.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'MiniMax-M2.5', model_name: 'MiniMax M2.5', recommended: true },
          { model_code: 'MiniMax-M2.5-highspeed', model_name: 'MiniMax M2.5 Highspeed', recommended: false },
          { model_code: 'abab6.5s-chat', model_name: 'MiniMax abab6.5s', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'image-01', model_name: 'MiniMax Image-01', recommended: true },
          { model_code: 'image-01-live', model_name: 'MiniMax Image-01 Live', recommended: false },
        ],
      },
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'MiniMax-Hailuo-02', model_name: 'MiniMax Hailuo 02', recommended: true },
          { model_code: 'MiniMax-Hailuo-2.3', model_name: 'MiniMax Hailuo 2.3', recommended: false },
          { model_code: 'T2V-01-Director', model_name: 'MiniMax T2V-01 Director', recommended: false },
          { model_code: 'I2V-01-Director', model_name: 'MiniMax I2V-01 Director', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'speech-2.8-hd', model_name: 'MiniMax Speech 2.8 HD', recommended: true },
          { model_code: 'speech-2.8-turbo', model_name: 'MiniMax Speech 2.8 Turbo', recommended: false },
          { model_code: 'speech-02-hd', model_name: 'MiniMax Speech 02 HD', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'asr-1.0', model_name: 'MiniMax ASR 1.0', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'MUSIC',
        ability_label: '音频模型（背景音乐）',
        models: [
          { model_code: 'music-2.5', model_name: 'MiniMax Music 2.5', recommended: true },
          { model_code: 'music-2.0', model_name: 'MiniMax Music 2.0', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'KLING',
    provider_name: '可灵 (Kling AI)',
    auth_type: 'AK_SK',
    default_base_url: 'https://api-beijing.klingai.com',
    abilities: [
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'kling-v3', model_name: 'Kling V3', recommended: true },
          { model_code: 'kling-v2-6', model_name: 'Kling V2.6', recommended: false },
          { model_code: 'kling-v2-5-turbo', model_name: 'Kling V2.5 Turbo', recommended: false },
          { model_code: 'kling-v2-1-master', model_name: 'Kling V2.1 Master', recommended: false },
          { model_code: 'kling-v1-6', model_name: 'Kling V1.6', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'kling-v3', model_name: 'Kling Image V3', recommended: true },
          { model_code: 'kling-v2-1', model_name: 'Kling Image V2.1', recommended: false },
          { model_code: 'kling-v1-5', model_name: 'Kling Image V1.5', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'kling-tts', model_name: 'Kling TTS', recommended: true },
        ],
      },
    ],
  },
  {
    provider_code: 'PIXVERSE',
    provider_name: 'PixVerse',
    auth_type: 'API_KEY_HEADER',
    default_base_url: 'https://app-api.pixverse.ai',
    abilities: [
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'v6', model_name: 'PixVerse v6', recommended: true },
          { model_code: 'v6-max', model_name: 'PixVerse v6 Max', recommended: false },
          { model_code: 'v5.6', model_name: 'PixVerse v5.6', recommended: false },
          { model_code: 'v5', model_name: 'PixVerse v5', recommended: false },
          { model_code: 'v4.5', model_name: 'PixVerse v4.5', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'DEEPSEEK',
    provider_name: 'DeepSeek',
    auth_type: 'BEARER',
    default_base_url: 'https://api.deepseek.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'deepseek-chat', model_name: 'DeepSeek Chat', recommended: true },
          { model_code: 'deepseek-reasoner', model_name: 'DeepSeek Reasoner', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'XAI',
    provider_name: 'Grok (xAI)',
    auth_type: 'BEARER',
    default_base_url: 'https://api.x.ai/v1',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'grok-4.7', model_name: 'Grok 4.7', recommended: true },
          { model_code: 'grok-4', model_name: 'Grok 4', recommended: false },
          { model_code: 'grok-3', model_name: 'Grok 3', recommended: false },
          { model_code: 'grok-3-mini', model_name: 'Grok 3 Mini', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'grok-imagine-image-2.0', model_name: 'Grok Imagine Image 2.0', recommended: true },
          { model_code: 'grok-2-image', model_name: 'Grok 2 Image', recommended: false },
        ],
      },
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'grok-imagine-video-1.5', model_name: 'Grok Imagine Video 1.5', recommended: true },
          { model_code: 'grok-imagine-video-1.5-lite', model_name: 'Grok Imagine Video 1.5 Lite', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'grok-tts', model_name: 'Grok TTS', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'grok-voice-transcribe-2.0', model_name: 'Grok Voice Transcribe 2.0', recommended: true },
        ],
      },
    ],
  },
  {
    provider_code: 'OPENAI',
    provider_name: 'OpenAI',
    auth_type: 'BEARER',
    default_base_url: 'https://api.openai.com/v1',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'gpt-6-luna', model_name: 'GPT-6 Luna', recommended: true },
          { model_code: 'gpt-5.4', model_name: 'GPT-5.4', recommended: false },
          { model_code: 'gpt-4o', model_name: 'GPT-4o', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'gpt-image-2.6-sunburst', model_name: 'GPT Image 2.6 Sunburst', recommended: true },
          { model_code: 'gpt-image-2.6-flare', model_name: 'GPT Image 2.6 Flare', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'gpt-transcribe', model_name: 'GPT Transcribe', recommended: true },
          { model_code: 'whisper-1', model_name: 'Whisper', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'ANTHROPIC',
    provider_name: 'Claude',
    auth_type: 'BEARER',
    default_base_url: 'https://api.anthropic.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'claude-opus-5-5', model_name: 'Claude Opus 5.5', recommended: true },
          { model_code: 'claude-sonnet-5-5', model_name: 'Claude Sonnet 5.5', recommended: false },
          { model_code: 'claude-haiku-5-5', model_name: 'Claude Haiku 5.5', recommended: false },
        ],
      },
    ],
  },
  {
    provider_code: 'BGSSAI_TOKENHUB',
    provider_name: 'BGSSAI TokenHub',
    auth_type: 'BEARER',
    default_base_url: 'https://www.bgssai-tokenhub.cn',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'deepseek-chat', model_name: 'DeepSeek Chat', recommended: true },
          { model_code: 'hy4-preview', model_name: 'Hy4 preview', recommended: false },
          { model_code: 'kimi-k3', model_name: 'Kimi K3', recommended: false },
        ],
      },
      {
        modality_type: 'IMAGE',
        ability_type: 'IMAGE',
        ability_label: '图片模型',
        models: [
          { model_code: 'kling-v3', model_name: '可灵文生图', recommended: true },
        ],
      },
      {
        modality_type: 'VIDEO',
        ability_type: 'VIDEO',
        ability_label: '视频模型',
        models: [
          { model_code: 'minimax-video-h3', model_name: 'MiniMax Video H3', recommended: true },
          { model_code: 'kling-video-v3', model_name: 'Kling Video V3', recommended: false },
          { model_code: 'pixverse-video-v6.0', model_name: 'PixVerse Video v6', recommended: false },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'VOICE',
        ability_label: '音频模型（配音合成）',
        models: [
          { model_code: 'minimax-speech-2.8-hd', model_name: 'MiniMax Speech 2.8 HD', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'ASR',
        ability_label: '音频模型（字幕识别）',
        models: [
          { model_code: 'hy-asr-3.0-preview', model_name: 'Hy ASR 3.0 Preview', recommended: true },
        ],
      },
      {
        modality_type: 'AUDIO',
        ability_type: 'MUSIC',
        ability_label: '音频模型（背景音乐）',
        models: [
          { model_code: 'minimax-music-v2.6', model_name: 'MiniMax Music v2.6', recommended: true },
        ],
      },
    ],
  },
  {
    provider_code: 'TENCENT_TOKENHUB',
    provider_name: '腾讯云 TokenHub',
    auth_type: 'BEARER',
    default_base_url: 'https://tokenhub.tencentmaas.com',
    abilities: [
      {
        modality_type: 'TEXT',
        ability_type: 'TEXT',
        ability_label: '文本模型',
        models: [
          { model_code: 'hy4', model_name: 'Hy4', recommended: true },
          { model_code: 'deepseek-v4-pro', model_name: 'DeepSeek-V4-Pro', recommended: false },
          { model_code: 'kimi-k3', model_name: 'Kimi K3', recommended: false },
        ],
      },
    ],
  },
]
