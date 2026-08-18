package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortScriptVersion;
import com.bgssai.shortai.model.ShortScriptVersionExample;

@Mapper
public interface ShortScriptVersionMapper {
    long countByExample(ShortScriptVersionExample example);
    int insert(ShortScriptVersion row);
    int insertSelective(ShortScriptVersion row);
    List<ShortScriptVersion> selectByExample(ShortScriptVersionExample example);
    ShortScriptVersion selectOneByExample(ShortScriptVersionExample example);
    ShortScriptVersion selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortScriptVersion row, @Param("example") ShortScriptVersionExample example);
    int updateByPrimaryKeySelective(ShortScriptVersion row);
}
