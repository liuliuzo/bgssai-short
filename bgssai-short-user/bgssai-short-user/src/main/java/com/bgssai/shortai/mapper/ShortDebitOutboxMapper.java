package com.bgssai.shortai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.bgssai.shortai.model.ShortDebitOutboxEntry;
import com.bgssai.shortai.model.ShortDebitOutboxExample;

@Mapper
public interface ShortDebitOutboxMapper {
    int enqueue(@Param("id") String id, @Param("userId") String userId, @Param("amount") int amount);
    List<ShortDebitOutboxEntry> pending(ShortDebitOutboxExample example);
    int claim(@Param("id") String id);
    int acknowledge(@Param("id") String id);
    int retry(@Param("id") String id, @Param("delay") int delay);
}
