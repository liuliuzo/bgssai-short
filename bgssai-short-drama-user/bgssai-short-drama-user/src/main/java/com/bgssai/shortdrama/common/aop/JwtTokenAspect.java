package com.bgssai.shortdrama.common.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.exception.CommonErrorCode;
import com.bgssai.shortdrama.common.logging.MdcSupport;
import com.bgssai.shortdrama.utils.JwtUtil;

@Aspect
@Component
public class JwtTokenAspect {

    @Around("execution(public * com.bgssai.shortdrama..*(..)) && " + "@within(com.bgssai.shortdrama.common.aop.NeedAop) || "
            + "@annotation(com.bgssai.shortdrama.common.aop.NeedAop)")
    public Object validateJwtToken(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String jwtToken = request.getHeader("jwtToken");

        if (jwtToken == null || !JwtUtil.validateToken(jwtToken)) {
            ApiResponse response = new ApiResponse();
            response.setCode(CommonErrorCode.UNAUTHORIZED.code());
            response.setMessage(CommonErrorCode.UNAUTHORIZED.message());
            response.setSuccess(false);
            return response;
        }

        String userId = JwtUtil.getUserIdFromToken(jwtToken);
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
        MdcSupport.putUserContext(userId, companyCode);

        try {
            return joinPoint.proceed();
        } finally {
            MdcSupport.clearUserContext();
            UserContext.clear();
        }
    }
}
