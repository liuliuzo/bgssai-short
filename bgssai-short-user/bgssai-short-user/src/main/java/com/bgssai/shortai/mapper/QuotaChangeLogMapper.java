package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.QuotaChangeLog;
import com.bgssai.shortai.model.QuotaChangeLogExample;

@Mapper
public interface QuotaChangeLogMapper {
    long countByExample(QuotaChangeLogExample example);

    int deleteByExample(QuotaChangeLogExample example);

    int deleteByPrimaryKey(Long id);

    int insert(QuotaChangeLog row);

    int insertSelective(QuotaChangeLog row);

    List<QuotaChangeLog> selectByExample(QuotaChangeLogExample example);

    QuotaChangeLog selectOneByExample(QuotaChangeLogExample example);

    QuotaChangeLog selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") QuotaChangeLog row, @Param("example") QuotaChangeLogExample example);

    int updateByExample(@Param("row") QuotaChangeLog row, @Param("example") QuotaChangeLogExample example);

    int updateByPrimaryKeySelective(QuotaChangeLog row);

    int updateByPrimaryKey(QuotaChangeLog row);
}
