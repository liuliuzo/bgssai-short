package com.bgssai.shortai.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.UserAiSavedCredential;
import com.bgssai.shortai.model.UserAiSavedCredentialExample;

@Mapper
public interface UserAiSavedCredentialMapper {
    void lockOwner(String userId);
    List<UserAiSavedCredential> selectByExample(UserAiSavedCredentialExample example);
    void insert(UserAiSavedCredential credential);
    void update(UserAiSavedCredential credential);
    void clearActive(@Param("userId") String userId, @Param("provider") String provider);
    void delete(@Param("userId") String userId, @Param("id") String id);
    void recordCheck(@Param("credential") UserAiSavedCredential credential, @Param("status") String status, @Param("time") Date time);
}
