package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bgssai.shortai.model.ShortCsTicket;
import com.bgssai.shortai.model.ShortCsTicketExample;

@Mapper
public interface ShortCsTicketMapper {
    long countByExample(ShortCsTicketExample example);
    int insertSelective(ShortCsTicket row);
    List<ShortCsTicket> selectByExample(ShortCsTicketExample example);
    ShortCsTicket selectOneByExample(ShortCsTicketExample example);
    ShortCsTicket selectByPrimaryKey(Long id);
    int updateByPrimaryKeySelective(ShortCsTicket row);
}
