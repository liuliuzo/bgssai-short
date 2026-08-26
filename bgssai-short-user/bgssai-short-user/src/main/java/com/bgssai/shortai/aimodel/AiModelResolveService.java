package com.bgssai.shortai.aimodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;

@Service
public class AiModelResolveService {

    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;

    public AiModelConfig resolve(String abilityType, String modelCode) {
        if (abilityType == null || abilityType.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ability_type required");
        }
        String ability = abilityType.trim();
        if (modelCode != null && !modelCode.isBlank()) {
            AiModelConfigExample ex = new AiModelConfigExample();
            ex.createCriteria()
                    .andAbilityTypeEqualTo(ability)
                    .andModelCodeEqualTo(modelCode.trim())
                    .andEnabledFlagEqualTo(1)
                    .andDeletedFlagEqualTo(0);
            AiModelConfig row = aiModelConfigMapper.selectOneByExample(ex);
            if (row == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "model disabled or not found: " + modelCode);
            }
            return row;
        }
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andDefaultFlagEqualTo(1)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        AiModelConfig row = aiModelConfigMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE);
        }
        return row;
    }

    public int costCent(AiModelConfig model, int usageAmount) {
        int price = model.getUnitPriceCent() == null ? 0 : model.getUnitPriceCent();
        int amount = usageAmount <= 0 ? 1 : usageAmount;
        return price * amount;
    }
}
