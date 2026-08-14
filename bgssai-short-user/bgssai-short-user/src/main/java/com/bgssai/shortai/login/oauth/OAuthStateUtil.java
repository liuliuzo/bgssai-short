package com.bgssai.shortai.login.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 三方授权登录的 CSRF {@code state} 令牌：自包含签名值，不落库、不查表。
 *
 * <p>state 只是一次性防跨站请求伪造的短时值，用不着像验证码那样持久化查询——仿 {@link
 * com.bgssai.voiceunion.utils.JwtUtil} 的 HMAC 签名方式，复用同一把 {@code jwt.secret}
 * （与业务登录 JWT 用途不同但密钥可共用，仓库已有「一套密钥多处签名」的先例）。10 分钟有效期，
 * 校验失败一律 {@code 4401}，不区分"过期"还是"签名不对"（不向调用方暴露判别信息）。</p>
 */
@Component
public class OAuthStateUtil {

    private static final Logger log = LoggerFactory.getLogger(OAuthStateUtil.class);
    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    @Value("${jwt.secret:}")
    private String secret;

    public String issue(String platform) {
        Date now = new Date();
        return Jwts.builder()
                .claim("platform", platform)
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_MILLIS))
                .signWith(key())
                .compact();
    }

    /** 校验签名、过期时间与 platform 是否与发起时一致；失败统一抛 4401。 */
    public void verify(String state, String expectedPlatform) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(state).getPayload();
            String platform = claims.get("platform", String.class);
            if (!expectedPlatform.equals(platform)) {
                throw new BizException("4401", "登录会话已过期，请重新授权");
            }
        } catch (BizException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[OAuthStateUtil] state 校验失败 platform={} reason={}", expectedPlatform, e.getClass().getSimpleName());
            throw new BizException("4401", "登录会话已过期，请重新授权");
        }
    }

    private SecretKey key() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("jwt.secret not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
