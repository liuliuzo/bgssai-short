package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortComplianceHit;
import com.bgssai.shortai.model.ShortComplianceHitExample;

@Mapper
public interface ShortComplianceHitMapper {
    long countByExample(ShortComplianceHitExample example);
    int insert(ShortComplianceHit row);
    int insertSelective(ShortComplianceHit row);
    List<ShortComplianceHit> selectByExample(ShortComplianceHitExample example);
    ShortComplianceHit selectOneByExample(ShortComplianceHitExample example);
    ShortComplianceHit selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortComplianceHit row, @Param("example") ShortComplianceHitExample example);
    int updateByPrimaryKeySelective(ShortComplianceHit row);
}
