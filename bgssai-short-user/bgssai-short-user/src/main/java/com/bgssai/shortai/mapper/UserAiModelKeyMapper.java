package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.model.UserAiModelKeyExample;

@Mapper
public interface UserAiModelKeyMapper {
    long countByExample(UserAiModelKeyExample example);

    int deleteByExample(UserAiModelKeyExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserAiModelKey row);

    int insertSelective(UserAiModelKey row);

    List<UserAiModelKey> selectByExample(UserAiModelKeyExample example);

    UserAiModelKey selectOneByExample(UserAiModelKeyExample example);

    UserAiModelKey selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserAiModelKey row, @Param("example") UserAiModelKeyExample example);

    int updateByExample(@Param("row") UserAiModelKey row, @Param("example") UserAiModelKeyExample example);

    int updateByPrimaryKeySelective(UserAiModelKey row);

    int updateByPrimaryKey(UserAiModelKey row);
}
