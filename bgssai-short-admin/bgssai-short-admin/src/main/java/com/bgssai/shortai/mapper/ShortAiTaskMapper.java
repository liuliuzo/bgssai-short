package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;

@Mapper
public interface ShortAiTaskMapper {
    long countByExample(ShortAiTaskExample example);
    int insert(ShortAiTask row);
    int insertSelective(ShortAiTask row);
    List<ShortAiTask> selectByExample(ShortAiTaskExample example);
    ShortAiTask selectOneByExample(ShortAiTaskExample example);
    ShortAiTask selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortAiTask row, @Param("example") ShortAiTaskExample example);
    int updateByPrimaryKeySelective(ShortAiTask row);
}
