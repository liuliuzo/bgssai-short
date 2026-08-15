package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;

@Mapper
public interface SubscriptionPlanMapper {
    long countByExample(SubscriptionPlanExample example);

    int deleteByExample(SubscriptionPlanExample example);

    int deleteByPrimaryKey(Long id);

    int insert(SubscriptionPlan row);

    int insertSelective(SubscriptionPlan row);

    List<SubscriptionPlan> selectByExample(SubscriptionPlanExample example);

    SubscriptionPlan selectOneByExample(SubscriptionPlanExample example);

    SubscriptionPlan selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") SubscriptionPlan row, @Param("example") SubscriptionPlanExample example);

    int updateByExample(@Param("row") SubscriptionPlan row, @Param("example") SubscriptionPlanExample example);

    int updateByPrimaryKeySelective(SubscriptionPlan row);

    int updateByPrimaryKey(SubscriptionPlan row);
}
