package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.WebsiteUsageIdempotency;
import com.bgssai.shortai.model.WebsiteUsageIdempotencyExample;

@Mapper
public interface WebsiteUsageIdempotencyMapper {
    long countByExample(WebsiteUsageIdempotencyExample example);

    int deleteByExample(WebsiteUsageIdempotencyExample example);

    int deleteByPrimaryKey(Long id);

    int insert(WebsiteUsageIdempotency row);

    int insertSelective(WebsiteUsageIdempotency row);

    List<WebsiteUsageIdempotency> selectByExample(WebsiteUsageIdempotencyExample example);

    WebsiteUsageIdempotency selectOneByExample(WebsiteUsageIdempotencyExample example);

    WebsiteUsageIdempotency selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") WebsiteUsageIdempotency row, @Param("example") WebsiteUsageIdempotencyExample example);

    int updateByExample(@Param("row") WebsiteUsageIdempotency row, @Param("example") WebsiteUsageIdempotencyExample example);

    int updateByPrimaryKeySelective(WebsiteUsageIdempotency row);

    int updateByPrimaryKey(WebsiteUsageIdempotency row);
}
