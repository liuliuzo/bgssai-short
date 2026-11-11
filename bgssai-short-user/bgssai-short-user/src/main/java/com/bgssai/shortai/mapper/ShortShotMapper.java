package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;

@Mapper
public interface ShortShotMapper {
    long countByExample(ShortShotExample example);
    int insert(ShortShot row);
    int insertSelective(ShortShot row);
    List<ShortShot> selectByExample(ShortShotExample example);
    ShortShot selectOneByExample(ShortShotExample example);
    ShortShot selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortShot row, @Param("example") ShortShotExample example);
    int updateByPrimaryKeySelective(ShortShot row);
    int attachRecoveredVideoIfUnchanged(@Param("expected") ShortShot expected,
            @Param("patch") ShortShot patch, @Param("taskId") Long taskId);
}
