package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteUserSubscription;
import com.bgssai.shortai.model.WebsiteUserSubscriptionExample;

@Mapper
public interface WebsiteUserSubscriptionMapper {
    long countByExample(WebsiteUserSubscriptionExample example);

    int deleteByExample(WebsiteUserSubscriptionExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteUserSubscription row);

    int insertSelective(WebsiteUserSubscription row);

    List<WebsiteUserSubscription> selectByExample(WebsiteUserSubscriptionExample example);

    WebsiteUserSubscription selectOneByExample(WebsiteUserSubscriptionExample example);

    WebsiteUserSubscription selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteUserSubscription row, @Param("example") WebsiteUserSubscriptionExample example);

    int updateByExample(@Param("row") WebsiteUserSubscription row, @Param("example") WebsiteUserSubscriptionExample example);

    int updateByPrimaryKeySelective(WebsiteUserSubscription row);

    int updateByPrimaryKey(WebsiteUserSubscription row);
}
