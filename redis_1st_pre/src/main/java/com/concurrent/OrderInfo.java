package com.concurrent;

public class OrderInfo {
    private String productName;
    private Integer amount;
    private Long timestamp = System.currentTimeMillis();

    public OrderInfo(String productName, Integer amount, Long timestamp) {
        this.productName = productName;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
