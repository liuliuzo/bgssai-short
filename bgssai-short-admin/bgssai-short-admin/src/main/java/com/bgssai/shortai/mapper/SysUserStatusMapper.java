package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.production.dto.CreatorRow;

@Mapper
public interface SysUserStatusMapper {

    int updateStatus(@Param("userId") String userId, @Param("status") int status, @Param("updateBy") String updateBy);

    List<CreatorRow> selectCreators(@Param("keyword") String keyword, @Param("status") Integer status);
}
