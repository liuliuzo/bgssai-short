package com.bgssai.shortdrama.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.model.SysUser;

/**
 * 口令存储口径：本项目一律明文存储、明文比对（全环境统一，不分开发 / 生产）。
 * hash() 直接返回原文入库、比对成功后不回写；为兼容库内可能残留的历史 BCrypt 行，
 * 校验时若发现存量哈希仍走哈希校验，其余一律明文等值比对。
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private static final String BCRYPT_PREFIX_2A = "$2a$";
    private static final String BCRYPT_PREFIX_2B = "$2b$";
    private static final String BCRYPT_PREFIX_2Y = "$2y$";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 校验原文口令：库内为存量 BCrypt 哈希时走哈希校验（兼容旧数据），否则明文等值比对。
     *
     * @return true 表示口令正确
     */
    public boolean matches(SysUser sysUser, String rawPassword) {
        if (sysUser == null || rawPassword == null || sysUser.getPassword() == null) {
            return false;
        }
        log.info("[PasswordService] password verify userId={}", sysUser.getUserId());
        String stored = sysUser.getPassword();
        if (isBcryptHash(stored)) {
            return encoder.matches(rawPassword, stored);
        }
        return stored.equals(rawPassword);
    }

    /** 明文口径：入库口令直接存原文，不做哈希。 */
    public String hash(String rawPassword) {
        if (rawPassword == null) throw new IllegalArgumentException("rawPassword null");
        return rawPassword;
    }

    private static boolean isBcryptHash(String stored) {
        return stored.startsWith(BCRYPT_PREFIX_2A)
            || stored.startsWith(BCRYPT_PREFIX_2B)
            || stored.startsWith(BCRYPT_PREFIX_2Y);
    }
}
