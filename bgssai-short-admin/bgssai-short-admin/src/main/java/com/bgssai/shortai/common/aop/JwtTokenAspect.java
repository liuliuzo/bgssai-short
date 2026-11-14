package com.bgssai.shortai.common.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.beans.factory.annotation.Autowired;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.common.session.SessionState;
import com.bgssai.shortai.common.session.SingleSessionGuard;
import com.bgssai.shortai.common.logging.MdcSupport;
import com.bgssai.shortai.utils.JwtUtil;

@Aspect
@Component
public class JwtTokenAspect {

    @Autowired
    private SingleSessionGuard singleSessionGuard;

    @Autowired(required = false)
    private com.bgssai.shortai.login.service.LoginSessionSeen loginSessionSeen;

    @Around("execution(public * com.bgssai.shortai..*(..)) && " + "@within(com.bgssai.shortai.common.aop.NeedAop) || "
            + "@annotation(com.bgssai.shortai.common.aop.NeedAop)")
    public Object validateJwtToken(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String jwtToken = request.getHeader("jwtToken");

        // Throw instead of returning ApiResponse: controllers that return PageResponse
        // (or other non-ApiResponse types) would ClassCastException on the CGLIB proxy
        // and surface as system 500. GlobalExceptionHandler maps this to code 2002.
        if (jwtToken == null || !JwtUtil.validateToken(jwtToken)) {
            throw new UnauthorizedException();
        }

        String userId = JwtUtil.getUserIdFromToken(jwtToken);
        String sessionId = JwtUtil.getSessionIdFromToken(jwtToken);

        // 验签通过之后、进入业务之前比对单会话。三态而非布尔：只有「sid 两侧都在且不相等」
        // 才是被顶下线（2003）；sid 缺失 / 账号已逻辑删除 / 已服务端登出一律回既有失效码（2002）。
        SessionState state = singleSessionGuard.check(userId, sessionId);
        if (state == SessionState.SUPERSEDED) {
            throw new BizException(SingleSessionGuard.SESSION_KICKED_CODE,
                    SingleSessionGuard.SESSION_KICKED_MESSAGE);
        }
        if (state != SessionState.VALID) {
            throw new UnauthorizedException();
        }

        String realName = JwtUtil.getRealNameFromToken(jwtToken);
        String accountType = JwtUtil.getUserAccountType(jwtToken);
        String companyCode = JwtUtil.getCompanyCodeFromToken(jwtToken);
        String companyName = JwtUtil.getCompanyNameFromToken(jwtToken);
        String role = JwtUtil.getRoleFromToken(jwtToken);
        UserContext.setUserId(userId);
        UserContext.setRealName(realName);
        UserContext.setUserAccountType(accountType);
        UserContext.setUserCompanyCode(companyCode);
        UserContext.setUserCompanyName(companyName);
        UserContext.setUserRole(role);
        UserContext.setSessionId(sessionId);
        if (loginSessionSeen != null) {
            loginSessionSeen.touch(sessionId);
        }
        MdcSupport.putUserContext(userId, companyCode);

        try {
            return joinPoint.proceed();
        } finally {
            MdcSupport.clearUserContext();
            UserContext.clear();
        }
    }
}
