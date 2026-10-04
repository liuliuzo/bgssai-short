package com.bgssai.shortai.supportbridge;

import java.util.List;

public interface CsSupportOutboxStore {
    int insert(CsSupportOutbox row);

    List<CsSupportOutbox> selectByExample(CsSupportOutboxExample example);

    int updateByExampleSelective(CsSupportOutbox record, CsSupportOutboxExample example);
}
