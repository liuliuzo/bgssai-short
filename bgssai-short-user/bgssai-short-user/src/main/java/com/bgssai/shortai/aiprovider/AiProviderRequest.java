package com.bgssai.shortai.aiprovider;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bgssai.shortai.model.AiModelConfig;

public class AiProviderRequest {

    private String abilityType;
    private String operation;
    private AiModelConfig model;
    private String prompt;
    private Map<String, Object> params = new LinkedHashMap<>();

    public String getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(String abilityType) {
        this.abilityType = abilityType;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public AiModelConfig getModel() {
        return model;
    }

    public void setModel(AiModelConfig model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }

    public Object param(String key) {
        return params.get(key);
    }
}
