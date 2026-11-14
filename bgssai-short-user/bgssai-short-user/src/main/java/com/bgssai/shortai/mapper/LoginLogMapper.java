package com.bgssai.shortai.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.LoginLog;
import com.bgssai.shortai.model.LoginLogExample;

@Mapper
public interface LoginLogMapper {
    long countByExample(LoginLogExample example);

    int deleteByExample(LoginLogExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(LoginLog row);

    int insertSelective(LoginLog row);

    List<LoginLog> selectByExample(LoginLogExample example);
    
    LoginLog selectOneByExample(LoginLogExample example);

    LoginLog selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("row") LoginLog row, @Param("example") LoginLogExample example);

    int updateByExample(@Param("row") LoginLog row, @Param("example") LoginLogExample example);

    int updateByPrimaryKeySelective(LoginLog row);

    int updateByPrimaryKey(LoginLog row);

    List<LoginLog> selectRecentSuccess(@Param("userId") String userId, @Param("limit") int limit);

    int touchLastSeen(@Param("sessionId") String sessionId, @Param("seenAt") Date seenAt);

    int markLogout(@Param("sessionId") String sessionId, @Param("logoutAt") Date logoutAt);
}