package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteUsageLedger;
import com.bgssai.shortai.model.WebsiteUsageLedgerExample;

@Mapper
public interface WebsiteUsageLedgerMapper {
    long countByExample(WebsiteUsageLedgerExample example);

    int deleteByExample(WebsiteUsageLedgerExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteUsageLedger row);

    int insertSelective(WebsiteUsageLedger row);

    List<WebsiteUsageLedger> selectByExample(WebsiteUsageLedgerExample example);

    WebsiteUsageLedger selectOneByExample(WebsiteUsageLedgerExample example);

    WebsiteUsageLedger selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteUsageLedger row, @Param("example") WebsiteUsageLedgerExample example);

    int updateByExample(@Param("row") WebsiteUsageLedger row, @Param("example") WebsiteUsageLedgerExample example);

    int updateByPrimaryKeySelective(WebsiteUsageLedger row);

    int updateByPrimaryKey(WebsiteUsageLedger row);
}
