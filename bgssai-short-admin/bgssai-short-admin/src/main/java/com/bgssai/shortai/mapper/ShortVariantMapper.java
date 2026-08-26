package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortVariant;
import com.bgssai.shortai.model.ShortVariantExample;

@Mapper
public interface ShortVariantMapper {
    long countByExample(ShortVariantExample example);
    int insert(ShortVariant row);
    int insertSelective(ShortVariant row);
    List<ShortVariant> selectByExample(ShortVariantExample example);
    ShortVariant selectOneByExample(ShortVariantExample example);
    ShortVariant selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortVariant row, @Param("example") ShortVariantExample example);
    int updateByPrimaryKeySelective(ShortVariant row);
}
