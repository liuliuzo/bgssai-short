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
import com.bgssai.shortai.mapper.WechatPayConfigMapper;
import com.bgssai.shortai.model.WechatPayConfig;
import com.bgssai.shortai.model.WechatPayConfigExample;
import com.bgssai.shortai.paymerchant.dto.PayConfigCollectionActiveResult;
import com.bgssai.shortai.paymerchant.dto.PayConfigEnabledResult;
import com.bgssai.shortai.paymerchant.dto.PayConfigIdResult;
import com.bgssai.shortai.paymerchant.dto.ReqPayConfigId;
import com.bgssai.shortai.paymerchant.dto.ReqSaveWechatPayConfig;
import com.bgssai.shortai.paymerchant.dto.ReqSetPayConfigEnabled;
import com.bgssai.shortai.paymerchant.dto.WechatPayConfigItem;

/**
 * Admin CRUD for wechat_pay_config — one row per WeChat Pay merchant, with a manually designated
 * collection merchant (PRD docs/feature/pay-merchant-config.md, R62).
 *
 * <p>Write-side invariants enforced here (see design doc §3):</p>
 * <ul>
 *   <li>PMC-02: at most one row per channel has is_collection_active=1 (cleared then set, one transaction).</li>
 *   <li>PMC-03: a disabled merchant cannot be designated as the collection merchant.</li>
 *   <li>PMC-04: disabling a merchant also clears its collection flag, so the illegal
 *       "disabled but still collecting" state cannot exist.</li>
 *   <li>PMC-05: the current collection merchant cannot be deleted.</li>
 *   <li>PMC-06/07: secret credential columns are echoed masked (•••• + last 4);
 *       blank or the masked echo on save keeps the stored value.</li>
 *   <li>PMC-14: uk_wechat_pay_config_mch_id is a bare single-column unique key, so a soft-deleted row
 *       keeps holding its mch_id and recreating that number revives the row. Revive is a create, not
 *       an edit: the row is rewritten in full so no credential, connectivity state or creation
 *       provenance of the previous merchant survives.</li>
 * </ul>
 *
 * <p>Credential values are never logged: logs carry mchId / appId, booleans and the names of the
 * credential fields that were rewritten — never their values.</p>
 */
@Service
public class WechatPayConfigService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayConfigService.class);

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String AUDIT_TARGET_TYPE = "WECHAT_PAY_CONFIG";

    @Autowired
    private WechatPayConfigMapper wechatPayConfigMapper;

    public ApiResponse list() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can list wechat pay configs");
        }
        log.info("admin list wechat pay configs");

        WechatPayConfigExample example = new WechatPayConfigExample();
        example.createCriteria().andDelFlagEqualTo(0);
        example.setOrderByClause("is_collection_active desc, is_enabled desc, id asc");
        List<WechatPayConfig> rows = wechatPayConfigMapper.selectByExample(example);

        SimpleDateFormat fmt = new SimpleDateFormat(TIME_PATTERN);
        List<WechatPayConfigItem> items = new ArrayList<>(rows.size());
        String activeMchId = null;
        for (WechatPayConfig row : rows) {
            if (Integer.valueOf(1).equals(row.getCollectionActive())) {
                activeMchId = row.getMchId();
            }
            WechatPayConfigItem item = new WechatPayConfigItem();
            item.setId(row.getId());
            item.setMerchantName(row.getMerchantName());
            item.setMchId(row.getMchId());
            item.setAppId(row.getAppId());
            item.setEnabled(Integer.valueOf(1).equals(row.getEnabled()));
            item.setCollectionActive(Integer.valueOf(1).equals(row.getCollectionActive()));
            item.setKeyMode(row.getKeyMode());
            item.setMerchantSerialNo(row.getMerchantSerialNo());
            item.setWechatPublicKeyId(row.getWechatPublicKeyId());
            item.setPlatformCertSerialNo(row.getPlatformCertSerialNo());
            item.setJsapiAuthDir(row.getJsapiAuthDir());
            item.setNotifyUrl(row.getNotifyUrl());
            item.setEnvMode(row.getEnvMode());
            // Secret credentials are echoed masked; blank / masked echo on save keeps the stored value.
            item.setMchPrivateKey(maskSecret(row.getMchPrivateKey()));
            item.setApiclientCert(maskSecret(row.getApiclientCert()));
            item.setMchCertP12(maskSecret(row.getMchCertP12()));
            item.setWechatPublicKey(row.getWechatPublicKey());
            item.setPlatformCert(row.getPlatformCert());
            item.setApiV3Key(maskSecret(row.getApiV3Key()));
            item.setApiV2Key(maskSecret(row.getApiV2Key()));
            item.setLastTestStatus(row.getLastTestStatus());
            item.setLastTestAt(row.getLastTestAt() == null ? null : fmt.format(row.getLastTestAt()));
            item.setLastTestError(row.getLastTestError());
            item.setGmtCreate(row.getGmtCreate() == null ? null : fmt.format(row.getGmtCreate()));
            item.setGmtModified(row.getGmtModified() == null ? null : fmt.format(row.getGmtModified()));
            item.setModifier(row.getModifier());
            items.add(item);
        }
        log.info("admin list wechat pay configs done count={} activeMchId={}", items.size(), activeMchId);
        return ok(items);
    }

    @Transactional
    public ApiResponse save(ReqSaveWechatPayConfig req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can save wechat pay configs");
        }
        log.info("admin save wechat pay config id={} mchId={} enabled={}",
                req.getId(), req.getMchId(), req.getEnabled());

        String merchantName = requireText(req.getMerchantName(), "merchant_name", 128);
        String mchId = requireText(req.getMchId(), "mch_id", 64);
        String appId = requireText(req.getAppId(), "app_id", 64);
        String merchantSerialNo = optionalText(req.getMerchantSerialNo(), "merchant_serial_no", 128);
        String wechatPublicKeyId = optionalText(req.getWechatPublicKeyId(), "wechat_public_key_id", 128);
        String platformCertSerialNo = optionalText(req.getPlatformCertSerialNo(), "platform_cert_serial_no", 128);
        String jsapiAuthDir = optionalText(req.getJsapiAuthDir(), "jsapi_auth_dir", 255);
        String notifyUrl = optionalText(req.getNotifyUrl(), "notify_url", 512);
        String keyMode = trimOrNull(req.getKeyMode()) == null ? "PUBLIC_KEY" : req.getKeyMode().trim();
        if (!"PUBLIC_KEY".equals(keyMode) && !"PLATFORM_CERT".equals(keyMode)) {
            throw new BizException("60009", "key_mode must be PUBLIC_KEY or PLATFORM_CERT");
        }
        String envMode = trimOrNull(req.getEnvMode()) == null ? "PRODUCTION" : req.getEnvMode().trim();
        if (!"SANDBOX".equals(envMode) && !"PRODUCTION".equals(envMode)) {
            throw new BizException("60009", "env_mode must be SANDBOX or PRODUCTION");
        }
        String apiV3Key = trimOrNull(req.getApiV3Key());
        String apiV2Key = trimOrNull(req.getApiV2Key());
        if (apiV3Key != null && apiV3Key.length() > 128) {
            throw new BizException("60009", "api_v3_key must not exceed 128 characters");
        }
        if (apiV2Key != null && apiV2Key.length() > 128) {
            throw new BizException("60009", "api_v2_key must not exceed 128 characters");
        }

        // uk_wechat_pay_config_mch_id covers mch_id alone, so a soft-deleted row still occupies the
        // number: the lookup must span del_flag=0 and del_flag=1 or the insert below would hit a
        // duplicate-key error the admin cannot resolve from the console.
        WechatPayConfigExample dupExample = new WechatPayConfigExample();
        dupExample.createCriteria().andMchIdEqualTo(mchId);
        WechatPayConfig sameNumber = wechatPayConfigMapper.selectOneByExample(dupExample);
        WechatPayConfig revivable = null;
        if (sameNumber != null && !sameNumber.getId().equals(req.getId())) {
            if (!Integer.valueOf(1).equals(sameNumber.getDelFlag())) {
                log.warn("admin save wechat pay config rejected-duplicate mchId={} existingId={}",
                        mchId, sameNumber.getId());
                throw new BizException("60014", "mch_id already exists: " + mchId);
            }
            if (req.getId() != null) {
                log.warn("admin save wechat pay config rejected-deleted-duplicate id={} mchId={} deletedId={}",
                        req.getId(), mchId, sameNumber.getId());
                throw new BizException("60014",
                        "mch_id belongs to a deleted merchant, recreate it instead of renaming: " + mchId);
            }
            revivable = sameNumber;
        }

        String operator = resolveOperator();
        boolean enabled = !Boolean.FALSE.equals(req.getEnabled());
        Date now = new Date();

        WechatPayConfig row = new WechatPayConfig();
        row.setMerchantName(merchantName);
        row.setMchId(mchId);
        row.setAppId(appId);
        row.setEnabled(enabled ? 1 : 0);
        row.setKeyMode(keyMode);
        row.setMerchantSerialNo(merchantSerialNo);
        row.setWechatPublicKeyId(wechatPublicKeyId);
        row.setPlatformCertSerialNo(platformCertSerialNo);
        row.setJsapiAuthDir(jsapiAuthDir);
        row.setNotifyUrl(notifyUrl);
        row.setEnvMode(envMode);
        row.setModifier(operator);
        row.setGmtModified(now);
        if (!enabled) {
            // PMC-04: a disabled merchant must never remain the collection merchant.
            row.setCollectionActive(0);
        }

        // PMC-07: credential fields stay null when the request leaves them blank. On the edit path
        // updateByPrimaryKeySelective skips null columns, so the stored value survives; on the revive
        // path below the full updateByPrimaryKey writes those nulls, because reviving is a create.
        List<String> updatedSecrets = new ArrayList<>(6);
        String mchPrivateKey = trimOrNull(req.getMchPrivateKey());
        if (mchPrivateKey != null) {
            row.setMchPrivateKey(mchPrivateKey);
            updatedSecrets.add("mch_private_key");
        }
        String mchCertP12 = trimOrNull(req.getMchCertP12());
        if (mchCertP12 != null) {
            row.setMchCertP12(mchCertP12);
            updatedSecrets.add("mch_cert_p12");
        }
        if (apiV3Key != null) {
            row.setApiV3Key(apiV3Key);
            updatedSecrets.add("api_v3_key");
        }
        if (apiV2Key != null) {
            row.setApiV2Key(apiV2Key);
            updatedSecrets.add("api_v2_key");
        }
        String wechatPublicKey = trimOrNull(req.getWechatPublicKey());
        if (wechatPublicKey != null) {
            row.setWechatPublicKey(wechatPublicKey);
            updatedSecrets.add("wechat_public_key");
        }
        String apiclientCert = trimOrNull(req.getApiclientCert());
        if (apiclientCert != null) {
            row.setApiclientCert(apiclientCert);
            updatedSecrets.add("apiclient_cert");
        }
        String platformCert = trimOrNull(req.getPlatformCert());
        if (platformCert != null) {
            row.setPlatformCert(platformCert);
            updatedSecrets.add("platform_cert");
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
            wechatPayConfigMapper.updateByPrimaryKey(row);
            targetId = revivable.getId();
            action = "CREATE_WECHAT_PAY_CONFIG";
            log.info("admin save wechat pay config revived soft-deleted row id={} mchId={}",
                    targetId, mchId);
        } else if (req.getId() == null) {
            row.setCollectionActive(0);
            row.setDelFlag(0);
            row.setCreator(operator);
            wechatPayConfigMapper.insertSelective(row);
            targetId = row.getId();
            action = "CREATE_WECHAT_PAY_CONFIG";
        } else {
            WechatPayConfig existing = requireRow(req.getId());
            row.setId(existing.getId());
            wechatPayConfigMapper.updateByPrimaryKeySelective(row);
            targetId = existing.getId();
            action = "UPDATE_WECHAT_PAY_CONFIG";
        }

        log.info("admin save wechat pay config done id={} mchId={} enabled={} secretsUpdated={}",
                targetId, mchId, enabled, updatedSecrets.size());
        /* no ops audit table in short */
        return ok(new PayConfigIdResult(targetId));
    }

    @Transactional
    public ApiResponse setEnabled(ReqSetPayConfigEnabled req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can change wechat pay config state");
        }
        log.info("admin set wechat pay config enabled id={} enabled={}", req.getId(), req.getEnabled());
        if (req.getEnabled() == null) {
            throw new BizException("60009", "is_enabled must not be null");
        }
        WechatPayConfig existing = requireRow(req.getId());
        boolean enabled = Boolean.TRUE.equals(req.getEnabled());
        boolean clearedCollection = !enabled && Integer.valueOf(1).equals(existing.getCollectionActive());

        WechatPayConfig update = new WechatPayConfig();
        update.setId(existing.getId());
        update.setEnabled(enabled ? 1 : 0);
        if (!enabled) {
            // PMC-04: disabling the collection merchant clears the flag in the same transaction.
            update.setCollectionActive(0);
        }
        update.setModifier(resolveOperator());
        update.setGmtModified(new Date());
        wechatPayConfigMapper.updateByPrimaryKeySelective(update);

        if (clearedCollection) {
            log.warn("admin disabled the active wechat collection merchant id={} mchId={};"
                    + " channel now has no collection merchant", existing.getId(), existing.getMchId());
        }
        log.info("admin set wechat pay config enabled done id={} mchId={} enabled={} clearedCollection={}",
                existing.getId(), existing.getMchId(), enabled, clearedCollection);
        /* no ops audit table in short */
        return ok(new PayConfigEnabledResult(existing.getId(), enabled,
                enabled ? Integer.valueOf(1).equals(existing.getCollectionActive()) : Boolean.FALSE));
    }

    @Transactional
    public ApiResponse setCollectionActive(ReqPayConfigId req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can designate the wechat collection merchant");
        }
        log.info("admin set wechat collection merchant id={}", req.getId());
        WechatPayConfig existing = requireRow(req.getId());
        if (!Integer.valueOf(1).equals(existing.getEnabled())) {
            log.warn("admin set wechat collection merchant rejected-disabled id={} mchId={}",
                    existing.getId(), existing.getMchId());
            throw new BizException("60015",
                    "a disabled merchant cannot be designated as the collection merchant");
        }

        String operator = resolveOperator();
        Date now = new Date();

        // PMC-02: clear the channel first, then set the target — both inside this transaction.
        WechatPayConfig clear = new WechatPayConfig();
        clear.setCollectionActive(0);
        clear.setModifier(operator);
        clear.setGmtModified(now);
        WechatPayConfigExample clearExample = new WechatPayConfigExample();
        clearExample.createCriteria()
                .andDelFlagEqualTo(0)
                .andCollectionActiveEqualTo(1);
        int cleared = wechatPayConfigMapper.updateByExampleSelective(clear, clearExample);

        WechatPayConfig update = new WechatPayConfig();
        update.setId(existing.getId());
        update.setCollectionActive(1);
        update.setModifier(operator);
        update.setGmtModified(now);
        wechatPayConfigMapper.updateByPrimaryKeySelective(update);

        log.info("admin set wechat collection merchant done id={} mchId={} previousCleared={}",
                existing.getId(), existing.getMchId(), cleared);
        /* no ops audit table in short */
        return ok(new PayConfigCollectionActiveResult(existing.getId(), Boolean.TRUE));
    }

    @Transactional
    public ApiResponse delete(ReqPayConfigId req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can delete wechat pay configs");
        }
        log.info("admin delete wechat pay config id={}", req.getId());
        WechatPayConfig existing = requireRow(req.getId());
        if (Integer.valueOf(1).equals(existing.getCollectionActive())) {
            log.warn("admin delete wechat pay config rejected-active id={} mchId={}",
                    existing.getId(), existing.getMchId());
            throw new BizException("60015",
                    "the current collection merchant cannot be deleted; switch or disable it first");
        }

        WechatPayConfig update = new WechatPayConfig();
        update.setId(existing.getId());
        update.setDelFlag(1);
        update.setModifier(resolveOperator());
        update.setGmtModified(new Date());
        wechatPayConfigMapper.updateByPrimaryKeySelective(update);

        log.info("admin delete wechat pay config done id={} mchId={}",
                existing.getId(), existing.getMchId());
        /* no ops audit table in short */
        return ok(new PayConfigIdResult(existing.getId()));
    }

    private WechatPayConfig requireRow(Long id) {
        if (id == null) {
            throw new BizException("60009", "id must not be null");
        }
        WechatPayConfig row = wechatPayConfigMapper.selectByPrimaryKey(id);
        if (row == null || Integer.valueOf(1).equals(row.getDelFlag())) {
            throw new BizException("60009", "wechat pay config not found: " + id);
        }
        return row;
    }

    private static String auditDetail(String merchantName, boolean enabled, List<String> updatedSecrets) {
        // Only field names of updated credentials are recorded — never their values (PMC-12).
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

    /** Required non-blank field: trimmed, length-checked, rejected with 60009 when absent. */
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

    /** Optional field: blank collapses to empty string so clearing it in the console persists. */
    private static String optionalText(String value, String field, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > maxLength) {
            throw new BizException("60009", field + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    /** Credential field: blank collapses to null, which the selective mapper skips (PMC-07). */
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
