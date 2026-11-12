# 契约：AI 戏剧多专家协同

字段 snake_case；统一响应 `{ code, message, success, result }`；鉴权请求头 `Jwttoken`。

原型：[`../demo-static/web/user/drama-assistant.html`](../demo-static/web/user/drama-assistant.html)。需求：[`../feature/drama-assistant-experts.md`](../feature/drama-assistant-experts.md)。

## POST /bgssai/user/drama-assist/advise

登录用户。按角色调用 CN `TEXT` 模型，注入作品上下文，返回结构化建议。不自动写库。

### 请求

```json
{
  "work_id": "w-dm-005",
  "episode_id": "ep-xxx",
  "shot_id": "shot-xxx",
  "role": "DIRECTOR",
  "question": "请检查相邻镜头是否跳轴",
  "model_code": null
}
```

- `work_id` 必填。
- `role`：`SCRIPT` / `DIRECTOR` / `VISUAL` / `EDITOR`。
- `question` 必填，最长 2000。
- `episode_id` / `shot_id` / `model_code` 可选；未传模型则用默认 TEXT。

### 成功

```json
{
  "code": "0",
  "success": true,
  "result": {
    "role": "DIRECTOR",
    "role_name": "分镜导演",
    "advice_text": "...",
    "summary": "...",
    "action_list": ["调整镜2视线方向"],
    "apply_target": "shot_desc",
    "apply_text": "...",
    "context_used": { "work_id": "w-dm-005", "episode_id": "ep-xxx", "shot_id": "shot-xxx", "character_count": 3 },
    "cost_cent": 12,
    "model_display_name": "文本模型"
  }
}
```

### 失败

- `4001` / `4003` 鉴权或无权。
- `INVALID_PARAM`：缺字段 / 非法 role。
- 额度不足：既有额度错误码。
- 上游失败：可读文案，不暴露中枢品牌。

## POST /bgssai/user/drama-assist/apply

登录用户。确认后写回单个镜头字段。

### 请求

```json
{
  "work_id": "w-dm-005",
  "shot_id": "shot-xxx",
  "apply_target": "shot_desc",
  "apply_text": "过肩中景，视线朝右..."
}
```

- `apply_target`：`shot_desc` / `dialogue_text` / `extra_prompt`。

### 成功

```json
{
  "code": "0",
  "success": true,
  "result": {
    "shot_id": "shot-xxx",
    "apply_target": "shot_desc",
    "applied_flag": 1
  }
}
```
