package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;

@Mapper
public interface ShortAssetMapper {
    long countByExample(ShortAssetExample example);
    int insert(ShortAsset row);
    int insertSelective(ShortAsset row);
    List<ShortAsset> selectByExample(ShortAssetExample example);
    ShortAsset selectOneByExample(ShortAssetExample example);
    ShortAsset selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortAsset row, @Param("example") ShortAssetExample example);
    int updateByPrimaryKeySelective(ShortAsset row);
}
