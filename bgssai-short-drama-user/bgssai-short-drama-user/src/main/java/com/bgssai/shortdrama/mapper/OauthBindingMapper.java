package com.bgssai.shortdrama.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.bgssai.shortdrama.model.OauthBinding;
import com.bgssai.shortdrama.model.OauthBindingExample;

@Mapper
public interface OauthBindingMapper {
    long countByExample(OauthBindingExample example);
    int deleteByExample(OauthBindingExample example);
    int deleteByPrimaryKey(Long id);
    int insert(OauthBinding row);
    int insertSelective(OauthBinding row);
    List<OauthBinding> selectByExample(OauthBindingExample example);
    OauthBinding selectOneByExample(OauthBindingExample example);
    OauthBinding selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") OauthBinding row, @Param("example") OauthBindingExample example);
    int updateByExample(@Param("row") OauthBinding row, @Param("example") OauthBindingExample example);
    int updateByPrimaryKeySelective(OauthBinding row);
    int updateByPrimaryKey(OauthBinding row);
}
