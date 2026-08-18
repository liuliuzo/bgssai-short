package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;

@Mapper
public interface ShortSceneCardMapper {
    long countByExample(ShortSceneCardExample example);
    int insert(ShortSceneCard row);
    int insertSelective(ShortSceneCard row);
    List<ShortSceneCard> selectByExample(ShortSceneCardExample example);
    ShortSceneCard selectOneByExample(ShortSceneCardExample example);
    ShortSceneCard selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortSceneCard row, @Param("example") ShortSceneCardExample example);
    int updateByPrimaryKeySelective(ShortSceneCard row);
}
