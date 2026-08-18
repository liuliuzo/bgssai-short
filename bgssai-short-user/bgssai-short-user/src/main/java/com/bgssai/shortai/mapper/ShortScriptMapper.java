package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;

@Mapper
public interface ShortScriptMapper {
    long countByExample(ShortScriptExample example);
    int insert(ShortScript row);
    int insertSelective(ShortScript row);
    List<ShortScript> selectByExample(ShortScriptExample example);
    ShortScript selectOneByExample(ShortScriptExample example);
    ShortScript selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortScript row, @Param("example") ShortScriptExample example);
    int updateByPrimaryKeySelective(ShortScript row);
}
