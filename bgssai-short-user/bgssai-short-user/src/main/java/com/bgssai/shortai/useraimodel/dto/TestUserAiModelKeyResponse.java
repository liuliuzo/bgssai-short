package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class TestUserAiModelKeyResponse {
    private boolean ok;
    private String status;
    private String message;
    private boolean modelMatched;
    private List<String> discoveredModels;

    public boolean getOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getModelMatched() {
        return modelMatched;
    }

    public void setModelMatched(boolean modelMatched) {
        this.modelMatched = modelMatched;
    }

    public List<String> getDiscoveredModels() {
        return discoveredModels;
    }

    public void setDiscoveredModels(List<String> discoveredModels) {
        this.discoveredModels = discoveredModels;
    }
}
