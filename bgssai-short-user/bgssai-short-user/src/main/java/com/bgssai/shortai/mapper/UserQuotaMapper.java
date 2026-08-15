package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;

@Mapper
public interface UserQuotaMapper {
    long countByExample(UserQuotaExample example);

    int deleteByExample(UserQuotaExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserQuota row);

    int insertSelective(UserQuota row);

    List<UserQuota> selectByExample(UserQuotaExample example);

    UserQuota selectOneByExample(UserQuotaExample example);

    UserQuota selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserQuota row, @Param("example") UserQuotaExample example);

    int updateByExample(@Param("row") UserQuota row, @Param("example") UserQuotaExample example);

    int updateByPrimaryKeySelective(UserQuota row);

    int updateByPrimaryKey(UserQuota row);
}
