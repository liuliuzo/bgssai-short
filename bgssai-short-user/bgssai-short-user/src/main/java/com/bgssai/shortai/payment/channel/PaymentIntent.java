package com.bgssai.shortai.payment.channel;

public class PaymentIntent {

    private final String orderNo;
    private final int amountCent;
    private final String subject;

    public PaymentIntent(String orderNo, int amountCent, String subject) {
        this.orderNo = orderNo;
        this.amountCent = amountCent;
        this.subject = subject;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public int getAmountCent() {
        return amountCent;
    }

    public String getSubject() {
        return subject;
    }
}
