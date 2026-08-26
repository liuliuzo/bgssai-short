package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortContinuityIssueExample;

@Mapper
public interface ShortContinuityIssueMapper {
    long countByExample(ShortContinuityIssueExample example);
    int insert(ShortContinuityIssue row);
    int insertSelective(ShortContinuityIssue row);
    List<ShortContinuityIssue> selectByExample(ShortContinuityIssueExample example);
    ShortContinuityIssue selectOneByExample(ShortContinuityIssueExample example);
    ShortContinuityIssue selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortContinuityIssue row, @Param("example") ShortContinuityIssueExample example);
    int updateByPrimaryKeySelective(ShortContinuityIssue row);
}
