package org.dododo.controller;

import lombok.RequiredArgsConstructor;
import org.dododo.service.BusinessMetricsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务指标测试接口
 */
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessMetricsController {

    private final BusinessMetricsService businessMetricsService;

    /**
     * 创建订单
     * 测试: curl -X POST "http://localhost:8080/api/business/order?userId=user001&amount=99.9"
     */
    @PostMapping("/order")
    public Map<String, Object> createOrder(
            @RequestParam String userId,
            @RequestParam double amount) {
        Map<String, Object> result = new HashMap<>();
        try {
            String orderId = businessMetricsService.createOrder(userId, amount);
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("message", "订单创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 处理支付
     * 测试: curl -X POST "http://localhost:8080/api/business/payment?orderId=ORDER_123&amount=99.9"
     */
    @PostMapping("/payment")
    public Map<String, Object> processPayment(
            @RequestParam String orderId,
            @RequestParam double amount) {
        Map<String, Object> result = new HashMap<>();
        boolean success = businessMetricsService.processPayment(orderId, amount);
        result.put("success", success);
        result.put("message", success ? "支付成功" : "支付失败");
        return result;
    }

    /**
     * 用户登录
     * 测试: curl -X POST "http://localhost:8080/api/business/login?userId=user001"
     */
    @PostMapping("/login")
    public Map<String, Object> userLogin(@RequestParam String userId) {
        businessMetricsService.userLogin(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登录成功");
        return result;
    }

    /**
     * 用户登出
     * 测试: curl -X POST "http://localhost:8080/api/business/logout?userId=user001"
     */
    @PostMapping("/logout")
    public Map<String, Object> userLogout(@RequestParam String userId) {
        businessMetricsService.userLogout(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    /**
     * 添加任务到队列
     * 测试: curl -X POST "http://localhost:8080/api/business/queue/add"
     */
    @PostMapping("/queue/add")
    public Map<String, Object> addToQueue() {
        businessMetricsService.addToQueue();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "任务已添加到队列");
        return result;
    }

    /**
     * 从队列移除任务
     * 测试: curl -X POST "http://localhost:8080/api/business/queue/remove"
     */
    @PostMapping("/queue/remove")
    public Map<String, Object> removeFromQueue() {
        businessMetricsService.removeFromQueue();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "任务已从队列移除");
        return result;
    }

    /**
     * 记录自定义事件
     * 测试: curl -X POST "http://localhost:8080/api/business/event?type=user_action&status=success"
     */
    @PostMapping("/event")
    public Map<String, Object> recordEvent(
            @RequestParam String type,
            @RequestParam String status) {
        businessMetricsService.recordCustomEvent(type, status);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "事件已记录");
        return result;
    }

    /**
     * 批量测试 - 模拟真实业务场景
     * 测试: curl -X POST "http://localhost:8080/api/business/simulate?count=10"
     */
    @PostMapping("/simulate")
    public Map<String, Object> simulateBusinessScenario(@RequestParam(defaultValue = "10") int count) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        
        for (int i = 0; i < count; i++) {
            try {
                // 模拟用户登录
                businessMetricsService.userLogin("user" + i);
                
                // 模拟创建订单
                String orderId = businessMetricsService.createOrder("user" + i, 100.0 + i);
                
                // 模拟支付
                boolean paymentSuccess = businessMetricsService.processPayment(orderId, 100.0 + i);
                
                if (paymentSuccess) {
                    successCount++;
                }
                
                // 部分用户登出
                if (i % 3 == 0) {
                    businessMetricsService.userLogout("user" + i);
                }
                
                Thread.sleep(100); // 避免过快
            } catch (Exception e) {
                // 记录失败
            }
        }
        
        result.put("success", true);
        result.put("totalRequests", count);
        result.put("successCount", successCount);
        result.put("message", "业务场景模拟完成");
        return result;
    }
}
