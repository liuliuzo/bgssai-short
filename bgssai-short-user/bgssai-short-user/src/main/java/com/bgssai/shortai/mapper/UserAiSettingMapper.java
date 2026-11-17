package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserAiSetting;
import com.bgssai.shortai.model.UserAiSettingExample;

@Mapper
public interface UserAiSettingMapper {
    long countByExample(UserAiSettingExample example);

    int deleteByExample(UserAiSettingExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserAiSetting row);

    int insertSelective(UserAiSetting row);

    List<UserAiSetting> selectByExample(UserAiSettingExample example);

    UserAiSetting selectOneByExample(UserAiSettingExample example);

    UserAiSetting selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserAiSetting row, @Param("example") UserAiSettingExample example);

    int updateByExample(@Param("row") UserAiSetting row, @Param("example") UserAiSettingExample example);

    int updateByPrimaryKeySelective(UserAiSetting row);

    int updateByPrimaryKey(UserAiSetting row);
}
