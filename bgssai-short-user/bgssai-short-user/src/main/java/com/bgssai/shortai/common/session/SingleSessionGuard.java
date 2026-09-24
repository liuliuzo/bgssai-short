package com.bgssai.shortai.common.session;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.common.exception.UnauthorizedException;

/** Database-backed last-login-wins check; no process-local session cache. */
@Component
public class SingleSessionGuard {
    public static final String SESSION_KICKED_CODE = "2003";
    public static final String SESSION_KICKED_MESSAGE = "账号已在其他设备登录";
    private final SysUserMapper users;
    public SingleSessionGuard(SysUserMapper users) { this.users = users; }
    public String newSessionId() { return UUID.randomUUID().toString().replace("-", ""); }
    public int bindSession(String userId, String sessionId) {
        SysUser patch = new SysUser();
        patch.setCurrentSessionId(sessionId);
        SysUserExample query = new SysUserExample();
        query.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false).andStatusEqualTo(true);
        int changed = users.updateByExampleSelective(patch, query);
        if (changed != 1) throw new UnauthorizedException();
        return changed;
    }
    public SessionState check(String userId, String sessionId) {
        if (userId == null || userId.isBlank() || sessionId == null || sessionId.isBlank()) return SessionState.EXPIRED;
        SysUserExample query = new SysUserExample();
        query.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false).andStatusEqualTo(true);
        SysUser user = users.selectOneByExample(query);
        if (user == null || Boolean.TRUE.equals(user.getDelFlag()) || !Boolean.TRUE.equals(user.getStatus())
                || user.getCurrentSessionId() == null || user.getCurrentSessionId().isBlank()) return SessionState.EXPIRED;
        return sessionId.equals(user.getCurrentSessionId()) ? SessionState.VALID : SessionState.SUPERSEDED;
    }
}
