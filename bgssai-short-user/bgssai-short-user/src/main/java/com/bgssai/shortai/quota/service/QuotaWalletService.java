package com.bgssai.shortai.quota.service;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;

@Service
public class QuotaWalletService {

    @Autowired
    private UserQuotaMapper userQuotaMapper;

    public UserQuota requireWallet(String userId) {
        UserQuotaExample ex = new UserQuotaExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(0);
        UserQuota row = userQuotaMapper.selectOneByExample(ex);
        if (row != null) {
            return row;
        }
        Date now = new Date();
        UserQuota created = new UserQuota();
        created.setUserId(userId);
        created.setBalanceCent(0);
        created.setExtraBalanceCent(0);
        created.setMonthlyLimitCent(0);
        created.setMonthSpentCent(0);
        created.setMonthResetDate(now);
        created.setDelFlag(0);
        created.setCreator(userId);
        created.setModifier(userId);
        created.setGmtCreate(now);
        created.setGmtModified(now);
        userQuotaMapper.insertSelective(created);
        return created;
    }

    public void resetMonthIfNeeded(UserQuota quota) {
        if (quota.getMonthResetDate() != null && sameMonth(quota.getMonthResetDate(), new Date())) {
            return;
        }
        UserQuota patch = new UserQuota();
        patch.setMonthSpentCent(0);
        patch.setMonthResetDate(new Date());
        patch.setGmtModified(new Date());
        UserQuotaExample ex = new UserQuotaExample();
        ex.createCriteria().andIdEqualTo(quota.getId()).andDelFlagEqualTo(0);
        userQuotaMapper.updateByExampleSelective(patch, ex);
        quota.setMonthSpentCent(0);
        quota.setMonthResetDate(patch.getMonthResetDate());
    }

    private static boolean sameMonth(Date a, Date b) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH);
    }
}
