package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;

@Mapper
public interface SysUserMapper {
    long countByExample(SysUserExample example);

    int deleteByExample(SysUserExample example);

    /**
     * 单会话登出：把匹配行的 current_session_id 置为 SQL NULL，只写这一列。
     *
     * <p>不能用 updateByExampleSelective——它以 {@code <if test="x != null">} 守卫，
     * 传 null 等于不写这一列；也不用非选择性 updateByExample——那要整行回写，
     * 会与并发的资料编辑产生读-改-写覆盖窗口。</p>
     */
    int clearCurrentSessionIdByExample(SysUserExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(SysUser record);

    int insertSelective(SysUser record);

    List<SysUser> selectByExample(SysUserExample example);
    
    SysUser selectOneByExample(SysUserExample example);

    SysUser selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") SysUser record, @Param("example") SysUserExample example);

    int updateByExample(@Param("record") SysUser record, @Param("example") SysUserExample example);

    int updateByPrimaryKeySelective(SysUser record);

    int updateByPrimaryKey(SysUser record);
}