package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;

@Mapper
public interface AiModelConfigMapper {
    long countByExample(AiModelConfigExample example);
    int insert(AiModelConfig row);
    int insertSelective(AiModelConfig row);
    List<AiModelConfig> selectByExample(AiModelConfigExample example);
    AiModelConfig selectOneByExample(AiModelConfigExample example);
    AiModelConfig selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") AiModelConfig row, @Param("example") AiModelConfigExample example);
    int updateByPrimaryKeySelective(AiModelConfig row);
}
