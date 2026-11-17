package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class ProviderAbilityPreset {
    private String modalityType;
    private String abilityType;
    private String abilityLabel;
    private List<ModelPresetOption> models;

    public ProviderAbilityPreset() {
    }

    public ProviderAbilityPreset(String modalityType, String abilityType, String abilityLabel,
            List<ModelPresetOption> models) {
        this.modalityType = modalityType;
        this.abilityType = abilityType;
        this.abilityLabel = abilityLabel;
        this.models = models;
    }

    public String getModalityType() {
        return modalityType;
    }

    public void setModalityType(String modalityType) {
        this.modalityType = modalityType;
    }

    public String getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(String abilityType) {
        this.abilityType = abilityType;
    }

    public String getAbilityLabel() {
        return abilityLabel;
    }

    public void setAbilityLabel(String abilityLabel) {
        this.abilityLabel = abilityLabel;
    }

    public List<ModelPresetOption> getModels() {
        return models;
    }

    public void setModels(List<ModelPresetOption> models) {
        this.models = models;
    }
}
