package com.bgssai.shortai.aiprovider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;

/**
 * User-owned Tencent Cloud TokenHub (product 1823). Bean code is separate from
 * the platform {@code TENCENT_TOKENHUB} client, which calls BGSSAI TokenHub.
 * Text uses {@code POST /v1/chat/completions} with {@code Authorization: Bearer}.
 */
@Component
public class TencentCloudTokenHubAiProvider implements AiProvider {

    public static final String CODE = "TENCENT_CLOUD_TOKENHUB";
    public static final String DEFAULT_TEXT_MODEL = "hy4";

    private static final Logger log = LoggerFactory.getLogger(TencentCloudTokenHubAiProvider.class);

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    private final ProviderHttp.Client http;

    public TencentCloudTokenHubAiProvider() {
        this(new ProviderHttp.JdkClient());
    }

    TencentCloudTokenHubAiProvider(UserAiModelKeyService userAiModelKeyService, ProviderHttp.Client http) {
        this.userAiModelKeyService = userAiModelKeyService;
        this.http = http;
    }

    TencentCloudTokenHubAiProvider(ProviderHttp.Client http) {
        this.http = http;
    }

    void setUserAiModelKeyService(UserAiModelKeyService userAiModelKeyService) {
        this.userAiModelKeyService = userAiModelKeyService;
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        if (!DirectVendorGuard.isExplicitUserModelKey(request.getModel())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "腾讯云 TokenHub 仅接受自有密钥");
        }
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (!"TEXT".equals(ability)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "腾讯云 TokenHub 当前仅支持文本");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String base = UserAiModelSupport.normalizeBaseUrl(
                ShortJson.text(cfg.get("base_url")), UserAiModelSupport.PROVIDER_TENCENT_TOKENHUB);
        String root = UserAiModelSupport.stripTrailingPath(base, "/v1");
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        if (userAiModelKeyService == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "自有模型密钥配置缺失");
        }
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        String modelCode = request.getModel().getModelCode();
        if (modelCode == null || modelCode.isBlank()) {
            modelCode = DEFAULT_TEXT_MODEL;
        } else {
            modelCode = modelCode.trim();
        }
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        log.info("调用腾讯云 TokenHub userId={} keyId={} model={}", userId, keyId, modelCode);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
            body.put("stream", false);
            String raw = http.postJson(root + "/v1/chat/completions",
                    Map.of("Authorization", "Bearer " + apiKey),
                    ShortJson.toJson(body), Math.min(120, timeoutSec));
            Map<String, Object> parsed = ShortJson.map(raw);
            String content = UserProviderMediaSupport.extractChatContent(parsed);
            if (content.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "腾讯云 TokenHub 文本返回内容为空");
            }
            AiProviderResult result = AiProviderResult.text(content);
            int tokens = UserProviderMediaSupport.nestedInt(parsed, "usage", "total_tokens", 1);
            result.setUsageAmount(Math.max(1, tokens));
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "腾讯云 TokenHub 模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("腾讯云 TokenHub 调用异常 userId={} keyId={} model={} errType={}",
                    userId, keyId, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "腾讯云 TokenHub 请求失败，请检查密钥与网络后重试");
        }
    }
}
