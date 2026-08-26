package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortStoryMap;
import com.bgssai.shortai.model.ShortStoryMapExample;

@Mapper
public interface ShortStoryMapMapper {
    long countByExample(ShortStoryMapExample example);
    int insert(ShortStoryMap row);
    int insertSelective(ShortStoryMap row);
    List<ShortStoryMap> selectByExample(ShortStoryMapExample example);
    ShortStoryMap selectOneByExample(ShortStoryMapExample example);
    ShortStoryMap selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortStoryMap row, @Param("example") ShortStoryMapExample example);
    int updateByPrimaryKeySelective(ShortStoryMap row);
}
