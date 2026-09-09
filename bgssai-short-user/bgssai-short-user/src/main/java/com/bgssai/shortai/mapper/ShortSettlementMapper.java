package com.bgssai.shortai.mapper;
import org.apache.ibatis.annotations.*;
@Mapper
public interface ShortSettlementMapper {
    @Update("UPDATE short_work SET cost_spent_cent = COALESCE(cost_spent_cent, 0) + #{cost}, updated_time = NOW() WHERE work_id = #{workId} AND user_id = #{userId} AND deleted_flag = 0")
    int addWorkCost(@Param("workId") String workId, @Param("userId") String userId, @Param("cost") int cost);
}
