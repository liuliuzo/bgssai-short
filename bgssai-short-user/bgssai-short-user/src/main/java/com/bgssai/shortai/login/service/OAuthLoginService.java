package com.bgssai.shortai.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.login.dto.req.ReqOAuthCallback;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.login.oauth.OAuthProvider;
import com.bgssai.shortai.login.oauth.OAuthProviderFactory;
import com.bgssai.shortai.login.oauth.OAuthStateUtil;
import com.bgssai.shortai.login.oauth.OAuthUserInfo;
import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.OauthBinding;
import com.bgssai.shortai.model.OauthBindingExample;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.utils.JwtUtil;

@Service
public class OAuthLoginService {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginService.class);
    private static final String USER_ROLE = "USER";

    @Autowired private OAuthStateUtil oauthStateUtil;
    @Autowired private OAuthProviderFactory oauthProviderFactory;
    @Autowired private OauthBindingMapper oauthBindingMapper;
    @Autowired private SysUserMapper sysUserMapper;

    public RespUserLogin loginOrRegister(ReqOAuthCallback req) {
        oauthStateUtil.verify(req.getState(), req.getPlatform());
        OAuthProvider provider = oauthProviderFactory.get(req.getPlatform());
        OAuthUserInfo userInfo = provider.fetchUserInfo(req.getCode());

        OauthBinding binding = findBinding(req.getPlatform(), userInfo.getOpenId());
        if (binding != null) {
            SysUser user = findUser(binding.getUserId());
            log.info("三方授权登录命中已绑定账号 platform={} userId={}", req.getPlatform(), binding.getUserId());
            return withNewUser(issue(user), Boolean.FALSE);
        }

        SysUser user = provision(syntheticUserName(req.getPlatform()), userInfo.getNickname());
        OauthBinding newBinding = new OauthBinding();
        newBinding.setBindingId(OAuthIds.next("ob-"));
        newBinding.setPlatform(req.getPlatform());
        newBinding.setOpenId(userInfo.getOpenId());
        newBinding.setUnionId(userInfo.getUnionId());
        newBinding.setUserId(user.getUserId());
        newBinding.setNickname(userInfo.getNickname());
        newBinding.setAvatarUrl(userInfo.getAvatarUrl());
        newBinding.setDelFlag(Boolean.FALSE);
        newBinding.setCreator(user.getUserId());
        newBinding.setModifier(user.getUserId());
        try {
            oauthBindingMapper.insertSelective(newBinding);
        } catch (DuplicateKeyException e) {
            OauthBinding existing = findBinding(req.getPlatform(), userInfo.getOpenId());
            if (existing == null) throw e;
            SysUser existingUser = findUser(existing.getUserId());
            log.warn("三方授权登录并发命中唯一索引 platform={} userId={}", req.getPlatform(), existing.getUserId());
            return withNewUser(issue(existingUser), Boolean.FALSE);
        }

        log.info("三方授权登录首次建号 platform={} userId={}", req.getPlatform(), user.getUserId());
        return withNewUser(issue(user), Boolean.TRUE);
    }

    private static RespUserLogin withNewUser(RespUserLogin resp, Boolean newUser) {
        resp.setNewUser(newUser);
        return resp;
    }

    private OauthBinding findBinding(String platform, String openId) {
        OauthBindingExample ex = new OauthBindingExample();
        ex.createCriteria().andPlatformEqualTo(platform).andOpenIdEqualTo(openId).andDelFlagEqualTo(Boolean.FALSE);
        return oauthBindingMapper.selectOneByExample(ex);
    }

    private SysUser findUser(String userId) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false);
        return sysUserMapper.selectOneByExample(ex);
    }

    private static String syntheticUserName(String platform) {
        return platform + "_" + OAuthIds.next("").substring(0, 8);
    }

    private RespUserLogin toResp(SysUser user, String token) {
        RespUserLogin resp = new RespUserLogin();
        resp.setJwtToken(token);
        resp.setRoleCode(USER_ROLE);
        resp.setRoleName("用户");
        resp.setUserId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setRealName(user.getRealName());
        return resp;
    }

    private SysUser provision(String userName, String nickname) {
        String userId = OAuthIds.next("u-");
        SysUser row = new SysUser();
        row.setUserId(userId);
        row.setUserName(userName);
        row.setRealName(nickname);
        row.setOrgCode(USER_ROLE);
        row.setStatus(Boolean.TRUE);
        row.setDelFlag(Boolean.FALSE);
        row.setCreateBy(userId);
        sysUserMapper.insertSelective(row);
        return row;
    }

    private RespUserLogin issue(SysUser user) {
        String token = JwtUtil.generateToken(
                user.getUserId(), user.getRealName(), "01", null, null, USER_ROLE);
        return toResp(user, token);
    }
}
