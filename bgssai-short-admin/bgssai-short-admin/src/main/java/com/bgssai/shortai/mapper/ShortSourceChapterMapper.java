package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortSourceChapter;
import com.bgssai.shortai.model.ShortSourceChapterExample;

@Mapper
public interface ShortSourceChapterMapper {
    long countByExample(ShortSourceChapterExample example);
    int insert(ShortSourceChapter row);
    int insertSelective(ShortSourceChapter row);
    List<ShortSourceChapter> selectByExample(ShortSourceChapterExample example);
    ShortSourceChapter selectOneByExample(ShortSourceChapterExample example);
    ShortSourceChapter selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortSourceChapter row, @Param("example") ShortSourceChapterExample example);
    int updateByPrimaryKeySelective(ShortSourceChapter row);
}
