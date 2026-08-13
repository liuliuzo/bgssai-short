package com.bgssai.shortdrama.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.mapper.SysUserMapper;
import com.bgssai.shortdrama.model.SysUser;

/**
 * Password verification with BCrypt soft-migration.
 * Handles both BCrypt-hashed and legacy plaintext passwords.
 * On first plaintext match, upgrades in-place to BCrypt.
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private static final String BCRYPT_PREFIX_2A = "$2a$";
    private static final String BCRYPT_PREFIX_2B = "$2b$";
    private static final String BCRYPT_PREFIX_2Y = "$2y$";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * Verify raw password against stored value.
     * Handles BCrypt hash and legacy plaintext. Upgrades plaintext on success.
     *
     * @return true if password is correct, false otherwise
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
        if (stored.equals(rawPassword)) {
            upgradeToBcrypt(sysUser, rawPassword);
            return true;
        }
        return false;
    }

    public String hash(String rawPassword) {
        if (rawPassword == null) throw new IllegalArgumentException("rawPassword null");
        return encoder.encode(rawPassword);
    }

    private static boolean isBcryptHash(String stored) {
        return stored.startsWith(BCRYPT_PREFIX_2A)
            || stored.startsWith(BCRYPT_PREFIX_2B)
            || stored.startsWith(BCRYPT_PREFIX_2Y);
    }

    private void upgradeToBcrypt(SysUser sysUser, String rawPassword) {
        try {
            SysUser update = new SysUser();
            update.setId(sysUser.getId());
            update.setPassword(encoder.encode(rawPassword));
            sysUserMapper.updateByPrimaryKeySelective(update);
            log.info("[PasswordService] legacy password upgraded to BCrypt userId={}", sysUser.getUserId());
        } catch (Exception e) {
            log.warn("[PasswordService] upgrade failed userId={} err={}", sysUser.getUserId(), e.getMessage());
        }
    }
}
