package com.bgssai.shortai.model;

/** Query criteria for the durable delivery queue, maintained with the mapper XML. */
public class ShortDebitOutboxExample {
    private final int limit;
    public ShortDebitOutboxExample(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("invalid delivery batch size");
        this.limit = limit;
    }
    public int getLimit() { return limit; }
}
