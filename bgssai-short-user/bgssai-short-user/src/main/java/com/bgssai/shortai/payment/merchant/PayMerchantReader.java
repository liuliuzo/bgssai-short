package com.bgssai.shortai.payment.merchant;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.AlipayConfigMapper;
import com.bgssai.shortai.mapper.WechatPayConfigMapper;
import com.bgssai.shortai.model.AlipayConfig;
import com.bgssai.shortai.model.AlipayConfigExample;
import com.bgssai.shortai.model.WechatPayConfig;
import com.bgssai.shortai.model.WechatPayConfigExample;
import com.bgssai.shortai.payment.config.PaymentProperties;

@Component
public class PayMerchantReader {

    private static final Logger log = LoggerFactory.getLogger(PayMerchantReader.class);

    @Autowired
    private WechatPayConfigMapper wechatPayConfigMapper;

    @Autowired
    private AlipayConfigMapper alipayConfigMapper;

    @Autowired
    private PaymentProperties paymentProperties;

    public WechatMerchant requireActiveWechatMerchant() {
        WechatPayConfigExample example = new WechatPayConfigExample();
        example.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1)
                .andCollectionActiveEqualTo(1);
        example.setOrderByClause("id asc");
        WechatPayConfig row = wechatPayConfigMapper.selectOneByExample(example);
        if (row == null) {
            log.warn("no active wechat collection merchant configured");
            throw new BizException("60013", "production payment not configured: no active wechat collection merchant");
        }
        log.info("resolved active wechat collection merchant mchId={}", row.getMchId());
        return toWechatMerchant(row);
    }

    public boolean hasActiveCollectionMerchant() {
        WechatPayConfigExample we = new WechatPayConfigExample();
        we.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1)
                .andCollectionActiveEqualTo(1);
        if (wechatPayConfigMapper.selectOneByExample(we) != null) {
            return true;
        }
        AlipayConfigExample ae = new AlipayConfigExample();
        ae.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1)
                .andCollectionActiveEqualTo(1);
        return alipayConfigMapper.selectOneByExample(ae) != null;
    }

    public List<WechatMerchant> listVerifiableWechatMerchants() {
        WechatPayConfigExample example = new WechatPayConfigExample();
        example.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1);
        example.setOrderByClause("is_collection_active desc, id asc");
        List<WechatPayConfig> rows = wechatPayConfigMapper.selectByExample(example);
        List<WechatMerchant> merchants = new ArrayList<>(rows.size());
        for (WechatPayConfig row : rows) {
            WechatMerchant merchant = toWechatMerchant(row);
            if (merchant.hasCompleteCredentials()) {
                merchants.add(merchant);
            }
        }
        log.info("wechat notify verify candidates count={}", merchants.size());
        return merchants;
    }

    public AlipayMerchant requireActiveAlipayMerchant() {
        AlipayConfigExample example = new AlipayConfigExample();
        example.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1)
                .andCollectionActiveEqualTo(1);
        example.setOrderByClause("id asc");
        AlipayConfig row = alipayConfigMapper.selectOneByExample(example);
        if (row == null) {
            log.warn("no active alipay collection merchant configured");
            throw new BizException("60013", "production payment not configured: no active alipay collection merchant");
        }
        log.info("resolved active alipay collection merchant appId={}", row.getAppId());
        return toAlipayMerchant(row);
    }

    public List<AlipayMerchant> listVerifiableAlipayMerchants() {
        AlipayConfigExample example = new AlipayConfigExample();
        example.createCriteria()
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1);
        example.setOrderByClause("is_collection_active desc, id asc");
        List<AlipayConfig> rows = alipayConfigMapper.selectByExample(example);
        List<AlipayMerchant> merchants = new ArrayList<>(rows.size());
        for (AlipayConfig row : rows) {
            AlipayMerchant merchant = toAlipayMerchant(row);
            if (merchant.hasCompleteCredentials()) {
                merchants.add(merchant);
            }
        }
        log.info("alipay query/verify candidates count={}", merchants.size());
        return merchants;
    }

    public AlipayMerchant findAlipayMerchantByAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            log.warn("alipay notify carries no app_id");
            return null;
        }
        AlipayConfigExample example = new AlipayConfigExample();
        example.createCriteria()
                .andAppIdEqualTo(appId.trim())
                .andDelFlagEqualTo(0)
                .andEnabledEqualTo(1);
        AlipayConfig row = alipayConfigMapper.selectOneByExample(example);
        if (row == null) {
            log.warn("alipay notify app_id not matched to an enabled merchant appId={}", appId);
            return null;
        }
        log.info("alipay notify routed to merchant appId={}", row.getAppId());
        return toAlipayMerchant(row);
    }

    private WechatMerchant toWechatMerchant(WechatPayConfig row) {
        return new WechatMerchant(
                row.getMchId(),
                row.getAppId(),
                row.getMerchantSerialNo(),
                row.getMchPrivateKey(),
                row.getApiV3Key(),
                resolveNotifyUrl(row.getNotifyUrl(), "/bgssai/user/payment/wechat/notify"));
    }

    private AlipayMerchant toAlipayMerchant(AlipayConfig row) {
        return new AlipayMerchant(
                row.getAppId(),
                row.getAppPrivateKey(),
                row.getAlipayPublicKey(),
                row.getSignType(),
                row.getCharset(),
                row.getGatewayUrl(),
                resolveNotifyUrl(row.getNotifyUrl(), "/bgssai/user/payment/alipay/notify"),
                row.getReturnUrl());
    }

    private String resolveNotifyUrl(String configured, String path) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String base = paymentProperties.getNotifyBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + path;
    }
}
