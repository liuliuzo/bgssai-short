package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortReferenceTemplate;
import com.bgssai.shortai.model.ShortReferenceTemplateExample;

@Mapper
public interface ShortReferenceTemplateMapper {
    long countByExample(ShortReferenceTemplateExample example);
    int insert(ShortReferenceTemplate row);
    int insertSelective(ShortReferenceTemplate row);
    List<ShortReferenceTemplate> selectByExample(ShortReferenceTemplateExample example);
    ShortReferenceTemplate selectOneByExample(ShortReferenceTemplateExample example);
    ShortReferenceTemplate selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortReferenceTemplate row, @Param("example") ShortReferenceTemplateExample example);
    int updateByPrimaryKeySelective(ShortReferenceTemplate row);
}
