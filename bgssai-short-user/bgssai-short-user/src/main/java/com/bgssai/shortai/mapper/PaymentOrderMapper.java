package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;

@Mapper
public interface PaymentOrderMapper {
    long countByExample(PaymentOrderExample example);

    int deleteByExample(PaymentOrderExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PaymentOrder row);

    int insertSelective(PaymentOrder row);

    List<PaymentOrder> selectByExample(PaymentOrderExample example);

    PaymentOrder selectOneByExample(PaymentOrderExample example);

    PaymentOrder selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PaymentOrder row, @Param("example") PaymentOrderExample example);

    int updateByExample(@Param("row") PaymentOrder row, @Param("example") PaymentOrderExample example);

    int updateByPrimaryKeySelective(PaymentOrder row);

    int updateByPrimaryKey(PaymentOrder row);
}
