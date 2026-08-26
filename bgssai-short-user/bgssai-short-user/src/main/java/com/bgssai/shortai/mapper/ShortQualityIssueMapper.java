package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortQualityIssue;
import com.bgssai.shortai.model.ShortQualityIssueExample;

@Mapper
public interface ShortQualityIssueMapper {
    long countByExample(ShortQualityIssueExample example);
    int insert(ShortQualityIssue row);
    int insertSelective(ShortQualityIssue row);
    List<ShortQualityIssue> selectByExample(ShortQualityIssueExample example);
    ShortQualityIssue selectOneByExample(ShortQualityIssueExample example);
    ShortQualityIssue selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortQualityIssue row, @Param("example") ShortQualityIssueExample example);
    int updateByPrimaryKeySelective(ShortQualityIssue row);
}
