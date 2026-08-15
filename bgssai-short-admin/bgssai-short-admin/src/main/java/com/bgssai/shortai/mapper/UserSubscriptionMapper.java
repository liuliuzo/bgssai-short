package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserSubscription;
import com.bgssai.shortai.model.UserSubscriptionExample;

@Mapper
public interface UserSubscriptionMapper {
    long countByExample(UserSubscriptionExample example);

    int deleteByExample(UserSubscriptionExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserSubscription row);

    int insertSelective(UserSubscription row);

    List<UserSubscription> selectByExample(UserSubscriptionExample example);

    UserSubscription selectOneByExample(UserSubscriptionExample example);

    UserSubscription selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserSubscription row, @Param("example") UserSubscriptionExample example);

    int updateByExample(@Param("row") UserSubscription row, @Param("example") UserSubscriptionExample example);

    int updateByPrimaryKeySelective(UserSubscription row);

    int updateByPrimaryKey(UserSubscription row);
}
