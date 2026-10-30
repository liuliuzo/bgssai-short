package com.bgssai.shortai.quota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.UserQuota;

class QuotaConsumeServiceTest {

    private UserQuotaMapper userQuotaMapper;
    private QuotaWalletService quotaWalletService;
    private QuotaConsumeService service;

    @BeforeEach
    void setUp() {
        userQuotaMapper = org.mockito.Mockito.mock(UserQuotaMapper.class);
        quotaWalletService = org.mockito.Mockito.mock(QuotaWalletService.class);
        service = new QuotaConsumeService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "debitOutbox", org.mockito.Mockito.mock(ShortDebitOutbox.class));
        ReflectionTestUtils.setField(service, "userQuotaMapper", userQuotaMapper);
        ReflectionTestUtils.setField(service, "quotaChangeLogMapper",
                org.mockito.Mockito.mock(QuotaChangeLogMapper.class));
        ReflectionTestUtils.setField(service, "quotaWalletService", quotaWalletService);
    }

    private UserQuota quota(int balance, int extra, int spent, int limit) {
        UserQuota q = new UserQuota();
        q.setId(1L);
        q.setBalanceCent(balance);
        q.setExtraBalanceCent(extra);
        q.setMonthSpentCent(spent);
        q.setMonthlyLimitCent(limit);
        q.setMonthResetDate(new java.util.Date());
        return q;
    }

    @Test
    void consumePeriodThenExtra() {
        UserQuota q = quota(30, 20, 0, 1000);
        when(quotaWalletService.requireWallet("u-1")).thenReturn(q);
        when(userQuotaMapper.updateByExampleSelective(any(), any())).thenReturn(1);
        service.consume("u-1", 40);
        assertEquals(30, q.getBalanceCent());
    }

    @Test
    void assertAffordableInsufficientThrows4007() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(10, 5, 0, 1000));
        BizException ex = assertThrows(BizException.class, () -> service.assertAffordable("u-1", 20));
        assertEquals("4007", ex.getCode());
    }

    @Test
    void assertAffordableMonthCapThrows4015() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(500, 500, 90, 100));
        BizException ex = assertThrows(BizException.class, () -> service.assertAffordable("u-1", 20));
        assertEquals("4015", ex.getCode());
    }

    @Test
    void insufficientThrows4007() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(10, 5, 0, 1000));
        BizException ex = assertThrows(BizException.class, () -> service.consume("u-1", 20));
        assertEquals("4007", ex.getCode());
    }

    @Test
    void monthCapThrows4015() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(500, 500, 90, 100));
        BizException ex = assertThrows(BizException.class, () -> service.consume("u-1", 20));
        assertEquals("4015", ex.getCode());
    }

    @Test
    void unboundUserRejectedWhenBgssaiWebsiteLedgerClientInjected() {
        com.bgssai.shortai.mapper.OauthBindingMapper bindingMapper =
                org.mockito.Mockito.mock(com.bgssai.shortai.mapper.OauthBindingMapper.class);
        com.bgssai.shortai.centralledger.service.CentralLedgerService centralLedger =
                org.mockito.Mockito.mock(com.bgssai.shortai.centralledger.service.CentralLedgerService.class);
        com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient ledgerClient =
                new com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient(bindingMapper, centralLedger);
        service.setBgssaiWebsiteLedgerClient(ledgerClient);

        IllegalStateException ex1 = assertThrows(
                IllegalStateException.class, () -> service.assertAffordable("unbound-u", 20));
        org.junit.jupiter.api.Assertions.assertTrue(ex1.getMessage().contains("bgssai-chat"));

        IllegalStateException ex2 = assertThrows(
                IllegalStateException.class,
                () -> service.consumeLlmQuota("unbound-u", 200L, "bgssai-chat", "idem-1"));
        org.junit.jupiter.api.Assertions.assertTrue(ex2.getMessage().contains("bgssai-chat"));
    }

    @Test
    void boundUserDelegatesQuotaCheckAndDeductionToBgssaiChatCentralLedger() {
        com.bgssai.shortai.mapper.OauthBindingMapper bindingMapper =
                org.mockito.Mockito.mock(com.bgssai.shortai.mapper.OauthBindingMapper.class);
        com.bgssai.shortai.model.OauthBinding binding = new com.bgssai.shortai.model.OauthBinding();
        binding.setUserId("u-1");
        binding.setPlatform("bgssai");
        binding.setOpenId("chat-u-1");
        when(bindingMapper.selectOneByExample(any())).thenReturn(binding);

        com.bgssai.shortai.centralledger.service.CentralLedgerService centralLedger =
                org.mockito.Mockito.mock(com.bgssai.shortai.centralledger.service.CentralLedgerService.class);
        when(centralLedger.hasAvailableQuota("chat-u-1", 40L)).thenReturn(true);
        when(centralLedger.consumeQuota("chat-u-1", 40L, "1x", "quota_cent", "task:t-1", "short task debit"))
                .thenReturn(new com.bgssai.shortai.centralledger.service.CentralLedgerService.ConsumeResult(
                        true, "chat-u-1", 40L, 40L, 1.0d, "quota_cent", "task:t-1"));
        when(centralLedger.consumeQuota("chat-u-1", 200L, "bgssai-chat", "llm_tokens", "llm:1", "short llm call"))
                .thenReturn(new com.bgssai.shortai.centralledger.service.CentralLedgerService.ConsumeResult(
                        true, "chat-u-1", 200L, 100L, 0.5d, "llm_tokens", "llm:1"));

        com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient ledgerClient =
                new com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient(bindingMapper, centralLedger);
        service.setBgssaiWebsiteLedgerClient(ledgerClient);

        service.assertAffordable("u-1", 40);
        service.consume("u-1", 40, "task:t-1");
        var res = service.consumeLlmQuota("u-1", 200L, "bgssai-chat", "llm:1");
        org.junit.jupiter.api.Assertions.assertTrue(res.isSuccess());
        assertEquals(100L, res.billedTokens());
        assertEquals(0.5d, res.multiplier());
    }
}
