package com.bgssai.shortai.centralledger.service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport;
import com.bgssai.shortai.model.WebsiteExternalSubscription;
import com.bgssai.shortai.model.WebsiteExternalSubscriptionExample;
import com.bgssai.shortai.model.WebsiteSubscriptionPlan;
import com.bgssai.shortai.model.WebsiteSubscriptionPlanExample;
import com.bgssai.shortai.model.WebsiteUsageIdempotency;
import com.bgssai.shortai.model.WebsiteUsageIdempotencyExample;
import com.bgssai.shortai.model.WebsiteUsageLedger;
import com.bgssai.shortai.model.WebsiteUser;
import com.bgssai.shortai.model.WebsiteUserExample;
import com.bgssai.shortai.model.WebsiteUserQuota;
import com.bgssai.shortai.model.WebsiteUserQuotaExample;
import com.bgssai.shortai.model.WebsiteUserSubscription;
import com.bgssai.shortai.model.WebsiteUserSubscriptionExample;
import com.bgssai.shortai.mapper.WebsiteExternalSubscriptionMapper;
import com.bgssai.shortai.mapper.WebsiteSubscriptionPlanMapper;
import com.bgssai.shortai.mapper.WebsiteUsageIdempotencyMapper;
import com.bgssai.shortai.mapper.WebsiteUsageLedgerMapper;
import com.bgssai.shortai.mapper.WebsiteUserMapper;
import com.bgssai.shortai.mapper.WebsiteUserQuotaMapper;
import com.bgssai.shortai.mapper.WebsiteUserSubscriptionMapper;

/**
 * 中央账本（官网 bgssai_website 库）直连。产品与官网同在一个 MySQL 实例，跨库读写，不经 HTTP。
 *
 * <p>三件事：
 * <ul>
 *   <li>isEntitled：读官网订阅，判定「官网付费用户在本产品视为已开通」；</li>
 *   <li>consume：本产品消耗的用量按官网 QuotaWalletService 的口径直接扣官网用量钱包并落用量流水；</li>
 *   <li>reportOwnSubscription / linkWebsiteUser：本产品自己卖出的订阅写进 external_subscription，
 *       让中央账本看得见；用户日后绑定 BGSSAI 账号时回填归属。</li>
 * </ul>
 * 表契约见官网仓 docs/api/BILLING.md 与 docs/api/EXTERNAL-SUBSCRIPTION.md。
 * 中央账本的任何失败只记日志，由调用方兜底，不得影响本产品自己的计费与业务。
 */
@Service
public class CentralLedgerService {

    private static final Logger log = LoggerFactory.getLogger(CentralLedgerService.class);

    /** 本产品在中央账本里的编码，同时也是 oauth_client 里登记的 client_id。 */
    public static final String PRODUCT_CODE = "bgssai-short";

    public static final String SUB_ACTIVE = "ACTIVE";
    public static final String SUB_EXPIRED = "EXPIRED";
    public static final String SUB_CANCELED = "CANCELED";
    public static final String SUB_REFUNDED = "REFUNDED";

    private static final String PLAN_FREE = "FREE";
    private static final String WEBSITE_SUB_ACTIVE = "active";
    private static final String KIND_PRODUCT_CREDIT = "product_tokens";
    private static final String CURRENCY_CNY = "CNY";
    private static final Integer USER_STATUS_NORMAL = 1;
    private static final long FIVE_HOURS_MS = TimeUnit.HOURS.toMillis(5);
    private static final long WEEK_MS = TimeUnit.DAYS.toMillis(7);
    private static final int MAX_CAS_RETRY = 5;
    private static final int REMARK_MAX = 256;

    private final WebsiteUserMapper websiteUserMapper;
    private final WebsiteUserSubscriptionMapper websiteUserSubscriptionMapper;
    private final WebsiteUserQuotaMapper websiteUserQuotaMapper;
    private final WebsiteSubscriptionPlanMapper websiteSubscriptionPlanMapper;
    private final WebsiteUsageLedgerMapper websiteUsageLedgerMapper;
    private final WebsiteUsageIdempotencyMapper websiteUsageIdempotencyMapper;
    private final WebsiteExternalSubscriptionMapper websiteExternalSubscriptionMapper;

    public CentralLedgerService(WebsiteUserMapper websiteUserMapper,
                                WebsiteUserSubscriptionMapper websiteUserSubscriptionMapper,
                                WebsiteUserQuotaMapper websiteUserQuotaMapper,
                                WebsiteSubscriptionPlanMapper websiteSubscriptionPlanMapper,
                                WebsiteUsageLedgerMapper websiteUsageLedgerMapper,
                                WebsiteUsageIdempotencyMapper websiteUsageIdempotencyMapper,
                                WebsiteExternalSubscriptionMapper websiteExternalSubscriptionMapper) {
        this.websiteUserMapper = websiteUserMapper;
        this.websiteUserSubscriptionMapper = websiteUserSubscriptionMapper;
        this.websiteUserQuotaMapper = websiteUserQuotaMapper;
        this.websiteSubscriptionPlanMapper = websiteSubscriptionPlanMapper;
        this.websiteUsageLedgerMapper = websiteUsageLedgerMapper;
        this.websiteUsageIdempotencyMapper = websiteUsageIdempotencyMapper;
        this.websiteExternalSubscriptionMapper = websiteExternalSubscriptionMapper;
    }

    /**
     * 官网付费套餐（PRO 及以上）且未过期即视为已开通。账号冻结、FREE、过期一律 false。
     * 只读，不代官网做过期回落；官网自己下次读钱包时会处理。
     */
    public boolean isEntitled(String websiteUserId) {
        log.info("中央账本权益查询 product={} websiteUserId={}", PRODUCT_CODE, websiteUserId);
        if (isBlank(websiteUserId)) {
            return false;
        }
        WebsiteUser user = findUser(websiteUserId);
        if (user == null || !USER_STATUS_NORMAL.equals(user.getStatus())) {
            return false;
        }
        WebsiteUserSubscription sub = findSubscription(websiteUserId);
        if (sub == null || sub.getPlanCode() == null || PLAN_FREE.equals(sub.getPlanCode())) {
            return false;
        }
        if (!WEBSITE_SUB_ACTIVE.equals(sub.getStatus())) {
            return false;
        }
        Date end = sub.getPeriodEnd();
        return end == null || end.after(new Date());
    }

    /**
     * 把本产品消耗的用量扣到官网用量钱包并落 usage_ledger，口径与官网 QuotaWalletService.consume 一致：
     * 先扣套餐双窗口（5 小时 / 7 天，到点归零），窗口不够再扣加量包；乐观锁 quota_version。
     * 带 idempotencyKey 时同一键只入账一次。
     *
     * @return true 已入账或命中幂等重放；false 未入账（用量不足、钱包缺失、并发重试耗尽）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean consume(String websiteUserId, int tokens, String usageKind, String idempotencyKey, String remark) {
        log.info("中央账本用量入账 product={} websiteUserId={} tokens={} kind={}",
                PRODUCT_CODE, websiteUserId, tokens, usageKind);
        if (isBlank(websiteUserId) || tokens <= 0) {
            return false;
        }
        String key = blankToNull(idempotencyKey);
        String ledgerId = newId();
        if (key != null) {
            WebsiteUsageIdempotency idem = new WebsiteUsageIdempotency();
            idem.setClientId(PRODUCT_CODE);
            idem.setIdempotencyKey(key);
            idem.setLedgerId(ledgerId);
            idem.setUserId(websiteUserId);
            idem.setDelFlag(Boolean.FALSE);
            idem.setCreator(PRODUCT_CODE);
            idem.setModifier(PRODUCT_CODE);
            try {
                websiteUsageIdempotencyMapper.insertSelective(idem);
            } catch (DuplicateKeyException ex) {
                log.info("中央账本用量已入账过，跳过 product={} websiteUserId={} key={}", PRODUCT_CODE, websiteUserId, key);
                return true;
            }
        }
        boolean applied = false;
        try {
            applied = deductAndRecord(websiteUserId, tokens, blankToNull(usageKind), remark, ledgerId);
        } finally {
            if (!applied && key != null) {
                WebsiteUsageIdempotencyExample undo = new WebsiteUsageIdempotencyExample();
                undo.createCriteria()
                        .andClientIdEqualTo(PRODUCT_CODE)
                        .andIdempotencyKeyEqualTo(key)
                        .andLedgerIdEqualTo(ledgerId);
                websiteUsageIdempotencyMapper.deleteByExample(undo);
            }
        }
        return applied;
    }

    /**
     * 本产品卖出 / 续费 / 到期 / 退款一笔订阅，写进中央账本。同一 externalOrderNo 只落一行，
     * 重复上报按主键更新；websiteUserId 为空时不覆盖已回填的归属。
     *
     * @return true 已写入或已更新；false 参数不完整或状态非法
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reportOwnSubscription(OwnSubscriptionReport report) {
        if (report == null) {
            return false;
        }
        log.info("自有订阅上报中央账本 product={} externalUserId={} orderNo={} plan={} status={} amountCent={}",
                PRODUCT_CODE, report.getExternalUserId(), report.getExternalOrderNo(),
                report.getPlanCode(), report.getStatus(), report.getAmountCent());
        if (isBlank(report.getExternalUserId()) || isBlank(report.getExternalOrderNo()) || isBlank(report.getPlanCode())) {
            log.warn("自有订阅上报参数不完整，跳过 product={} orderNo={}", PRODUCT_CODE, report.getExternalOrderNo());
            return false;
        }
        String status = normalizeStatus(report.getStatus());
        if (status == null) {
            log.warn("自有订阅状态非法，跳过 product={} orderNo={} status={}",
                    PRODUCT_CODE, report.getExternalOrderNo(), report.getStatus());
            return false;
        }
        int amount = report.getAmountCent() == null ? 0 : Math.max(0, report.getAmountCent());
        Date now = new Date();
        WebsiteExternalSubscription existing = findByOrderNo(report.getExternalOrderNo());
        if (existing == null) {
            WebsiteExternalSubscription row = new WebsiteExternalSubscription();
            row.setSubscriptionId(newId());
            row.setClientId(PRODUCT_CODE);
            row.setProductCode(PRODUCT_CODE);
            row.setExternalUserId(report.getExternalUserId());
            row.setExternalOrderNo(report.getExternalOrderNo());
            row.setUserId(blankToNull(report.getWebsiteUserId()));
            row.setPlanCode(report.getPlanCode());
            row.setPlanName(report.getPlanName());
            row.setAmountCent(amount);
            row.setCurrency(CURRENCY_CNY);
            row.setStatus(status);
            row.setPeriodStart(report.getPeriodStart());
            row.setPeriodEnd(report.getPeriodEnd());
            row.setReportedAt(now);
            row.setRemark(truncate(report.getRemark()));
            row.setDelFlag(Boolean.FALSE);
            row.setCreator(PRODUCT_CODE);
            row.setModifier(PRODUCT_CODE);
            try {
                websiteExternalSubscriptionMapper.insertSelective(row);
                log.info("自有订阅已写入中央账本 product={} orderNo={} subscriptionId={} status={}",
                        PRODUCT_CODE, report.getExternalOrderNo(), row.getSubscriptionId(), status);
                return true;
            } catch (DuplicateKeyException ex) {
                existing = findByOrderNo(report.getExternalOrderNo());
                if (existing == null) {
                    log.warn("自有订阅并发写入后仍未查到，放弃 product={} orderNo={}", PRODUCT_CODE, report.getExternalOrderNo());
                    return false;
                }
            }
        }
        WebsiteExternalSubscription patch = new WebsiteExternalSubscription();
        patch.setId(existing.getId());
        patch.setUserId(blankToNull(report.getWebsiteUserId()));
        patch.setPlanCode(report.getPlanCode());
        patch.setPlanName(report.getPlanName());
        patch.setAmountCent(amount);
        patch.setStatus(status);
        patch.setPeriodStart(report.getPeriodStart());
        patch.setPeriodEnd(report.getPeriodEnd());
        patch.setReportedAt(now);
        patch.setRemark(truncate(report.getRemark()));
        patch.setModifier(PRODUCT_CODE);
        websiteExternalSubscriptionMapper.updateByPrimaryKeySelective(patch);
        log.info("自有订阅已更新中央账本 product={} orderNo={} subscriptionId={} status={}",
                PRODUCT_CODE, report.getExternalOrderNo(), existing.getSubscriptionId(), status);
        return true;
    }

    /**
     * 用户完成 BGSSAI 账号绑定后，把本产品此前上报、尚未关联官网账号的订阅一次回填归属。
     *
     * @return 回填的行数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int linkWebsiteUser(String externalUserId, String websiteUserId) {
        log.info("回填中央账本订阅归属 product={} externalUserId={} websiteUserId={}",
                PRODUCT_CODE, externalUserId, websiteUserId);
        if (isBlank(externalUserId) || isBlank(websiteUserId)) {
            return 0;
        }
        WebsiteExternalSubscription patch = new WebsiteExternalSubscription();
        patch.setUserId(websiteUserId);
        patch.setReportedAt(new Date());
        patch.setModifier(PRODUCT_CODE);
        WebsiteExternalSubscriptionExample example = new WebsiteExternalSubscriptionExample();
        example.createCriteria()
                .andClientIdEqualTo(PRODUCT_CODE)
                .andExternalUserIdEqualTo(externalUserId)
                .andUserIdIsNull()
                .andDelFlagEqualTo(Boolean.FALSE);
        int linked = websiteExternalSubscriptionMapper.updateByExampleSelective(patch, example);
        log.info("中央账本订阅归属已回填 product={} externalUserId={} linked={}", PRODUCT_CODE, externalUserId, linked);
        return linked;
    }

    private boolean deductAndRecord(String websiteUserId, int tokens, String usageKind, String remark, String ledgerId) {
        for (int attempt = 0; attempt < MAX_CAS_RETRY; attempt++) {
            WebsiteUserQuota quota = findQuota(websiteUserId);
            if (quota == null) {
                log.warn("中央用量钱包缺失，未入账 product={} websiteUserId={}", PRODUCT_CODE, websiteUserId);
                return false;
            }
            Date now = new Date();
            String planCode = quota.getPlanCode();
            int fiveLimit = nvl(quota.getFiveHourLimitTokens());
            int weekLimit = nvl(quota.getWeeklyLimitTokens());
            if (paidPlanExpired(findSubscription(websiteUserId), now)) {
                WebsiteSubscriptionPlan free = findPlan(PLAN_FREE);
                if (free != null) {
                    planCode = PLAN_FREE;
                    fiveLimit = nvl(free.getFiveHourLimitTokens());
                    weekLimit = nvl(free.getWeeklyLimitTokens());
                }
            }
            int fiveUsed = nvl(quota.getFiveHourUsedTokens());
            Date fiveStart = quota.getFiveHourWindowStart();
            if (fiveStart == null || now.getTime() - fiveStart.getTime() >= FIVE_HOURS_MS) {
                fiveUsed = 0;
                fiveStart = now;
            }
            int weekUsed = nvl(quota.getWeeklyUsedTokens());
            Date weekAnchor = quota.getWeeklyAnchorAt();
            if (weekAnchor == null || now.getTime() - weekAnchor.getTime() >= WEEK_MS) {
                weekUsed = 0;
                weekAnchor = now;
            }
            int windowRemain = Math.min(Math.max(0, fiveLimit - fiveUsed), Math.max(0, weekLimit - weekUsed));
            int extra = nvl(quota.getExtraTokens());
            if (windowRemain + extra < tokens) {
                log.warn("中央用量不足，未入账 product={} websiteUserId={} tokens={} remaining={}",
                        PRODUCT_CODE, websiteUserId, tokens, windowRemain + extra);
                return false;
            }
            int fromWindow = Math.min(tokens, windowRemain);
            int fromExtra = tokens - fromWindow;

            WebsiteUserQuota patch = new WebsiteUserQuota();
            patch.setPlanCode(planCode);
            patch.setFiveHourLimitTokens(fiveLimit);
            patch.setFiveHourUsedTokens(fiveUsed + fromWindow);
            patch.setFiveHourWindowStart(fiveStart);
            patch.setWeeklyLimitTokens(weekLimit);
            patch.setWeeklyUsedTokens(weekUsed + fromWindow);
            patch.setWeeklyAnchorAt(weekAnchor);
            patch.setExtraTokens(extra - fromExtra);
            patch.setQuotaVersion(nvl(quota.getQuotaVersion()) + 1);
            patch.setModifier(PRODUCT_CODE);
            WebsiteUserQuotaExample cas = new WebsiteUserQuotaExample();
            cas.createCriteria()
                    .andIdEqualTo(quota.getId())
                    .andQuotaVersionEqualTo(nvl(quota.getQuotaVersion()))
                    .andDelFlagEqualTo(Boolean.FALSE);
            if (websiteUserQuotaMapper.updateByExampleSelective(patch, cas) != 1) {
                continue;
            }

            WebsiteUsageLedger ledger = new WebsiteUsageLedger();
            ledger.setLedgerId(ledgerId);
            ledger.setUserId(websiteUserId);
            ledger.setProductCode(PRODUCT_CODE);
            ledger.setClientId(PRODUCT_CODE);
            ledger.setUsageKind(usageKind == null ? KIND_PRODUCT_CREDIT : usageKind);
            ledger.setUsageTokens(tokens);
            ledger.setPromptTokens(0);
            ledger.setCompletionTokens(0);
            ledger.setRemark(truncate(remark));
            ledger.setDelFlag(Boolean.FALSE);
            ledger.setCreator(PRODUCT_CODE);
            ledger.setModifier(PRODUCT_CODE);
            websiteUsageLedgerMapper.insertSelective(ledger);
            log.info("中央账本用量已入账 product={} websiteUserId={} tokens={} ledgerId={}",
                    PRODUCT_CODE, websiteUserId, tokens, ledgerId);
            return true;
        }
        log.warn("中央用量入账并发重试耗尽，未入账 product={} websiteUserId={} tokens={}", PRODUCT_CODE, websiteUserId, tokens);
        return false;
    }

    private static boolean paidPlanExpired(WebsiteUserSubscription sub, Date now) {
        return sub != null
                && sub.getPlanCode() != null
                && !PLAN_FREE.equals(sub.getPlanCode())
                && WEBSITE_SUB_ACTIVE.equals(sub.getStatus())
                && sub.getPeriodEnd() != null
                && sub.getPeriodEnd().before(now);
    }

    private WebsiteUser findUser(String websiteUserId) {
        WebsiteUserExample example = new WebsiteUserExample();
        example.selectColumns("id", "user_id", "status");
        example.createCriteria().andUserIdEqualTo(websiteUserId).andDelFlagEqualTo(Boolean.FALSE);
        return websiteUserMapper.selectOneByExample(example);
    }

    private WebsiteUserSubscription findSubscription(String websiteUserId) {
        WebsiteUserSubscriptionExample example = new WebsiteUserSubscriptionExample();
        example.createCriteria().andUserIdEqualTo(websiteUserId).andDelFlagEqualTo(Boolean.FALSE);
        return websiteUserSubscriptionMapper.selectOneByExample(example);
    }

    private WebsiteUserQuota findQuota(String websiteUserId) {
        WebsiteUserQuotaExample example = new WebsiteUserQuotaExample();
        example.createCriteria().andUserIdEqualTo(websiteUserId).andDelFlagEqualTo(Boolean.FALSE);
        return websiteUserQuotaMapper.selectOneByExample(example);
    }

    private WebsiteSubscriptionPlan findPlan(String planCode) {
        WebsiteSubscriptionPlanExample example = new WebsiteSubscriptionPlanExample();
        example.createCriteria().andPlanCodeEqualTo(planCode).andDelFlagEqualTo(Boolean.FALSE);
        return websiteSubscriptionPlanMapper.selectOneByExample(example);
    }

    private WebsiteExternalSubscription findByOrderNo(String externalOrderNo) {
        WebsiteExternalSubscriptionExample example = new WebsiteExternalSubscriptionExample();
        example.createCriteria()
                .andClientIdEqualTo(PRODUCT_CODE)
                .andExternalOrderNoEqualTo(externalOrderNo)
                .andDelFlagEqualTo(Boolean.FALSE);
        return websiteExternalSubscriptionMapper.selectOneByExample(example);
    }

    private static String normalizeStatus(String status) {
        String value = blankToNull(status);
        if (value == null) {
            return SUB_ACTIVE;
        }
        String upper = value.toUpperCase();
        if (SUB_ACTIVE.equals(upper) || SUB_EXPIRED.equals(upper)
                || SUB_CANCELED.equals(upper) || SUB_REFUNDED.equals(upper)) {
            return upper;
        }
        return null;
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= REMARK_MAX ? value : value.substring(0, REMARK_MAX);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
