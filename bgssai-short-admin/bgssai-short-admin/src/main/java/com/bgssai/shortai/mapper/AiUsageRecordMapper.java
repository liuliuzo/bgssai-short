package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.AiUsageRecord;
import com.bgssai.shortai.model.AiUsageRecordExample;

@Mapper
public interface AiUsageRecordMapper {
    long countByExample(AiUsageRecordExample example);
    int insert(AiUsageRecord row);
    int insertSelective(AiUsageRecord row);
    List<AiUsageRecord> selectByExample(AiUsageRecordExample example);
    AiUsageRecord selectOneByExample(AiUsageRecordExample example);
    AiUsageRecord selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") AiUsageRecord row, @Param("example") AiUsageRecordExample example);
    int updateByPrimaryKeySelective(AiUsageRecord row);
}
