# Central Ledger Accounting (`bgssai-short`)

## 1. Architecture Overview (Plan A: Cross-DB `bgssai_chat.*` + `bgssai-tokenhub-cn`)

`bgssai-short` is a Mainland China (CN) application deployed on the same regional MySQL instance as `bgssai-chat-cn` (schema `bgssai_chat`) and routes all AI/LLM model calls through the same-region gateway `bgssai-tokenhub-cn`.

Under **Plan A**:
1. **Unified LLM Gateway (`bgssai-tokenhub-cn`)**:
   - All text/LLM and multimodal model requests are routed through `bgssai-tokenhub-cn` (`https://www.bgssai-tokenhub.cn` in production, `https://dev.user.bgssai-tokenhub.cn` in dev).
   - Default model is `bgssai-chat` (`0.5x` multiplier tier).
   - User-facing UI and error messages never expose `TokenHub` branding and only display model names with `0.5x / 1x / 1.5x` billing multipliers.
2. **Mandatory `bgssai-chat` Account Binding & Cross-Database Central Ledger (`bgssai_chat.*`)**:
   - Calling any LLM/AI model requires the user to be logged in or bound to a `bgssai-chat` account (`oauth_binding.platform = 'bgssai'`). Unbound users are rejected with `请先绑定或使用 bgssai-chat 账号登录后再调用大模型`.
   - User subscription and quota verification/deduction read and write directly to `bgssai-chat-cn`'s `bgssai_chat.*` tables via MyBatis (`Website*Mapper.xml`), with no HTTP round-trip and no duplicate local `ledger_*` tables.

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

1. **Account Resolution**: `BgssaiWebsiteLedgerClient.requireBoundWebsiteUserId(localUserId)` resolves `oauth_binding.open_id` (`platform = 'bgssai'`). If missing, throws `IllegalStateException("请先绑定或使用 bgssai-chat 账号登录后再调用大模型")`.
2. **Multiplier Calculation**:
   - `0.5x`: `bgssai-chat` (default)
   - `1x`: Standard domestic models (`deepseek-*`, `qwen-*`, `glm-*`, `doubao-*`, `kimi-*`, `minimax-*`)
   - `1.5x`: Overseas flagship models (`gpt-*`, `claude-*`, `gemini-*`, `grok-*`)
3. **Deduction Order**: `CentralLedgerService` deducts `billedTokens = round(rawTokens * multiplier)` in `Propagation.REQUIRES_NEW` order:
   1. `bonus_remaining_tokens`
   2. `month_quota_tokens - month_used_tokens`
   3. `remaining_tokens`
4. **Audit & Idempotency**: Writes idempotency record to `bgssai_chat.usage_idempotency` and usage entry to `bgssai_chat.usage_ledger`.
