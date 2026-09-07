package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortEditExport;
import com.bgssai.shortai.model.ShortEditExportExample;

@Mapper
public interface ShortEditExportMapper {
    long countByExample(ShortEditExportExample example);
    int insert(ShortEditExport row);
    int insertSelective(ShortEditExport row);
    List<ShortEditExport> selectByExample(ShortEditExportExample example);
    ShortEditExport selectOneByExample(ShortEditExportExample example);
    ShortEditExport selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortEditExport row, @Param("example") ShortEditExportExample example);
    int updateByPrimaryKeySelective(ShortEditExport row);
}
