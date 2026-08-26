package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;

@Mapper
public interface ShortDubbingMapper {
    long countByExample(ShortDubbingExample example);
    int insert(ShortDubbing row);
    int insertSelective(ShortDubbing row);
    List<ShortDubbing> selectByExample(ShortDubbingExample example);
    ShortDubbing selectOneByExample(ShortDubbingExample example);
    ShortDubbing selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortDubbing row, @Param("example") ShortDubbingExample example);
    int updateByPrimaryKeySelective(ShortDubbing row);
}
