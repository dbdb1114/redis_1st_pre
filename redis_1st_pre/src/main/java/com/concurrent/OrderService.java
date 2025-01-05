package com.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService {
    // 상품 DB
    private final ConcurrentHashMap<String, Integer> productDatabase = new ConcurrentHashMap<>();
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
        try {
            Thread.sleep(1); // 동시성 이슈 유발을 위한 인위적 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        productDatabase.compute(productName,(key, currentStock)->{ // 원자적 연산처리
            if (currentStock >= amount) {
                System.out.printf("%s 주문 정보: \n\t %s: 1건 ([%d])\n", Thread.currentThread().getName().substring(7), productName, amount);
                latestOrderDatabase.put(productName, new OrderInfo(productName, amount, System.currentTimeMillis()));
                return currentStock - amount;
            } else {
                return currentStock;
            }
        });
    }

    public synchronized void orderWithSynchronized(String productName, int amount){
        int currentStock = productDatabase.get(productName);

        try {
            Thread.sleep(1); // 동시성 이슈 유발을 위한 인위적 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (currentStock >= amount) {
            System.out.printf("%s 주문 정보: \n\t %s: 1건 ([%d])\n", Thread.currentThread().getName().substring(7), productName, amount);
            latestOrderDatabase.put(productName, new OrderInfo(productName, amount, System.currentTimeMillis()));
            productDatabase.put(productName, currentStock - amount);
        }
    }

    // 재고 조회
    public int getStock(String productName) {
        return productDatabase.getOrDefault(productName, 0);
    }
}
