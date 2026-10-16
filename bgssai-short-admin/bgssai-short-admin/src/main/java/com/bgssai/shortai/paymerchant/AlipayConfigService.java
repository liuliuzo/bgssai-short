package com.bgssai.shortai.paymerchant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.AlipayConfigMapper;
import com.bgssai.shortai.model.AlipayConfig;
import com.bgssai.shortai.model.AlipayConfigExample;
import com.bgssai.shortai.paymerchant.dto.AlipayConfigItem;
import com.bgssai.shortai.paymerchant.dto.PayConfigCollectionActiveResult;
import com.bgssai.shortai.paymerchant.dto.PayConfigEnabledResult;
import com.bgssai.shortai.paymerchant.dto.PayConfigIdResult;
import com.bgssai.shortai.paymerchant.dto.ReqPayConfigId;
import com.bgssai.shortai.paymerchant.dto.ReqSaveAlipayConfig;
import com.bgssai.shortai.paymerchant.dto.ReqSetPayConfigEnabled;

/**
 * Admin CRUD for alipay_config — one row per Alipay merchant, with a manually designated collection
 * merchant. Structurally identical to {@link WechatPayConfigService}; the two stay separate classes
 * because the credential column sets differ and neither channel should be able to break the other
 * (Standards §7: one implementation per channel, no if-else channel dispatch inside a shared class).
 *
 * <p>Write-side invariants PMC-02..PMC-07 and PMC-14 are enforced here exactly as documented in
 * docs/architecture/pay-merchant-config-design.md §3. Credential values are never logged.</p>
 *
 * <p>PMC-14: uk_alipay_config_app_id is a bare single-column unique key, so a soft-deleted row keeps
 * holding its app_id and recreating that number revives the row. Revive is a create, not an edit: the
 * row is rewritten in full so no credential, connectivity state or creation provenance of the previous
 * merchant survives.</p>
 */
@Service
public class AlipayConfigService {

    private static final Logger log = LoggerFactory.getLogger(AlipayConfigService.class);

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String AUDIT_TARGET_TYPE = "ALIPAY_CONFIG";
    private static final String SIGN_TYPE_RSA2 = "RSA2";
    private static final String DEFAULT_CHARSET = "utf-8";
    /** Mirrors the alipay_config.charset column width in sql/DDL.sql. */
    private static final int CHARSET_MAX_LENGTH = 16;
    private static final String DEFAULT_GATEWAY_URL = "https://openapi.alipay.com/gateway.do";
    private static final String SANDBOX_GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    @Autowired
    private AlipayConfigMapper alipayConfigMapper;

    public ApiResponse list() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can list alipay configs");
        }
        log.info("admin list alipay configs");

        AlipayConfigExample example = new AlipayConfigExample();
        example.createCriteria().andDelFlagEqualTo(0);
        example.setOrderByClause("is_collection_active desc, is_enabled desc, id asc");
        List<AlipayConfig> rows = alipayConfigMapper.selectByExample(example);

        SimpleDateFormat fmt = new SimpleDateFormat(TIME_PATTERN);
        List<AlipayConfigItem> items = new ArrayList<>(rows.size());
        String activeAppId = null;
        for (AlipayConfig row : rows) {
            if (Integer.valueOf(1).equals(row.getCollectionActive())) {
                activeAppId = row.getAppId();
            }
            AlipayConfigItem item = new AlipayConfigItem();
            item.setId(row.getId());
            item.setMerchantName(row.getMerchantName());
            item.setAppId(row.getAppId());
            item.setEnabled(Integer.valueOf(1).equals(row.getEnabled()));
            item.setCollectionActive(Integer.valueOf(1).equals(row.getCollectionActive()));
            item.setKeyMode(row.getKeyMode());
            item.setSignType(row.getSignType());
            item.setCharset(row.getCharset());
            item.setGatewayUrl(row.getGatewayUrl());
            item.setNotifyUrl(row.getNotifyUrl());
            item.setReturnUrl(row.getReturnUrl());
            item.setEnvMode(row.getEnvMode());
            item.setAppCertSn(row.getAppCertSn());
            item.setAlipayRootCertSn(row.getAlipayRootCertSn());
            // Secret credentials are echoed masked; blank / masked echo on save keeps the stored value.
            item.setAppPrivateKey(maskSecret(row.getAppPrivateKey()));
            item.setAlipayPublicKey(row.getAlipayPublicKey());
            item.setAppCertPublicKey(row.getAppCertPublicKey());
            item.setAlipayCertPublicKey(row.getAlipayCertPublicKey());
            item.setAlipayRootCert(row.getAlipayRootCert());
            item.setLastTestStatus(row.getLastTestStatus());
            item.setLastTestAt(row.getLastTestAt() == null ? null : fmt.format(row.getLastTestAt()));
            item.setLastTestError(row.getLastTestError());
            item.setGmtCreate(row.getGmtCreate() == null ? null : fmt.format(row.getGmtCreate()));
            item.setGmtModified(row.getGmtModified() == null ? null : fmt.format(row.getGmtModified()));
            item.setModifier(row.getModifier());
            items.add(item);
        }
        log.info("admin list alipay configs done count={} activeAppId={}", items.size(), activeAppId);
        return ok(items);
    }

    @Transactional
    public ApiResponse save(ReqSaveAlipayConfig req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can save alipay configs");
        }
        log.info("admin save alipay config id={} appId={} enabled={}",
                req.getId(), req.getAppId(), req.getEnabled());

        String merchantName = requireText(req.getMerchantName(), "merchant_name", 128);
        String appId = requireText(req.getAppId(), "app_id", 64);
        String notifyUrl = optionalText(req.getNotifyUrl(), "notify_url", 512);
        String returnUrl = optionalText(req.getReturnUrl(), "return_url", 255);

        String signType = trimOrNull(req.getSignType());
        signType = signType == null ? SIGN_TYPE_RSA2 : signType;
        if (!SIGN_TYPE_RSA2.equals(signType)) {
            log.warn("admin save alipay config rejected-sign_type appId={} signType={}", appId, signType);
            throw new BizException("60009", "sign_type only supports RSA2");
        }
        String charset = trimOrNull(req.getCharset());
        charset = charset == null ? DEFAULT_CHARSET : charset;
        // alipay_config.charset is varchar(16): without this check an over-long value reaches the
        // driver and surfaces as a 500 (data truncation) instead of the documented 60009.
        if (charset.length() > CHARSET_MAX_LENGTH) {
            log.warn("admin save alipay config rejected-charset appId={} charsetLength={}",
                    appId, charset.length());
            throw new BizException("60009",
                    "charset must not exceed " + CHARSET_MAX_LENGTH + " characters");
        }
        String keyMode = trimOrNull(req.getKeyMode()) == null ? "PUBLIC_KEY" : req.getKeyMode().trim();
        if (!"PUBLIC_KEY".equals(keyMode) && !"CERT".equals(keyMode)) {
            throw new BizException("60009", "key_mode must be PUBLIC_KEY or CERT");
        }
        String envMode = trimOrNull(req.getEnvMode()) == null ? "PRODUCTION" : req.getEnvMode().trim();
        if (!"SANDBOX".equals(envMode) && !"PRODUCTION".equals(envMode)) {
            throw new BizException("60009", "env_mode must be SANDBOX or PRODUCTION");
        }
        String gatewayUrl = trimOrNull(req.getGatewayUrl());
        gatewayUrl = gatewayUrl == null
                ? ("SANDBOX".equals(envMode) ? SANDBOX_GATEWAY_URL : DEFAULT_GATEWAY_URL)
                : gatewayUrl;
        if (gatewayUrl.length() > 255) {
            throw new BizException("60009", "gateway_url must not exceed 255 characters");
        }
        String appCertSn = optionalText(req.getAppCertSn(), "app_cert_sn", 128);
        String alipayRootCertSn = optionalText(req.getAlipayRootCertSn(), "alipay_root_cert_sn", 255);

        // uk_alipay_config_app_id covers app_id alone, so a soft-deleted row still occupies the
        // number: the lookup must span del_flag=0 and del_flag=1 or the insert below would hit a
        // duplicate-key error the admin cannot resolve from the console.
        AlipayConfigExample dupExample = new AlipayConfigExample();
        dupExample.createCriteria().andAppIdEqualTo(appId);
        AlipayConfig sameNumber = alipayConfigMapper.selectOneByExample(dupExample);
        AlipayConfig revivable = null;
        if (sameNumber != null && !sameNumber.getId().equals(req.getId())) {
            if (!Integer.valueOf(1).equals(sameNumber.getDelFlag())) {
                log.warn("admin save alipay config rejected-duplicate appId={} existingId={}",
                        appId, sameNumber.getId());
                throw new BizException("60014", "app_id already exists: " + appId);
            }
            if (req.getId() != null) {
                log.warn("admin save alipay config rejected-deleted-duplicate id={} appId={} deletedId={}",
                        req.getId(), appId, sameNumber.getId());
                throw new BizException("60014",
                        "app_id belongs to a deleted merchant, recreate it instead of renaming: " + appId);
            }
            revivable = sameNumber;
        }

        String operator = resolveOperator();
        boolean enabled = !Boolean.FALSE.equals(req.getEnabled());
        Date now = new Date();

        AlipayConfig row = new AlipayConfig();
        row.setMerchantName(merchantName);
        row.setAppId(appId);
        row.setEnabled(enabled ? 1 : 0);
        row.setKeyMode(keyMode);
        row.setSignType(signType);
        row.setCharset(charset);
        row.setGatewayUrl(gatewayUrl);
        row.setNotifyUrl(notifyUrl);
        row.setReturnUrl(returnUrl);
        row.setEnvMode(envMode);
        row.setAppCertSn(appCertSn);
        row.setAlipayRootCertSn(alipayRootCertSn);
        row.setModifier(operator);
        row.setGmtModified(now);
        if (!enabled) {
            // PMC-04: a disabled merchant must never remain the collection merchant.
            row.setCollectionActive(0);
        }

        // PMC-07: blank credential fields stay null so the selective mapper keeps the stored value on
        // the edit path; on the revive path below the full updateByPrimaryKey writes those nulls,
        // because reviving is a create.
        List<String> updatedSecrets = new ArrayList<>(2);
        String appPrivateKey = trimOrNull(req.getAppPrivateKey());
        if (appPrivateKey != null) {
            row.setAppPrivateKey(appPrivateKey);
            updatedSecrets.add("app_private_key");
        }
        String alipayPublicKey = trimOrNull(req.getAlipayPublicKey());
        if (alipayPublicKey != null) {
            row.setAlipayPublicKey(alipayPublicKey);
            updatedSecrets.add("alipay_public_key");
        }
        String appCertPublicKey = trimOrNull(req.getAppCertPublicKey());
        if (appCertPublicKey != null) {
            row.setAppCertPublicKey(appCertPublicKey);
            updatedSecrets.add("app_cert_public_key");
        }
        String alipayCertPublicKey = trimOrNull(req.getAlipayCertPublicKey());
        if (alipayCertPublicKey != null) {
            row.setAlipayCertPublicKey(alipayCertPublicKey);
            updatedSecrets.add("alipay_cert_public_key");
        }
        String alipayRootCert = trimOrNull(req.getAlipayRootCert());
        if (alipayRootCert != null) {
            row.setAlipayRootCert(alipayRootCert);
            updatedSecrets.add("alipay_root_cert");
        }

        Long targetId;
        String action;
        if (req.getId() == null && revivable != null) {
            // The number is still held by a soft-deleted row: reuse that row's id instead of inserting
            // a second one, which the single-column unique key would reject. Reviving is a CREATE, so
            // the row is rewritten wholesale with updateByPrimaryKey (never the selective variant):
            // every business column this request did not supply is written as null / its default, and
            // nothing of the previous merchant — credentials above all, which round-trip in plaintext —
            // may leak into the new one. The result must be indistinguishable from a fresh insert.
            row.setId(revivable.getId());
            row.setCollectionActive(0);
            row.setDelFlag(0);
            // Connectivity state belongs to the previous merchant: this one has never been tested.
            row.setLastTestStatus("UNTESTED");
            row.setLastTestAt(null);
            row.setLastTestError(null);
            // Creation provenance is this operation's, not the deleted row's.
            row.setCreator(operator);
            row.setGmtCreate(now);
            alipayConfigMapper.updateByPrimaryKey(row);
            targetId = revivable.getId();
            action = "CREATE_ALIPAY_CONFIG";
            log.info("admin save alipay config revived soft-deleted row id={} appId={}",
                    targetId, appId);
        } else if (req.getId() == null) {
            row.setCollectionActive(0);
            row.setDelFlag(0);
            row.setCreator(operator);
            alipayConfigMapper.insertSelective(row);
            targetId = row.getId();
            action = "CREATE_ALIPAY_CONFIG";
        } else {
            AlipayConfig existing = requireRow(req.getId());
            row.setId(existing.getId());
            alipayConfigMapper.updateByPrimaryKeySelective(row);
            targetId = existing.getId();
            action = "UPDATE_ALIPAY_CONFIG";
        }

        log.info("admin save alipay config done id={} appId={} enabled={} secretsUpdated={}",
                targetId, appId, enabled, updatedSecrets.size());
        /* no ops audit table in short */
        return ok(new PayConfigIdResult(targetId));
    }

    @Transactional
    public ApiResponse setEnabled(ReqSetPayConfigEnabled req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can change alipay config state");
        }
        log.info("admin set alipay config enabled id={} enabled={}", req.getId(), req.getEnabled());
        if (req.getEnabled() == null) {
            throw new BizException("60009", "is_enabled must not be null");
        }
        AlipayConfig existing = requireRow(req.getId());
        boolean enabled = Boolean.TRUE.equals(req.getEnabled());
        boolean clearedCollection = !enabled && Integer.valueOf(1).equals(existing.getCollectionActive());

        AlipayConfig update = new AlipayConfig();
        update.setId(existing.getId());
        update.setEnabled(enabled ? 1 : 0);
        if (!enabled) {
            update.setCollectionActive(0);
        }
        update.setModifier(resolveOperator());
        update.setGmtModified(new Date());
        alipayConfigMapper.updateByPrimaryKeySelective(update);

        if (clearedCollection) {
            log.warn("admin disabled the active alipay collection merchant id={} appId={};"
                    + " channel now has no collection merchant", existing.getId(), existing.getAppId());
        }
        log.info("admin set alipay config enabled done id={} appId={} enabled={} clearedCollection={}",
                existing.getId(), existing.getAppId(), enabled, clearedCollection);
        /* no ops audit table in short */
        return ok(new PayConfigEnabledResult(existing.getId(), enabled,
                enabled ? Integer.valueOf(1).equals(existing.getCollectionActive()) : Boolean.FALSE));
    }

    @Transactional
    public ApiResponse setCollectionActive(ReqPayConfigId req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can designate the alipay collection merchant");
        }
        log.info("admin set alipay collection merchant id={}", req.getId());
        AlipayConfig existing = requireRow(req.getId());
        if (!Integer.valueOf(1).equals(existing.getEnabled())) {
            log.warn("admin set alipay collection merchant rejected-disabled id={} appId={}",
                    existing.getId(), existing.getAppId());
            throw new BizException("60015",
                    "a disabled merchant cannot be designated as the collection merchant");
        }

        String operator = resolveOperator();
        Date now = new Date();

        AlipayConfig clear = new AlipayConfig();
        clear.setCollectionActive(0);
        clear.setModifier(operator);
        clear.setGmtModified(now);
        AlipayConfigExample clearExample = new AlipayConfigExample();
        clearExample.createCriteria()
                .andDelFlagEqualTo(0)
                .andCollectionActiveEqualTo(1);
        int cleared = alipayConfigMapper.updateByExampleSelective(clear, clearExample);

        AlipayConfig update = new AlipayConfig();
        update.setId(existing.getId());
        update.setCollectionActive(1);
        update.setModifier(operator);
        update.setGmtModified(now);
        alipayConfigMapper.updateByPrimaryKeySelective(update);

        log.info("admin set alipay collection merchant done id={} appId={} previousCleared={}",
                existing.getId(), existing.getAppId(), cleared);
        /* no ops audit table in short */
        return ok(new PayConfigCollectionActiveResult(existing.getId(), Boolean.TRUE));
    }

    @Transactional
    public ApiResponse delete(ReqPayConfigId req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can delete alipay configs");
        }
        log.info("admin delete alipay config id={}", req.getId());
        AlipayConfig existing = requireRow(req.getId());
        if (Integer.valueOf(1).equals(existing.getCollectionActive())) {
            log.warn("admin delete alipay config rejected-active id={} appId={}",
                    existing.getId(), existing.getAppId());
            throw new BizException("60015",
                    "the current collection merchant cannot be deleted; switch or disable it first");
        }

        AlipayConfig update = new AlipayConfig();
        update.setId(existing.getId());
        update.setDelFlag(1);
        update.setModifier(resolveOperator());
        update.setGmtModified(new Date());
        alipayConfigMapper.updateByPrimaryKeySelective(update);

        log.info("admin delete alipay config done id={} appId={}", existing.getId(), existing.getAppId());
        /* no ops audit table in short */
        return ok(new PayConfigIdResult(existing.getId()));
    }

    private AlipayConfig requireRow(Long id) {
        if (id == null) {
            throw new BizException("60009", "id must not be null");
        }
        AlipayConfig row = alipayConfigMapper.selectByPrimaryKey(id);
        if (row == null || Integer.valueOf(1).equals(row.getDelFlag())) {
            throw new BizException("60009", "alipay config not found: " + id);
        }
        return row;
    }

    private static String auditDetail(String merchantName, boolean enabled, List<String> updatedSecrets) {
        return "merchant_name=" + merchantName + " is_enabled=" + enabled
                + " updated_secret_fields=" + (updatedSecrets.isEmpty() ? "none" : String.join("/", updatedSecrets));
    }

    private static String resolveOperator() {
        String userId = UserContext.getUserId();
        return (userId != null && !userId.isBlank()) ? userId : "admin";
    }

    private static ApiResponse ok(Object result) {
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setResult(result);
        return resp;
    }

    private static String requireText(String value, String field, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BizException("60009", field + " must not be blank");
        }
        if (trimmed.length() > maxLength) {
            throw new BizException("60009", field + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value, String field, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > maxLength) {
            throw new BizException("60009", field + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    /** Console echo: •••• + last 4 chars (same as bgssai-magic #155). */
    static String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String v = value.strip();
        return "••••" + (v.length() > 4 ? v.substring(v.length() - 4) : "");
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        // A masked echo sent back unchanged means "keep the stored value", same as blank.
        if (trimmed.startsWith("••••")) {
            return null;
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
