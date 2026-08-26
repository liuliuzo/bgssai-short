package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortQualityGate;
import com.bgssai.shortai.model.ShortQualityGateExample;

@Mapper
public interface ShortQualityGateMapper {
    long countByExample(ShortQualityGateExample example);
    int insert(ShortQualityGate row);
    int insertSelective(ShortQualityGate row);
    List<ShortQualityGate> selectByExample(ShortQualityGateExample example);
    ShortQualityGate selectOneByExample(ShortQualityGateExample example);
    ShortQualityGate selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortQualityGate row, @Param("example") ShortQualityGateExample example);
    int updateByPrimaryKeySelective(ShortQualityGate row);
}
