package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserAiProviderKey;
import com.bgssai.shortai.model.UserAiProviderKeyExample;

@Mapper
public interface UserAiProviderKeyMapper {
    long countByExample(UserAiProviderKeyExample example);

    int deleteByExample(UserAiProviderKeyExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserAiProviderKey row);

    int insertSelective(UserAiProviderKey row);

    List<UserAiProviderKey> selectByExample(UserAiProviderKeyExample example);

    UserAiProviderKey selectOneByExample(UserAiProviderKeyExample example);

    UserAiProviderKey selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserAiProviderKey row, @Param("example") UserAiProviderKeyExample example);

    int updateByExample(@Param("row") UserAiProviderKey row, @Param("example") UserAiProviderKeyExample example);

    int updateByPrimaryKeySelective(UserAiProviderKey row);

    int updateByPrimaryKey(UserAiProviderKey row);
}
