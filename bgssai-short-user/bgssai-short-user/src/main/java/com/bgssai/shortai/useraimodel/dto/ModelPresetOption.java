package com.bgssai.shortai.useraimodel.dto;

public class ModelPresetOption {
    private String modelCode;
    private String modelName;
    private boolean recommended;

    public ModelPresetOption() {
    }

    public ModelPresetOption(String modelCode, String modelName, boolean recommended) {
        this.modelCode = modelCode;
        this.modelName = modelName;
        this.recommended = recommended;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public boolean getRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }
}
