package com.bgssai.shortai.supportbridge;

public interface CsSupportReceiptStore {
    int insert(CsSupportReceipt row);

    CsSupportReceipt selectOneByExample(CsSupportReceiptExample example);
}
