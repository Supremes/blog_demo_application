package org.dododo.controller;

import lombok.extern.slf4j.Slf4j;
import org.dododo.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    private Semaphore semaphore = new Semaphore(2);

    @GetMapping("/synchronizers")
    public Map<String, Object> getDashboard() {
        try {
            if (!semaphore.tryAcquire()) {
                log.warn("限流，无法访问");
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "系统繁忙，请稍后再试 (Rate Limited)");
//                return Map.of("code", 503, "message", "系统繁忙，请稍后再试 (Rate Limited)");
            }

            int taskCount = 3;
            CountDownLatch latch = new CountDownLatch(taskCount);
            Map<String, Object> res = new ConcurrentHashMap<>();

            long start = System.currentTimeMillis();

            // 分发任务
            dashboardService.fetchUserInfo(res, latch);
            dashboardService.fetchOrders(res, latch);
            dashboardService.fetchRecommendations(res, latch);

            boolean finished = latch.await(3, TimeUnit.SECONDS);

            long cost = System.currentTimeMillis() - start;
            res.put("processTimeMs", cost);
            res.put("allFinished", finished);

            semaphore.release();
            return res;
        } catch (InterruptedException exception) {
            log.error("InterruptedException: {}", exception.getLocalizedMessage());
            semaphore.release();
            return Map.of("code", 500, "message", "server internal error");
        }
    }

    @GetMapping("/completableFuture")
    public Map<String, Object> getDashboardWithFuture() {
        try {
            if (!semaphore.tryAcquire()) {
                log.warn("限流，无法访问");
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "系统繁忙，请稍后再试 (Rate Limited)");
//                return Map.of("code", 503, "message", "系统繁忙，请稍后再试 (Rate Limited)");
            }

            long start = System.currentTimeMillis();

            // 分发任务
            CompletableFuture<String> userFuture = dashboardService.fetchUserInfo();
            CompletableFuture<String> ordersFuture = dashboardService.fetchOrders();
            CompletableFuture<String> recommendationsFuture = dashboardService.fetchRecommendations();

            CompletableFuture.allOf(userFuture, ordersFuture, recommendationsFuture).join();


            long cost = System.currentTimeMillis() - start;
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("userInfo", userFuture.get()); // 处理可能的 ExecutionException
            finalResult.put("orders", ordersFuture.get());
            finalResult.put("recommendations", recommendationsFuture.get());

            finalResult.put("costTime", System.currentTimeMillis() - start);

            semaphore.release();
            log.warn("成功");
            return finalResult;
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Exception: {}", exception.getLocalizedMessage());
            semaphore.release();
            return Map.of("code", 500, "message", "server internal error");
        }
    }
}
