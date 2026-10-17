package com.bgssai.shortai.account.service;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.SysUser;

/**
 * Shared checks for deleted / inactive accounts across login and account APIs.
 */
public final class AccountGuard {

    private AccountGuard() {
    }

    public static void rejectIfDeleted(SysUser user) {
        if (user == null) {
            return;
        }
        if (Boolean.TRUE.equals(user.getDelFlag()) || user.getDeletedAt() != null) {
            throw new BizException(CommonErrorCode.ACCOUNT_DELETED);
        }
    }
}
