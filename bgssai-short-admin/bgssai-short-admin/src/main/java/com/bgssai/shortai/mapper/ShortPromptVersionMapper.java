package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortPromptVersion;
import com.bgssai.shortai.model.ShortPromptVersionExample;

@Mapper
public interface ShortPromptVersionMapper {
    long countByExample(ShortPromptVersionExample example);
    int insert(ShortPromptVersion row);
    int insertSelective(ShortPromptVersion row);
    List<ShortPromptVersion> selectByExample(ShortPromptVersionExample example);
    ShortPromptVersion selectOneByExample(ShortPromptVersionExample example);
    ShortPromptVersion selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortPromptVersion row, @Param("example") ShortPromptVersionExample example);
    int updateByPrimaryKeySelective(ShortPromptVersion row);
}
