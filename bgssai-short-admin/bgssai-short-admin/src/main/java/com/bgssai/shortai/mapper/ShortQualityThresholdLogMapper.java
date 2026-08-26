package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortQualityThresholdLog;
import com.bgssai.shortai.model.ShortQualityThresholdLogExample;

@Mapper
public interface ShortQualityThresholdLogMapper {
    long countByExample(ShortQualityThresholdLogExample example);
    int insert(ShortQualityThresholdLog row);
    int insertSelective(ShortQualityThresholdLog row);
    List<ShortQualityThresholdLog> selectByExample(ShortQualityThresholdLogExample example);
    ShortQualityThresholdLog selectOneByExample(ShortQualityThresholdLogExample example);
    ShortQualityThresholdLog selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortQualityThresholdLog row, @Param("example") ShortQualityThresholdLogExample example);
    int updateByPrimaryKeySelective(ShortQualityThresholdLog row);
}
