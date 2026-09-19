package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bgssai.shortai.model.ShortCsSession;
import com.bgssai.shortai.model.ShortCsSessionExample;

@Mapper
public interface ShortCsSessionMapper {
    long countByExample(ShortCsSessionExample example);
    int insertSelective(ShortCsSession row);
    List<ShortCsSession> selectByExample(ShortCsSessionExample example);
    ShortCsSession selectOneByExample(ShortCsSessionExample example);
    ShortCsSession selectByPrimaryKey(Long id);
    int updateByPrimaryKeySelective(ShortCsSession row);
}
