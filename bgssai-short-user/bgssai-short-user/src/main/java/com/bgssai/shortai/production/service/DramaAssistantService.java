package com.bgssai.shortai.production.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;

/**
 * 短剧工作台里的多角色助手。回复只来自已解析的文本模型，不返回写死的示例对白。
 */
@Service
public class DramaAssistantService {

    private static final int MESSAGE_LIMIT = 4000;
    private static final int CONTEXT_LIMIT = 8000;
    private static final Map<String, String> ROLES = Map.of(
            "SCRIPT", "你是短剧编剧。根据用户给出的情节写对白、钩子和戏剧动机，只使用用户提供的人物与事实。",
            "DIRECTOR", "你是短剧分镜导演。根据用户给出的场次建议景别、机位和衔接，不要假装已经改过工程。",
            "VISUAL", "你是短剧视觉顾问。把用户的画面要求整理成可直接用于出图的提示词，并说明光影和构图。",
            "EDITOR", "你是短剧剪辑顾问。根据用户描述建议镜头时长、节奏和声音点，不要声称文件已经导出。");

    private final AiModelResolveService models;
    private final AiProviderRegistry registry;

    public DramaAssistantService(AiModelResolveService models, AiProviderRegistry registry) {
        this.models = models;
        this.registry = registry;
    }

    public Map<String, Object> ask(String role, String message, String context, String modelCode) {
        if (UserContext.getUserId() == null || UserContext.getUserId().isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        String selected = normalizeRole(role);
        String question = requireText(message, "请先填写要问助手的内容", MESSAGE_LIMIT);
        String shot = trim(context, CONTEXT_LIMIT);
        String code = modelCode == null || modelCode.isBlank() ? null : modelCode.trim();
        AiModelConfig model = models.resolve("TEXT", code);
        AiProvider provider = registry.resolve(model);
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType("TEXT");
        request.setOperation("DRAMA_ASSISTANT");
        request.setModel(model);
        request.setPrompt(prompt(selected, question, shot));
        AiProviderResult result = provider.execute(request);
        String reply = result == null ? "" : result.getTextContent();
        if (reply == null || reply.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "模型未返回内容，请重试");
        }
        ProductModelAlias.Face face = ProductModelAlias.forStored("TEXT", model.getModelCode());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", selected);
        body.put("reply", reply.trim());
        body.put("model_code", face.code());
        body.put("model_name", face.name());
        return body;
    }

    private static String prompt(String role, String question, String shot) {
        StringBuilder text = new StringBuilder();
        text.append(ROLES.get(role)).append("\n");
        text.append("用简体中文直接回答。不要套用与本次问题无关的固定例句。\n");
        if (!shot.isBlank()) {
            text.append("\n当前镜头或场次上下文：\n").append(shot).append("\n");
        }
        text.append("\n用户请求：\n").append(question);
        return text.toString();
    }

    static String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toUpperCase();
        if (!ROLES.containsKey(value)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "不支持的助手角色");
        }
        return value;
    }

    private static String requireText(String raw, String emptyMessage, int limit) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, emptyMessage);
        }
        if (value.length() > limit) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "内容过长，请控制在 " + limit + " 字以内");
        }
        return value;
    }

    private static String trim(String raw, int limit) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > limit) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "镜头上下文过长，请控制在 " + limit + " 字以内");
        }
        return value;
    }
}
