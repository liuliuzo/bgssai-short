package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.model.ShortLookMaterialExample;

@Mapper
public interface ShortLookMaterialMapper {
    int insertSelective(ShortLookMaterial row);
    List<ShortLookMaterial> selectByExample(ShortLookMaterialExample example);
    ShortLookMaterial selectOneByExample(ShortLookMaterialExample example);
    int updateByExampleSelective(@Param("row") ShortLookMaterial row, @Param("example") ShortLookMaterialExample example);
}
