package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortShotPreviz;
import com.bgssai.shortai.model.ShortShotPrevizExample;

@Mapper
public interface ShortShotPrevizMapper {
    long countByExample(ShortShotPrevizExample example);
    int insert(ShortShotPreviz row);
    int insertSelective(ShortShotPreviz row);
    List<ShortShotPreviz> selectByExample(ShortShotPrevizExample example);
    ShortShotPreviz selectOneByExample(ShortShotPrevizExample example);
    ShortShotPreviz selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortShotPreviz row, @Param("example") ShortShotPrevizExample example);
    int updateByPrimaryKeySelective(ShortShotPreviz row);
}
