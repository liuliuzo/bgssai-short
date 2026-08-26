package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortForeshadow;
import com.bgssai.shortai.model.ShortForeshadowExample;

@Mapper
public interface ShortForeshadowMapper {
    long countByExample(ShortForeshadowExample example);
    int insert(ShortForeshadow row);
    int insertSelective(ShortForeshadow row);
    List<ShortForeshadow> selectByExample(ShortForeshadowExample example);
    ShortForeshadow selectOneByExample(ShortForeshadowExample example);
    ShortForeshadow selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortForeshadow row, @Param("example") ShortForeshadowExample example);
    int updateByPrimaryKeySelective(ShortForeshadow row);
}
