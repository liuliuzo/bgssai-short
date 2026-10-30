# Central Ledger Accounting (`bgssai-short`)

## 1. Architecture Overview (Plan A: Cross-DB `bgssai_chat.*` + `bgssai-tokenhub-cn`)

`bgssai-short` is a Mainland China (CN) application deployed on the same regional MySQL instance as `bgssai-chat-cn` (schema `bgssai_chat`) and routes all AI/LLM model calls through the same-region gateway `bgssai-tokenhub-cn`.

Under **Plan A**:
1. **Unified LLM Gateway (`bgssai-tokenhub-cn`)**:
   - All text/LLM and multimodal model requests are routed through `bgssai-tokenhub-cn` (`https://www.bgssai-tokenhub.cn` in production, `https://dev.user.bgssai-tokenhub.cn` in dev).
   - Default model is `bgssai-chat` (`0.5x` multiplier tier).
   - User-facing UI and error messages never expose `TokenHub` branding and only display model names with `0.5x / 1x / 1.5x` billing multipliers.
2. **Model credential vs quota account**:
   - The HTTP call uses the platform TokenHub-CN company key (`sys_secret` `ai.tokenhub_cn.api_key` via `secret_ref`). It does not send the user's bgssai-chat OAuth token. Company defaults `bgssai-chat` / `bgssai-image` / `bgssai-tts` work for a password QA user with no personal Chat bind.
   - Central ledger debit (`bgssai_chat.*`) applies only when `oauth_binding.platform = 'bgssai'` already exists. Unbound users are billed on this product's `user_quota`. If they have no local subscription, the FREE plan balance is granted first (fallback 10000 cent when the plan row is missing).
   - Create (`POST /bgssai/user/short-works/create`) checks that balance before insert and before the outline task is queued. A shortfall returns `4007` and does not start a task. The worker repeats the check before calling the model, so a later shortfall does not persist an outline and then mark the task `MODEL_TIMEOUT`.
   - `consumeLlmQuota` still requires a bound chat user. Task settlement does not use that method.
   - Ops: the company key and a gateway catalog entry for `bgssai-chat` must stay configured. A personal Chat OAuth bind is not required. If the account is already bound, an empty chat quota still blocks with `4007`.

## 2. Cross-Database Tables (`bgssai_chat.*`)

| Mapper XML | Target Table in `bgssai_chat` | Purpose |
|---|---|---|
| `WebsiteUserMapper.xml` | `bgssai_chat.website_user` | Central `bgssai-chat` user profile |
| `WebsiteUserSubscriptionMapper.xml` | `bgssai_chat.user_subscription` | Active subscription state |
| `WebsiteSubscriptionPlanMapper.xml` | `bgssai_chat.subscription_plan` | Subscription plan catalog |
| `WebsiteUserQuotaMapper.xml` | `bgssai_chat.user_quota` | Central token quota balance (`bonus_remaining_tokens`, `month_quota_tokens`, `month_used_tokens`, `remaining_tokens`) |
| `WebsiteUsageLedgerMapper.xml` | `bgssai_chat.usage_ledger` | Immutable token consumption ledger (`product_code = 'bgssai-short'`) |
| `WebsiteUsageIdempotencyMapper.xml` | `bgssai_chat.usage_idempotency` | Idempotency guard (`(client_id, idempotency_key)`) |
| `WebsiteExternalSubscriptionMapper.xml` | `bgssai_chat.external_subscription` | External subscription mapping |

## 3. Quota Check & Deduction Flow

1. **Account Resolution**: Task affordability and settlement call `websiteUserIdOf`. A missing `oauth_binding` (`platform = 'bgssai'`) selects the local wallet. `requireBoundWebsiteUserId` remains for explicit ledger APIs such as `consumeLlmQuota`, and still throws `请先绑定或使用 bgssai-chat 账号登录后再调用大模型` only on that path.
2. **Multiplier Calculation**:
   - `0.5x`: `bgssai-chat` (default)
   - `1x`: Standard domestic models (`deepseek-*`, `qwen-*`, `glm-*`, `doubao-*`, `kimi-*`, `minimax-*`)
   - `1.5x`: Overseas flagship models (`gpt-*`, `claude-*`, `gemini-*`, `grok-*`)
3. **Deduction Order**: `CentralLedgerService` deducts `billedTokens = round(rawTokens * multiplier)` in `Propagation.REQUIRES_NEW` order:
   1. `bonus_remaining_tokens`
   2. `month_quota_tokens - month_used_tokens`
   3. `remaining_tokens`
4. **Audit & Idempotency**: Writes idempotency record to `bgssai_chat.usage_idempotency` and usage entry to `bgssai_chat.usage_ledger`.
