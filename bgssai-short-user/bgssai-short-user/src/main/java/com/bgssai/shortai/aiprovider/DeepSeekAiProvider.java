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
 * provider_code=DEEPSEEK (DeepSeek API) for user-configured API keys.
 * Supports TEXT (/chat/completions) for deepseek-chat and deepseek-reasoner.
 */
@Component
public class DeepSeekAiProvider implements AiProvider {

    public static final String CODE = "DEEPSEEK";

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiProvider.class);

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    private final ProviderHttp.Client http;

    public DeepSeekAiProvider() {
        this.http = new ProviderHttp.JdkClient();
    }

    DeepSeekAiProvider(UserAiModelKeyService userAiModelKeyService, ProviderHttp.Client http) {
        this.userAiModelKeyService = userAiModelKeyService;
        this.http = http;
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
        if (!UserAiModelSupport.isUserKeyModel(request.getModel())) {
            throw DirectVendorGuard.rejected(CODE);
        }
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (!"TEXT".equals(ability)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    "DeepSeek 仅支持文本模型，请为当前能力配置其他供应商密钥或切换混合模式");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        String baseUrl = UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE);
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), "deepseek-chat");
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);

        log.info("调用 DeepSeek 文本模型 userId={} keyId={} model={}", userId, keyId, modelCode);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
            body.put("stream", false);
            String raw = http.postJson(baseUrl + "/chat/completions",
                    Map.of("Authorization", "Bearer " + apiKey),
                    ShortJson.toJson(body), Math.min(180, timeoutSec));
            Map<String, Object> parsed = ShortJson.map(raw);
            String content = UserProviderMediaSupport.extractChatContent(parsed);
            if (content.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "DeepSeek 文本返回内容为空");
            }
            AiProviderResult result = AiProviderResult.text(content);
            int tokens = UserProviderMediaSupport.nestedInt(parsed, "usage", "total_tokens", 1);
            result.setUsageAmount(Math.max(1, tokens));
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "DeepSeek 模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("DeepSeek 模型调用异常 userId={} keyId={} model={} errType={}",
                    userId, keyId, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "DeepSeek 模型请求失败，请检查密钥与网络后重试");
        }
    }
}
