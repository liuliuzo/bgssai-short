package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortLookLibrary;
import com.bgssai.shortai.model.ShortLookLibraryExample;

@Mapper
public interface ShortLookLibraryMapper {
    int insertSelective(ShortLookLibrary row);
    List<ShortLookLibrary> selectByExample(ShortLookLibraryExample example);
    ShortLookLibrary selectOneByExample(ShortLookLibraryExample example);
    int updateByExampleSelective(@Param("row") ShortLookLibrary row, @Param("example") ShortLookLibraryExample example);
}
