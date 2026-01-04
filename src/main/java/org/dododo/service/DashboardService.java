package org.dododo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

// 模拟耗时业务1
@Slf4j
@Service
public class DashboardService {

    @Async
    public void fetchUserInfo(Map<String, Object> res, CountDownLatch latch) {
        try {
            Thread.sleep(1000);
            res.put("userInfo", "User: Supremes");
            //log.info("{} - 获取用户信息完成", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    @Async
    public CompletableFuture<String> fetchUserInfo() {
        try {
            Thread.sleep(1000);
            //log.info("{} - 获取用户信息完成", Thread.currentThread().getName());
            return CompletableFuture.completedFuture("User: Supremes");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public void fetchOrders(Map<String, Object> res, CountDownLatch latch) {
        try {
            Thread.sleep(800);
            res.put("orderInfo", "Order ID: 1024, Status: PAID");
            //log.info("{} - 获取订单信息完成", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    @Async
    public CompletableFuture<String> fetchOrders() {
        try {
            Thread.sleep(800);
            //log.info("{} - 获取订单信息完成", Thread.currentThread().getName());
            return CompletableFuture.completedFuture("Order ID: 1024, Status: PAID");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public void fetchRecommendations(Map<String, Object> res, CountDownLatch latch) {
        try {
            Thread.sleep(1500);
            res.put("recommendations", "Item: Java Concurrency Book");
            //log.info("{} - 获取推荐信息完成", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    @Async
    public CompletableFuture<String> fetchRecommendations() {
        try {
            Thread.sleep(1500);
            //log.info("{} - 获取推荐信息完成", Thread.currentThread().getName());
            return CompletableFuture.completedFuture("Item: Java Concurrency Book");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }
}
