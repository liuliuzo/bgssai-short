package com.bgssai.shortai.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility. Configured at startup via JwtConfig @PostConstruct.
 * Secret injected from jwt.secret property (must be >= 32 chars).
 * Uses jjwt 0.12.x API.
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static volatile String SECRET_KEY;

    private static volatile long EXPIRATION_TIME_MS = 86400000L;

    private JwtUtil() {}

    static synchronized void configure(String secret, long expirationMs) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                "jwt.secret not configured — must be set to a random string of at least 32 characters");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "jwt.secret length is less than 32 characters — use `openssl rand -base64 48` to generate");
        }
        SECRET_KEY = secret;
        EXPIRATION_TIME_MS = expirationMs > 0 ? expirationMs : 86400000L;
        log.info("[JwtUtil] configured: secretLen={}, expirationMs={}", secret.length(), EXPIRATION_TIME_MS);
    }

    private static SecretKey key() {
        String secret = SECRET_KEY;
        if (secret == null) {
            throw new IllegalStateException(
                "JwtUtil not initialised — ensure JwtConfig bean is loaded (@PostConstruct calls configure())");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(String userId, String realName, String accountType,
                                       String companyCode, String companyName) {
        return generateToken(userId, realName, accountType, companyCode, companyName, null, null);
    }

    public static String generateToken(String userId, String realName, String accountType,
                                       String companyCode, String companyName, String role) {
        return generateToken(userId, realName, accountType, companyCode, companyName, role, null);
    }

    /**
     * 签发 token，可选携带单会话标识作为 {@code sid} claim。
     *
     * <p>sid 就是登录刚写进 sys_user.current_session_id 的值。每个需鉴权的请求都会比对两者
     * （见 JwtTokenAspect）；同一账号的后一次登录覆盖该列，于是此前签发的所有 token 全部失效
     * （last-login-wins）。传 null 则整个 claim 不出现——本特性发布前签发的 token 正是这个样子，
     * 切面对「缺 sid」必须回 2002 而不是 2003。</p>
     *
     * @param sessionId 单会话标识，最长 64 字符；null 或空串则不带该 claim
     */
    public static String generateToken(String userId, String realName, String accountType,
                                       String companyCode, String companyName, String role,
                                       String sessionId) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS);
        var builder = Jwts.builder()
                          .subject(userId)
                          .claim("realName", realName)
                          .claim("accountType", accountType)
                          .claim("companyCode", companyCode)
                          .claim("companyName", companyName)
                          .expiration(expirationDate)
                          .signWith(key());
        if (role != null && !role.isEmpty()) {
            builder.claim("role", role);
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            builder.claim("sid", sessionId);
        }
        return builder.compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                                        .verifyWith(key())
                                        .build()
                                        .parseSignedClaims(token);
            return !claimsJws.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    public static String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public static String getRealNameFromToken(String token) {
        return getClaims(token).get("realName", String.class);
    }

    public static String getUserAccountType(String token) {
        return getClaims(token).get("accountType", String.class);
    }

    public static String getCompanyCodeFromToken(String token) {
        return getClaims(token).get("companyCode", String.class);
    }

    public static String getCompanyNameFromToken(String token) {
        return getClaims(token).get("companyName", String.class);
    }

    /**
     * 读取 token 里的单会话标识。
     *
     * @return sid claim；本特性发布前签发的 token 返回 null
     */
    public static String getSessionIdFromToken(String token) {
        return getClaims(token).get("sid", String.class);
    }

    public static long expirationMillis() {
        return EXPIRATION_TIME_MS;
    }

    public static String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    private static Claims getClaims(String token) {
        return Jwts.parser()
                   .verifyWith(key())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
}
