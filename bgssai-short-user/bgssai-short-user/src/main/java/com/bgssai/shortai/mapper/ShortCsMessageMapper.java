package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bgssai.shortai.model.ShortCsMessage;
import com.bgssai.shortai.model.ShortCsMessageExample;

@Mapper
public interface ShortCsMessageMapper {
    long countByExample(ShortCsMessageExample example);
    int insertSelective(ShortCsMessage row);
    List<ShortCsMessage> selectByExample(ShortCsMessageExample example);
    ShortCsMessage selectOneByExample(ShortCsMessageExample example);
    ShortCsMessage selectByPrimaryKey(Long id);
    int updateByPrimaryKeySelective(ShortCsMessage row);
}
