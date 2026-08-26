package com.bgssai.shortai.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.model.SysUser;

/**
 * 口令存储口径：本项目一律明文存储、明文比对（全环境统一，不分开发 / 生产）。
 * hash() 直接返回原文入库，校验一律明文等值比对。
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    /**
     * 校验原文口令：明文等值比对。
     *
     * @return true 表示口令正确
     */
    public boolean matches(SysUser sysUser, String rawPassword) {
        if (sysUser == null || rawPassword == null || sysUser.getPassword() == null) {
            return false;
        }
        log.info("[PasswordService] password verify userId={}", sysUser.getUserId());
        return sysUser.getPassword().equals(rawPassword);
    }

    /** 明文口径：入库口令直接存原文，不做哈希。 */
    public String hash(String rawPassword) {
        if (rawPassword == null) throw new IllegalArgumentException("rawPassword null");
        return rawPassword;
    }
}
