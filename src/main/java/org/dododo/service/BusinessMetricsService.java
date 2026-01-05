package org.dododo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务指标服务示例
 * 演示如何使用 Micrometer 添加自定义业务指标
 */
@Slf4j
@Service
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counter: 计数器，只增不减
    private final Counter orderCounter;
    private final Counter orderFailureCounter;
    
    // Gauge: 仪表，可增可减，用于实时值
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    
    // Timer: 计时器，用于记录操作耗时
    private final Timer orderProcessTimer;
    private final Timer paymentTimer;
    
    private final Random random = new Random();

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 创建计数器 - 订单总数
        this.orderCounter = Counter.builder("business.orders.total")
                .description("订单总数")
                .tag("type", "all")
                .register(meterRegistry);
        
        // 创建计数器 - 订单失败数
        this.orderFailureCounter = Counter.builder("business.orders.failed")
                .description("订单失败数")
                .tag("type", "failed")
                .register(meterRegistry);
        
        // 创建仪表 - 活跃用户数
        Gauge.builder("business.users.active", activeUsers, AtomicInteger::get)
                .description("当前活跃用户数")
                .register(meterRegistry);
        
        // 创建仪表 - 队列大小
        Gauge.builder("business.queue.size", queueSize, AtomicInteger::get)
                .description("待处理队列大小")
                .register(meterRegistry);
        
        // 创建计时器 - 订单处理耗时
        this.orderProcessTimer = Timer.builder("business.order.process.duration")
                .description("订单处理耗时")
                .tag("operation", "process")
                .register(meterRegistry);
        
        // 创建计时器 - 支付处理耗时
        this.paymentTimer = Timer.builder("business.payment.duration")
                .description("支付处理耗时")
                .tag("operation", "payment")
                .register(meterRegistry);
    }

    /**
     * 创建订单
     */
    public String createOrder(String userId, double amount) {
        // 增加订单计数
        orderCounter.increment();
        
        // 记录订单处理耗时
        return orderProcessTimer.record(() -> {
            try {
                // 模拟订单处理
                Thread.sleep(random.nextInt(100) + 50);
                
                // 70% 的概率成功
                if (random.nextDouble() < 0.7) {
                    log.info("订单创建成功: userId={}, amount={}", userId, amount);
                    return "ORDER_" + System.currentTimeMillis();
                } else {
                    orderFailureCounter.increment();
                    log.warn("订单创建失败: userId={}, amount={}", userId, amount);
                    throw new RuntimeException("订单创建失败");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                orderFailureCounter.increment();
                throw new RuntimeException("订单处理被中断", e);
            }
        });
    }

    /**
     * 处理支付
     */
    public boolean processPayment(String orderId, double amount) {
        return paymentTimer.record(() -> {
            try {
                // 模拟支付处理
                Thread.sleep(random.nextInt(200) + 100);
                
                boolean success = random.nextDouble() < 0.9; // 90% 成功率
                log.info("支付处理: orderId={}, amount={}, success={}", orderId, amount, success);
                
                // 可以根据支付结果添加更多指标
                if (success) {
                    // 记录支付金额
                    meterRegistry.counter("business.payment.amount", 
                            "status", "success")
                            .increment(amount);
                } else {
                    meterRegistry.counter("business.payment.amount", 
                            "status", "failed")
                            .increment(amount);
                }
                
                return success;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }

    /**
     * 用户登录
     */
    public void userLogin(String userId) {
        activeUsers.incrementAndGet();
        log.info("用户登录: userId={}, 当前活跃用户数: {}", userId, activeUsers.get());
        
        // 也可以用 Counter 记录总登录次数
        meterRegistry.counter("business.user.login.total", 
                "event", "login").increment();
    }

    /**
     * 用户登出
     */
    public void userLogout(String userId) {
        activeUsers.decrementAndGet();
        log.info("用户登出: userId={}, 当前活跃用户数: {}", userId, activeUsers.get());
        
        meterRegistry.counter("business.user.logout.total", 
                "event", "logout").increment();
    }

    /**
     * 添加任务到队列
     */
    public void addToQueue() {
        queueSize.incrementAndGet();
        log.debug("任务入队，当前队列大小: {}", queueSize.get());
    }

    /**
     * 从队列移除任务
     */
    public void removeFromQueue() {
        queueSize.decrementAndGet();
        log.debug("任务出队，当前队列大小: {}", queueSize.get());
    }

    /**
     * 记录自定义事件
     */
    public void recordCustomEvent(String eventType, String eventStatus) {
        meterRegistry.counter("business.custom.event",
                "type", eventType,
                "status", eventStatus)
                .increment();
        
        log.info("记录自定义事件: type={}, status={}", eventType, eventStatus);
    }

    /**
     * 记录带标签的业务指标
     */
    public void recordBusinessMetric(String metricName, String businessType, double value) {
        meterRegistry.counter(metricName,
                "business_type", businessType)
                .increment(value);
    }
}
