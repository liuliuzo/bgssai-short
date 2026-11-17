package com.bgssai.shortai.aiprovider;

import java.util.Map;
import java.util.Optional;

import com.bgssai.shortai.billing.UsageScope;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

/**
 * 厂商直连安全守卫及用户 BYOK 凭据解析辅助。
 */
public final class DirectVendorGuard {

    private DirectVendorGuard() {
    }

    public static BizException rejected(String providerCode) {
        return new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                TokenHubClientSupport.userFacingMessage(
                        "direct vendor disabled for " + providerCode + "; use CN hub"));
    }

    public static String resolveUserId(AiProviderRequest request) {
        if (request == null) {
            return "";
        }
        return resolveUserId(request.getModel(), request.getParams());
    }

    public static String resolveUserId(AiModelConfig model, Map<String, Object> params) {
        if (model != null) {
            Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
            String fromCfg = ProviderHttp.firstNonBlank(
                    ShortJson.text(cfg.get("user_id")),
                    ShortJson.text(cfg.get("byok_user_id")));
            if (!fromCfg.isBlank()) {
                return fromCfg;
            }
        }
        if (params != null) {
            String fromParam = ShortJson.text(params.get("user_id"));
            if (!fromParam.isBlank()) {
                return fromParam;
            }
        }
        String fromCtx = UserContext.getUserId();
        if (fromCtx != null && !fromCtx.isBlank()) {
            return fromCtx.trim();
        }
        UsageScope scope = UsageScope.current();
        if (scope != null && scope.userId() != null && !scope.userId().isBlank()) {
            return scope.userId().trim();
        }
        return "";
    }

    public static boolean isExplicitUserModelKey(AiModelConfig model) {
        if (model == null || !UserAiModelSupport.isUserKeyModel(model)) {
            return false;
        }
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        String byokSource = ShortJson.text(cfg.get("byok_source"));
        if ("PROVIDER_KEY".equalsIgnoreCase(byokSource)) {
            return false;
        }
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")), model.getModelCode());
        if (keyId.startsWith(UserAiModelSupport.PROVIDER_KEY_PREFIX)) {
            return false;
        }
        return keyId.startsWith(UserAiModelSupport.USER_KEY_PREFIX) || !"PROVIDER_KEY".equalsIgnoreCase(byokSource);
    }

    public static Optional<ResolvedProviderKey> resolveProviderByok(AiProviderRequest request,
            UserAiProviderKeyService service) {
        if (request == null || request.getModel() == null) {
            return Optional.empty();
        }
        return resolveProviderByok(request.getModel(), request.getParams(), service);
    }

    public static Optional<ResolvedProviderKey> resolveProviderByok(AiModelConfig model,
            Map<String, Object> params, UserAiProviderKeyService service) {
        if (model == null || service == null) {
            return Optional.empty();
        }
        String userId = resolveUserId(model, params);
        if (userId.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        String rawProvider = ProviderHttp.firstNonBlank(
                ShortJson.text(cfg.get("byok_provider_code")),
                model.getProviderCode());
        if (rawProvider.isBlank()) {
            return Optional.empty();
        }
        String canonical = UserAiProviderCatalog.normalizeRuntimeProviderCode(rawProvider);
        return service.findActiveKey(userId, canonical);
    }

    public static boolean hasAnyUserByok(AiModelConfig model, Map<String, Object> params,
            UserAiProviderKeyService service) {
        return UserAiModelSupport.isUserKeyModel(model)
                || resolveProviderByok(model, params, service).isPresent();
    }
}
