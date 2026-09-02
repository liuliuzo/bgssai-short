package com.bgssai.shortai.centralledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport;
import com.bgssai.shortai.model.WebsiteExternalSubscription;
import com.bgssai.shortai.model.WebsiteExternalSubscriptionExample;
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

class CentralLedgerServiceTest {

    private static final String WEBSITE_USER = "wu-0001";

    private WebsiteUserMapper userMapper;
    private WebsiteUserSubscriptionMapper subscriptionMapper;
    private WebsiteUserQuotaMapper quotaMapper;
    private WebsiteSubscriptionPlanMapper planMapper;
    private WebsiteUsageLedgerMapper ledgerMapper;
    private WebsiteUsageIdempotencyMapper idempotencyMapper;
    private WebsiteExternalSubscriptionMapper externalMapper;
    private CentralLedgerService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(WebsiteUserMapper.class);
        subscriptionMapper = mock(WebsiteUserSubscriptionMapper.class);
        quotaMapper = mock(WebsiteUserQuotaMapper.class);
        planMapper = mock(WebsiteSubscriptionPlanMapper.class);
        ledgerMapper = mock(WebsiteUsageLedgerMapper.class);
        idempotencyMapper = mock(WebsiteUsageIdempotencyMapper.class);
        externalMapper = mock(WebsiteExternalSubscriptionMapper.class);
        service = new CentralLedgerService(userMapper, subscriptionMapper, quotaMapper, planMapper,
                ledgerMapper, idempotencyMapper, externalMapper);
    }

    @Test
    void writesUseIndependentTransactions() throws Exception {
        assertRequiresNew("consume", String.class, int.class, String.class, String.class, String.class);
        assertRequiresNew("reportOwnSubscription", OwnSubscriptionReport.class);
        assertRequiresNew("linkWebsiteUser", String.class, String.class);
    }

    @Test
    void entitledWhenPaidPlanActiveAndNotExpired() {
        stubUser(1);
        stubSubscription("PRO", "active", new Date(System.currentTimeMillis() + 86_400_000L));
        assertTrue(service.isEntitled(WEBSITE_USER));
    }

    @Test
    void notEntitledForFreeExpiredOrFrozen() {
        stubUser(1);
        stubSubscription("FREE", "active", null);
        assertFalse(service.isEntitled(WEBSITE_USER));

        stubSubscription("PRO", "active", new Date(System.currentTimeMillis() - 1000L));
        assertFalse(service.isEntitled(WEBSITE_USER));

        stubSubscription("PRO", "active", null);
        stubUser(2);
        assertFalse(service.isEntitled(WEBSITE_USER));
        assertFalse(service.isEntitled(" "));
    }

    @Test
    void consumeTakesWindowFirstThenExtraAndWritesLedger() {
        WebsiteUserQuota quota = quota(10, 8, 100, 50, 7, 3);
        when(quotaMapper.selectOneByExample(any(WebsiteUserQuotaExample.class))).thenReturn(quota);
        when(quotaMapper.updateByExampleSelective(any(WebsiteUserQuota.class), any(WebsiteUserQuotaExample.class))).thenReturn(1);
        stubSubscription("PRO", "active", new Date(System.currentTimeMillis() + 86_400_000L));

        assertTrue(service.consume(WEBSITE_USER, 5, "quota_cent", null, "test"));

        ArgumentCaptor<WebsiteUserQuota> patch = ArgumentCaptor.forClass(WebsiteUserQuota.class);
        verify(quotaMapper).updateByExampleSelective(patch.capture(), any(WebsiteUserQuotaExample.class));
        assertEquals(10, patch.getValue().getFiveHourUsedPoints());
        assertEquals(52, patch.getValue().getWeeklyUsedPoints());
        assertEquals(4, patch.getValue().getExtraPoints());
        assertEquals(4, patch.getValue().getQuotaVersion());

        ArgumentCaptor<WebsiteUsageLedger> ledger = ArgumentCaptor.forClass(WebsiteUsageLedger.class);
        verify(ledgerMapper).insertSelective(ledger.capture());
        assertEquals(5, ledger.getValue().getUsagePoints());
        assertEquals(CentralLedgerService.PRODUCT_CODE, ledger.getValue().getProductCode());
        verify(idempotencyMapper, never()).insertSelective(any(WebsiteUsageIdempotency.class));
    }

    @Test
    void consumeResetsExpiredWindowsBeforeCounting() {
        Date old = new Date(System.currentTimeMillis() - 6L * 3_600_000L);
        WebsiteUserQuota quota = quota(10, 10, 100, 100, 0, 1);
        quota.setFiveHourWindowStart(old);
        quota.setWeeklyAnchorAt(new Date(System.currentTimeMillis() - 8L * 86_400_000L));
        when(quotaMapper.selectOneByExample(any(WebsiteUserQuotaExample.class))).thenReturn(quota);
        when(quotaMapper.updateByExampleSelective(any(WebsiteUserQuota.class), any(WebsiteUserQuotaExample.class))).thenReturn(1);

        assertTrue(service.consume(WEBSITE_USER, 3, null, null, null));

        ArgumentCaptor<WebsiteUserQuota> patch = ArgumentCaptor.forClass(WebsiteUserQuota.class);
        verify(quotaMapper).updateByExampleSelective(patch.capture(), any(WebsiteUserQuotaExample.class));
        assertEquals(3, patch.getValue().getFiveHourUsedPoints());
        assertEquals(3, patch.getValue().getWeeklyUsedPoints());
        assertEquals(0, patch.getValue().getExtraPoints());
    }

    @Test
    void consumeRefusesWhenInsufficientAndReleasesIdempotencyKey() {
        WebsiteUserQuota quota = quota(10, 10, 100, 100, 1, 1);
        when(quotaMapper.selectOneByExample(any(WebsiteUserQuotaExample.class))).thenReturn(quota);

        assertFalse(service.consume(WEBSITE_USER, 5, null, "k-1", null));

        verify(idempotencyMapper).insertSelective(any(WebsiteUsageIdempotency.class));
        verify(idempotencyMapper).deleteByExample(any(WebsiteUsageIdempotencyExample.class));
        verify(ledgerMapper, never()).insertSelective(any(WebsiteUsageLedger.class));
    }

    @Test
    void consumeSkipsDuplicateIdempotencyKey() {
        when(idempotencyMapper.insertSelective(any(WebsiteUsageIdempotency.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        assertTrue(service.consume(WEBSITE_USER, 5, null, "k-1", null));

        verify(quotaMapper, never()).selectOneByExample(any(WebsiteUserQuotaExample.class));
    }

    @Test
    void reportOwnSubscriptionInsertsThenUpdatesSameOrder() {
        when(externalMapper.selectOneByExample(any(WebsiteExternalSubscriptionExample.class))).thenReturn(null);
        OwnSubscriptionReport report = report("o-1", "PRO", 9900, null);
        assertTrue(service.reportOwnSubscription(report));

        ArgumentCaptor<WebsiteExternalSubscription> row = ArgumentCaptor.forClass(WebsiteExternalSubscription.class);
        verify(externalMapper).insertSelective(row.capture());
        assertEquals(CentralLedgerService.PRODUCT_CODE, row.getValue().getClientId());
        assertEquals("ACTIVE", row.getValue().getStatus());
        assertEquals(9900, row.getValue().getAmountCent());

        WebsiteExternalSubscription existing = new WebsiteExternalSubscription();
        existing.setId(7L);
        existing.setSubscriptionId("sub-7");
        when(externalMapper.selectOneByExample(any(WebsiteExternalSubscriptionExample.class))).thenReturn(existing);
        assertTrue(service.reportOwnSubscription(report("o-1", "PRO", 9900, "refunded")));

        ArgumentCaptor<WebsiteExternalSubscription> patch = ArgumentCaptor.forClass(WebsiteExternalSubscription.class);
        verify(externalMapper).updateByPrimaryKeySelective(patch.capture());
        assertEquals(7L, patch.getValue().getId());
        assertEquals("REFUNDED", patch.getValue().getStatus());
    }

    @Test
    void reportOwnSubscriptionRejectsIncompleteOrUnknownStatus() {
        assertFalse(service.reportOwnSubscription(report(null, "PRO", 0, null)));
        assertFalse(service.reportOwnSubscription(report("o-2", "PRO", 0, "PAUSED")));
        verify(externalMapper, never()).insertSelective(any(WebsiteExternalSubscription.class));
    }

    @Test
    void linkWebsiteUserBackfillsOnlyUnlinkedRows() {
        when(externalMapper.updateByExampleSelective(any(WebsiteExternalSubscription.class),
                any(WebsiteExternalSubscriptionExample.class))).thenReturn(2);
        assertEquals(2, service.linkWebsiteUser("u-9", WEBSITE_USER));
        assertEquals(0, service.linkWebsiteUser("u-9", null));
        verify(externalMapper, times(1)).updateByExampleSelective(any(WebsiteExternalSubscription.class),
                any(WebsiteExternalSubscriptionExample.class));
    }

    private static void assertRequiresNew(String methodName, Class<?>... parameterTypes) throws Exception {
        Transactional transactional = CentralLedgerService.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private void stubUser(int status) {
        WebsiteUser user = new WebsiteUser();
        user.setUserId(WEBSITE_USER);
        user.setStatus(status);
        when(userMapper.selectOneByExample(any(WebsiteUserExample.class))).thenReturn(user);
    }

    private void stubSubscription(String plan, String status, Date periodEnd) {
        WebsiteUserSubscription sub = new WebsiteUserSubscription();
        sub.setUserId(WEBSITE_USER);
        sub.setPlanCode(plan);
        sub.setStatus(status);
        sub.setPeriodEnd(periodEnd);
        when(subscriptionMapper.selectOneByExample(any(WebsiteUserSubscriptionExample.class))).thenReturn(sub);
    }

    private static WebsiteUserQuota quota(int fiveLimit, int fiveUsed, int weekLimit, int weekUsed, int extra, int version) {
        WebsiteUserQuota quota = new WebsiteUserQuota();
        quota.setId(1L);
        quota.setUserId(WEBSITE_USER);
        quota.setPlanCode("PRO");
        quota.setFiveHourLimitPoints(fiveLimit);
        quota.setFiveHourUsedPoints(fiveUsed);
        quota.setFiveHourWindowStart(new Date());
        quota.setWeeklyLimitPoints(weekLimit);
        quota.setWeeklyUsedPoints(weekUsed);
        quota.setWeeklyAnchorAt(new Date());
        quota.setExtraPoints(extra);
        quota.setQuotaVersion(version);
        return quota;
    }

    private static OwnSubscriptionReport report(String orderNo, String plan, int amount, String status) {
        OwnSubscriptionReport report = new OwnSubscriptionReport();
        report.setExternalUserId("u-9");
        report.setExternalOrderNo(orderNo);
        report.setPlanCode(plan);
        report.setPlanName(plan);
        report.setAmountCent(amount);
        report.setStatus(status);
        return report;
    }
}
