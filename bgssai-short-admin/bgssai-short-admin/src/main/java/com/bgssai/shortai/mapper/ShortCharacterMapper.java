package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;

@Mapper
public interface ShortCharacterMapper {
    long countByExample(ShortCharacterExample example);
    int insert(ShortCharacter row);
    int insertSelective(ShortCharacter row);
    List<ShortCharacter> selectByExample(ShortCharacterExample example);
    ShortCharacter selectOneByExample(ShortCharacterExample example);
    ShortCharacter selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortCharacter row, @Param("example") ShortCharacterExample example);
    int updateByPrimaryKeySelective(ShortCharacter row);
}
