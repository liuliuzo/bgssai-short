package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortSubtitle;
import com.bgssai.shortai.model.ShortSubtitleExample;

@Mapper
public interface ShortSubtitleMapper {
    long countByExample(ShortSubtitleExample example);
    int insert(ShortSubtitle row);
    int insertSelective(ShortSubtitle row);
    List<ShortSubtitle> selectByExample(ShortSubtitleExample example);
    ShortSubtitle selectOneByExample(ShortSubtitleExample example);
    ShortSubtitle selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortSubtitle row, @Param("example") ShortSubtitleExample example);
    int updateByPrimaryKeySelective(ShortSubtitle row);
}
