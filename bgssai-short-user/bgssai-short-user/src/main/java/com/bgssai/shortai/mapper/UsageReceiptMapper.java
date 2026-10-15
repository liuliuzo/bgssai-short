package com.bgssai.shortai.mapper;
import com.bgssai.shortai.billing.UsageReceiptRow;
import com.bgssai.shortai.billing.UsageReceiptExample;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface UsageReceiptMapper {
 int insertSelective(UsageReceiptRow row);
 UsageReceiptRow selectByPrimaryKey(Long id);
 List<UsageReceiptRow> selectByExample(UsageReceiptExample example);
 int updateByPrimaryKeySelective(UsageReceiptRow row);
}
