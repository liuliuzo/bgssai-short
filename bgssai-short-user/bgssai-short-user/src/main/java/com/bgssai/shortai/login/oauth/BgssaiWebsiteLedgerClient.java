package com.bgssai.shortai.login.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport;
import com.bgssai.shortai.centralledger.service.CentralLedgerService;
import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.model.OauthBinding;
import com.bgssai.shortai.model.OauthBindingExample;

/**
 * 本产品账号与官网中央账本之间的桥：把本地 userId 解析成已绑定的官网 user_id（oauth_binding.open_id），
 * 再委托 CentralLedgerService 直连官网库读写，不再经 HTTP 调官网接口。
 * 未绑定则跳过；中央账本任何失败只打日志，不回滚本地扣减。
 */
@Component
public class BgssaiWebsiteLedgerClient {

    private static final Logger log = LoggerFactory.getLogger(BgssaiWebsiteLedgerClient.class);
    private static final String PLATFORM_BGSSAI = "bgssai";

    private final OauthBindingMapper oauthBindingMapper;
    private final CentralLedgerService centralLedger;

    public BgssaiWebsiteLedgerClient(OauthBindingMapper oauthBindingMapper,
                                     CentralLedgerService centralLedger) {
        this.oauthBindingMapper = oauthBindingMapper;
        this.centralLedger = centralLedger;
    }

    public boolean isEntitled(String userId) {
        String websiteUserId = websiteUserIdOf(userId);
        if (websiteUserId == null) {
            return false;
        }
        try {
            return centralLedger.isEntitled(websiteUserId);
        } catch (Exception e) {
            log.warn("BGSSAI 权益查询失败 userId={} error={}", userId, e.getMessage());
            return false;
        }
    }

    public void reportDebit(String userId, long amount, String usageKind) {
        if (amount < 1 || amount > Integer.MAX_VALUE) {
            return;
        }
        String websiteUserId = websiteUserIdOf(userId);
        if (websiteUserId == null) {
            return;
        }
        try {
            centralLedger.consume(websiteUserId, (int) amount, usageKind, null,
                    "short debit " + (usageKind == null ? "" : usageKind));
        } catch (Exception e) {
            log.warn("BGSSAI 中央账本上报异常 userId={} error={}", userId, e.getMessage());
        }
    }

    /** 本产品卖出 / 续费一笔订阅后，让中央账本看得见。 */
    public void reportOwnSubscription(String userId, OwnSubscriptionReport report) {
        if (OAuthIds.blank(userId) || report == null) {
            return;
        }
        report.setExternalUserId(userId);
        report.setWebsiteUserId(websiteUserIdOf(userId));
        try {
            centralLedger.reportOwnSubscription(report);
        } catch (Exception e) {
            log.warn("BGSSAI 自有订阅上报异常 userId={} orderNo={} error={}",
                    userId, report.getExternalOrderNo(), e.getMessage());
        }
    }

    /** 用户刚绑定官网账号：回填此前上报、尚未关联官网账号的订阅归属。 */
    public void linkWebsiteUser(String userId, String websiteUserId) {
        if (OAuthIds.blank(userId) || OAuthIds.blank(websiteUserId)) {
            return;
        }
        try {
            centralLedger.linkWebsiteUser(userId, websiteUserId);
        } catch (Exception e) {
            log.warn("BGSSAI 订阅归属回填异常 userId={} error={}", userId, e.getMessage());
        }
    }

    private String websiteUserIdOf(String userId) {
        if (OAuthIds.blank(userId)) {
            return null;
        }
        OauthBindingExample ex = new OauthBindingExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andPlatformEqualTo(PLATFORM_BGSSAI)
                .andDelFlagEqualTo(Boolean.FALSE);
        OauthBinding binding = oauthBindingMapper.selectOneByExample(ex);
        if (binding == null || OAuthIds.blank(binding.getOpenId())) {
            return null;
        }
        return binding.getOpenId();
    }
}
