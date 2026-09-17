package com.bgssai.shortai.login.sms;

/**
 * 单次短信发码结果：写入模板的产品名与序号（供发码 API 回显）。
 *
 * <p>序号由本进程递增生成，供客服对照短信正文。
 */
public final class SmsSendResult {

    private final String product;
    private final long seq;

    public SmsSendResult(String product, long seq) {
        this.product = product;
        this.seq = seq;
    }

    public String getProduct() {
        return product;
    }

    public long getSeq() {
        return seq;
    }
}
