package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bgssai.shortai.model.McpAccessToken;
import com.bgssai.shortai.model.McpAccessTokenExample;

@Mapper
public interface McpAccessTokenMapper {
    long countByExample(McpAccessTokenExample example);

    int insertSelective(McpAccessToken row);

    List<McpAccessToken> selectByExample(McpAccessTokenExample example);

    McpAccessToken selectOneByExample(McpAccessTokenExample example);

    McpAccessToken selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(McpAccessToken row);
}
