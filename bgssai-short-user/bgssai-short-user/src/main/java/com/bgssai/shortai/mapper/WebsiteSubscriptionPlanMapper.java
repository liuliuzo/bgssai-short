package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteSubscriptionPlan;
import com.bgssai.shortai.model.WebsiteSubscriptionPlanExample;

@Mapper
public interface WebsiteSubscriptionPlanMapper {
    long countByExample(WebsiteSubscriptionPlanExample example);

    int deleteByExample(WebsiteSubscriptionPlanExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteSubscriptionPlan row);

    int insertSelective(WebsiteSubscriptionPlan row);

    List<WebsiteSubscriptionPlan> selectByExample(WebsiteSubscriptionPlanExample example);

    WebsiteSubscriptionPlan selectOneByExample(WebsiteSubscriptionPlanExample example);

    WebsiteSubscriptionPlan selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteSubscriptionPlan row, @Param("example") WebsiteSubscriptionPlanExample example);

    int updateByExample(@Param("row") WebsiteSubscriptionPlan row, @Param("example") WebsiteSubscriptionPlanExample example);

    int updateByPrimaryKeySelective(WebsiteSubscriptionPlan row);

    int updateByPrimaryKey(WebsiteSubscriptionPlan row);
}
