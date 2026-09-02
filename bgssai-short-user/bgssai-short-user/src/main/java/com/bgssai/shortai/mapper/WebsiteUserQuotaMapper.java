package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteUserQuota;
import com.bgssai.shortai.model.WebsiteUserQuotaExample;

@Mapper
public interface WebsiteUserQuotaMapper {
    long countByExample(WebsiteUserQuotaExample example);

    int deleteByExample(WebsiteUserQuotaExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteUserQuota row);

    int insertSelective(WebsiteUserQuota row);

    List<WebsiteUserQuota> selectByExample(WebsiteUserQuotaExample example);

    WebsiteUserQuota selectOneByExample(WebsiteUserQuotaExample example);

    WebsiteUserQuota selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteUserQuota row, @Param("example") WebsiteUserQuotaExample example);

    int updateByExample(@Param("row") WebsiteUserQuota row, @Param("example") WebsiteUserQuotaExample example);

    int updateByPrimaryKeySelective(WebsiteUserQuota row);

    int updateByPrimaryKey(WebsiteUserQuota row);
}
