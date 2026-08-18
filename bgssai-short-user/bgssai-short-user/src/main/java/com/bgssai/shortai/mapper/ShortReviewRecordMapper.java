package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortReviewRecord;
import com.bgssai.shortai.model.ShortReviewRecordExample;

@Mapper
public interface ShortReviewRecordMapper {
    long countByExample(ShortReviewRecordExample example);
    int insert(ShortReviewRecord row);
    int insertSelective(ShortReviewRecord row);
    List<ShortReviewRecord> selectByExample(ShortReviewRecordExample example);
    ShortReviewRecord selectOneByExample(ShortReviewRecordExample example);
    ShortReviewRecord selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortReviewRecord row, @Param("example") ShortReviewRecordExample example);
    int updateByPrimaryKeySelective(ShortReviewRecord row);
}
