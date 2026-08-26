package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortCharacterRelation;
import com.bgssai.shortai.model.ShortCharacterRelationExample;

@Mapper
public interface ShortCharacterRelationMapper {
    long countByExample(ShortCharacterRelationExample example);
    int insert(ShortCharacterRelation row);
    int insertSelective(ShortCharacterRelation row);
    List<ShortCharacterRelation> selectByExample(ShortCharacterRelationExample example);
    ShortCharacterRelation selectOneByExample(ShortCharacterRelationExample example);
    ShortCharacterRelation selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortCharacterRelation row, @Param("example") ShortCharacterRelationExample example);
    int updateByPrimaryKeySelective(ShortCharacterRelation row);
}
