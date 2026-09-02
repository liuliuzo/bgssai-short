package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteExternalSubscription;
import com.bgssai.shortai.model.WebsiteExternalSubscriptionExample;

@Mapper
public interface WebsiteExternalSubscriptionMapper {
    long countByExample(WebsiteExternalSubscriptionExample example);

    int deleteByExample(WebsiteExternalSubscriptionExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteExternalSubscription row);

    int insertSelective(WebsiteExternalSubscription row);

    List<WebsiteExternalSubscription> selectByExample(WebsiteExternalSubscriptionExample example);

    WebsiteExternalSubscription selectOneByExample(WebsiteExternalSubscriptionExample example);

    WebsiteExternalSubscription selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteExternalSubscription row, @Param("example") WebsiteExternalSubscriptionExample example);

    int updateByExample(@Param("row") WebsiteExternalSubscription row, @Param("example") WebsiteExternalSubscriptionExample example);

    int updateByPrimaryKeySelective(WebsiteExternalSubscription row);

    int updateByPrimaryKey(WebsiteExternalSubscription row);
}
