package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.CreditPack;
import com.bgssai.shortai.model.CreditPackExample;

@Mapper
public interface CreditPackMapper {
    long countByExample(CreditPackExample example);

    int deleteByExample(CreditPackExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CreditPack row);

    int insertSelective(CreditPack row);

    List<CreditPack> selectByExample(CreditPackExample example);

    CreditPack selectOneByExample(CreditPackExample example);

    CreditPack selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") CreditPack row, @Param("example") CreditPackExample example);

    int updateByExample(@Param("row") CreditPack row, @Param("example") CreditPackExample example);

    int updateByPrimaryKeySelective(CreditPack row);

    int updateByPrimaryKey(CreditPack row);
}
