package com.concurrent;

import java.util.HashMap;
import java.util.Map;

public class OrderService {
    // 상품 DB
    private final Map<String, Integer> productDatabase = new HashMap<>();
    // 가장 최근 주문 정보를 저장하는 DB
    private final Map<String, OrderInfo> latestOrderDatabase = new HashMap<>();

    public OrderService() {
        // 초기 상품 데이터
        productDatabase.put("apple", 100);
        productDatabase.put("banana", 50);
        productDatabase.put("orange", 75);
    }

    // 주문 처리 메서드
    public void order(String productName, int amount) {
        Integer currentStock = productDatabase.getOrDefault(productName, 0);

        try {
            Thread.sleep(1); // 동시성 이슈 유발을 위한 인위적 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (currentStock >= amount) {
            System.out.println("Current Thread : " + Thread.currentThread().getName() +
                    " - CurrentStock : " + currentStock + " - Order : " + amount);
            productDatabase.put(productName, currentStock - amount);
            latestOrderDatabase.put(productName, new OrderInfo(productName, amount, System.currentTimeMillis()));
        }
    }

    // 재고 조회
    public int getStock(String productName) {
        return productDatabase.getOrDefault(productName, 0);
    }
}
