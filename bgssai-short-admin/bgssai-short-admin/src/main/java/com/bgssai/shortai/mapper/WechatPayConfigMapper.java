package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WechatPayConfig;
import com.bgssai.shortai.model.WechatPayConfigExample;

@Mapper
public interface WechatPayConfigMapper {
    long countByExample(WechatPayConfigExample example);

    int deleteByExample(WechatPayConfigExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WechatPayConfig row);

    int insertSelective(WechatPayConfig row);

    List<WechatPayConfig> selectByExample(WechatPayConfigExample example);

    WechatPayConfig selectOneByExample(WechatPayConfigExample example);

    WechatPayConfig selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WechatPayConfig row, @Param("example") WechatPayConfigExample example);

    int updateByExample(@Param("row") WechatPayConfig row, @Param("example") WechatPayConfigExample example);

    int updateByPrimaryKeySelective(WechatPayConfig row);

    int updateByPrimaryKey(WechatPayConfig row);
}
