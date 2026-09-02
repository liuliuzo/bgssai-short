package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteUser;
import com.bgssai.shortai.model.WebsiteUserExample;

@Mapper
public interface WebsiteUserMapper {
    long countByExample(WebsiteUserExample example);

    int deleteByExample(WebsiteUserExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteUser row);

    int insertSelective(WebsiteUser row);

    List<WebsiteUser> selectByExample(WebsiteUserExample example);

    WebsiteUser selectOneByExample(WebsiteUserExample example);

    WebsiteUser selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteUser row, @Param("example") WebsiteUserExample example);

    int updateByExample(@Param("row") WebsiteUser row, @Param("example") WebsiteUserExample example);

    int updateByPrimaryKeySelective(WebsiteUser row);

    int updateByPrimaryKey(WebsiteUser row);
}
