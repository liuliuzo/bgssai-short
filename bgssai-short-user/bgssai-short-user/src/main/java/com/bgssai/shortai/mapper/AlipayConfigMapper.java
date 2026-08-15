package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.AlipayConfig;
import com.bgssai.shortai.model.AlipayConfigExample;

@Mapper
public interface AlipayConfigMapper {
    long countByExample(AlipayConfigExample example);

    int deleteByExample(AlipayConfigExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AlipayConfig row);

    int insertSelective(AlipayConfig row);

    List<AlipayConfig> selectByExample(AlipayConfigExample example);

    AlipayConfig selectOneByExample(AlipayConfigExample example);

    AlipayConfig selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") AlipayConfig row, @Param("example") AlipayConfigExample example);

    int updateByExample(@Param("row") AlipayConfig row, @Param("example") AlipayConfigExample example);

    int updateByPrimaryKeySelective(AlipayConfig row);

    int updateByPrimaryKey(AlipayConfig row);
}
