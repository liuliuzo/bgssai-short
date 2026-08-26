package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.RechargeApply;
import com.bgssai.shortai.model.RechargeApplyExample;

@Mapper
public interface RechargeApplyMapper {
    long countByExample(RechargeApplyExample example);
    int insert(RechargeApply row);
    int insertSelective(RechargeApply row);
    List<RechargeApply> selectByExample(RechargeApplyExample example);
    RechargeApply selectOneByExample(RechargeApplyExample example);
    RechargeApply selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") RechargeApply row, @Param("example") RechargeApplyExample example);
    int updateByPrimaryKeySelective(RechargeApply row);
}
