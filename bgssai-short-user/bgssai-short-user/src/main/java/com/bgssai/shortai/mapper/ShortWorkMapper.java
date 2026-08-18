package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;

@Mapper
public interface ShortWorkMapper {
    long countByExample(ShortWorkExample example);
    int insert(ShortWork row);
    int insertSelective(ShortWork row);
    List<ShortWork> selectByExample(ShortWorkExample example);
    ShortWork selectOneByExample(ShortWorkExample example);
    ShortWork selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortWork row, @Param("example") ShortWorkExample example);
    int updateByPrimaryKeySelective(ShortWork row);
}
