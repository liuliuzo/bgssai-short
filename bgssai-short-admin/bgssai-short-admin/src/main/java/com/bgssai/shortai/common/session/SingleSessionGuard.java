package com.bgssai.shortai.common.session;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;

/**
 * 单会话闸门：同一账号同时只允许一个有效会话，后登录顶掉先登录（last-login-wins）。
 *
 * <p>机制：登录成功时生成新的 session_id 覆盖写入 sys_user.current_session_id，并作为 JWT 的
 * sid claim 签发；每个需鉴权的请求在验签通过之后、进入业务之前比对两者，不相等即判定被顶下线。
 * 登出把该列置为 SQL NULL，令登出设备上残留的 token 立刻失效，而不是等 JWT 自然过期。</p>
 *
 * <p>sys_user.current_session_id 是唯一真值，前面不放任何缓存或中间件。</p>
 */
@Component
public class SingleSessionGuard {

    /** 被顶下线的统一错误码，全产品线逐字一致。 */
    public static final String SESSION_KICKED_CODE = "2003";

    /** 与 SESSION_KICKED_CODE 配对的固定文案。 */
    public static final String SESSION_KICKED_MESSAGE = "账号已在其他设备登录";

    /**
     * 本仓既有的 token 失效码。token 不带 sid（升级前签发）、账号已被逻辑删除、已服务端登出等
     * 「没有别人顶你」的情形一律回它，不得混用 2003（详见 SessionState.EXPIRED）。
     */
    public static final String SESSION_EXPIRED_CODE = "2002";

    /** 与 SESSION_EXPIRED_CODE 配对的既有文案。 */
    public static final String SESSION_EXPIRED_MESSAGE = "token validation failed";

    private static final Logger log = LoggerFactory.getLogger(SingleSessionGuard.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    /** 生成新的会话标识。UUID 去掉连字符共 32 字符，落在 varchar(64) 内。 */
    public String newSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 登录成功后覆盖写入会话标识，此前签发的所有 token 就此失效。
     *
     * <p>只写 current_session_id 一列（selective 更新），不整行回写，避免与并发的资料编辑
     * 之间出现读-改-写覆盖窗口。</p>
     */
    public int bindSession(String userId, String sessionId) {
        SysUser record = new SysUser();
        record.setCurrentSessionId(sessionId);
        SysUserExample example = new SysUserExample();
        example.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false);
        int rows = sysUserMapper.updateByExampleSelective(record, example);
        log.info("[SingleSessionGuard] session bound userId={} rows={}", userId, rows);
        return rows;
    }

    /**
     * 服务端登出：把 current_session_id 置为 SQL NULL。
     *
     * <p>只写这一列，走 {@code clearCurrentSessionIdByExample}——selective 语句以
     * {@code <if test="x != null">} 守卫、写不进 NULL，而非选择性语句要整行回写。
     * 幂等：重复登出或会话早已被顶掉时返回 0，不报错。</p>
     */
    public int clearSession(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0;
        }
        SysUserExample example = new SysUserExample();
        example.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false);
        int rows = sysUserMapper.clearCurrentSessionIdByExample(example);
        log.info("[SingleSessionGuard] session cleared userId={} rows={}", userId, rows);
        return rows;
    }

    /**
     * 比对 token 携带的 sid 与库里的 current_session_id。
     *
     * @param userId    token subject 里的业务用户标识
     * @param sessionId token 的 sid claim；本特性发布前签发的旧 token 为 null
     * @return VALID / EXPIRED / SUPERSEDED，语义见 {@link SessionState}
     */
    public SessionState check(String userId, String sessionId) {
        if (userId == null || userId.isEmpty()) {
            return SessionState.EXPIRED;
        }
        if (sessionId == null || sessionId.isEmpty()) {
            // 升级发布那一刻所有在线用户手里的旧 token 都没有 sid。把「sid 为空」判成失配
            // 会让他们收到「账号已在其他设备登录」——与事实不符。按既有 token 失效码处理。
            log.info("[SingleSessionGuard] token without sid claim userId={}", userId);
            return SessionState.EXPIRED;
        }
        SysUserExample example = new SysUserExample();
        example.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(false);
        // 本仓 SysUserExample 尚未具备 Standards §2 要求的 selectColumns 扩展，
        // Base_Column_List 仍是 "*"，这里只能取整行。补齐按需列查询后应改回只取
        // SESSION_ID_COLUMN 一列——每个需鉴权请求都会走到这里，多取的列是白付的 IO 与暴露面。
        SysUser user = sysUserMapper.selectOneByExample(example);
        if (user == null) {
            log.warn("[SingleSessionGuard] account not found or deleted userId={}", userId);
            return SessionState.EXPIRED;
        }
        String current = user.getCurrentSessionId();
        if (current == null || current.isEmpty()) {
            // 已服务端登出或从未绑定：没有别的设备持有本账号，报 2003 就是假话。
            return SessionState.EXPIRED;
        }
        return current.equals(sessionId) ? SessionState.VALID : SessionState.SUPERSEDED;
    }
}
