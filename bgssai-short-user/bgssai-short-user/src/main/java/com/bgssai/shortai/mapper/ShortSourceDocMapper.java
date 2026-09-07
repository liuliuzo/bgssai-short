package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortSourceDoc;
import com.bgssai.shortai.model.ShortSourceDocExample;

@Mapper
public interface ShortSourceDocMapper {
    long countByExample(ShortSourceDocExample example);
    int insert(ShortSourceDoc row);
    int insertSelective(ShortSourceDoc row);
    List<ShortSourceDoc> selectByExample(ShortSourceDocExample example);
    ShortSourceDoc selectOneByExample(ShortSourceDocExample example);
    ShortSourceDoc selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortSourceDoc row, @Param("example") ShortSourceDocExample example);
    int updateByPrimaryKeySelective(ShortSourceDoc row);
}
