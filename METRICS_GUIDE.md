# 业务指标上报指南

## 📊 Micrometer 指标类型

### 1. Counter (计数器)
**特点**: 只增不减，用于累计值
**适用场景**: 
- 订单总数
- 请求总数
- 错误次数
- 用户注册数

```java
// 创建 Counter
Counter orderCounter = Counter.builder("business.orders.total")
        .description("订单总数")
        .tag("type", "all")
        .register(meterRegistry);

// 使用
orderCounter.increment();           // +1
orderCounter.increment(5);          // +5
```

### 2. Gauge (仪表)
**特点**: 可增可减，表示当前状态
**适用场景**:
- 当前活跃用户数
- 队列大小
- 缓存命中率
- 连接池大小

```java
// 方式1: 使用 AtomicInteger
AtomicInteger activeUsers = new AtomicInteger(0);
Gauge.builder("business.users.active", activeUsers, AtomicInteger::get)
        .description("当前活跃用户数")
        .register(meterRegistry);

activeUsers.incrementAndGet();  // 增加
activeUsers.decrementAndGet();  // 减少

// 方式2: 使用集合的 size
List<String> taskQueue = new ArrayList<>();
Gauge.builder("business.queue.size", taskQueue, List::size)
        .register(meterRegistry);
```

### 3. Timer (计时器)
**特点**: 记录操作耗时和调用次数
**适用场景**:
- API 接口耗时
- 数据库查询耗时
- 业务处理耗时
- 第三方接口调用耗时

```java
// 创建 Timer
Timer orderProcessTimer = Timer.builder("business.order.process.duration")
        .description("订单处理耗时")
        .tag("operation", "process")
        .register(meterRegistry);

// 使用方式1: record() 方法
String result = orderProcessTimer.record(() -> {
    // 你的业务逻辑
    processOrder();
    return "success";
});

// 使用方式2: recordCallable()
String result = orderProcessTimer.recordCallable(() -> {
    return processOrder();
});

// 使用方式3: 手动计时
Timer.Sample sample = Timer.start(meterRegistry);
try {
    processOrder();
} finally {
    sample.stop(orderProcessTimer);
}
```

### 4. DistributionSummary (分布摘要)
**特点**: 记录数值分布
**适用场景**:
- 请求体大小
- 响应体大小
- 订单金额分布

```java
DistributionSummary summary = DistributionSummary.builder("business.order.amount")
        .description("订单金额分布")
        .baseUnit("yuan")
        .register(meterRegistry);

summary.record(199.99);  // 记录一笔订单金额
```

## 🏷️ 使用标签 (Tags)

标签用于对指标进行多维度分类：

```java
// 基础用法
meterRegistry.counter("business.orders.total",
        "status", "success",
        "channel", "web")
        .increment();

// 更多示例
meterRegistry.counter("business.api.calls",
        "api", "createOrder",
        "status", "success",
        "client", "mobile")
        .increment();
```

## 🎯 实际应用示例

### 示例1: 订单业务指标

```java
@Service
public class OrderService {
    private final Counter orderCounter;
    private final Counter orderFailCounter;
    private final Timer orderProcessTimer;
    
    public OrderService(MeterRegistry registry) {
        this.orderCounter = registry.counter("business.orders.total");
        this.orderFailCounter = registry.counter("business.orders.failed");
        this.orderProcessTimer = registry.timer("business.order.process.time");
    }
    
    public Order createOrder(OrderRequest request) {
        return orderProcessTimer.record(() -> {
            try {
                Order order = doCreateOrder(request);
                orderCounter.increment();
                
                // 记录订单金额
                registry.counter("business.order.amount", 
                        "currency", "CNY")
                        .increment(order.getAmount());
                
                return order;
            } catch (Exception e) {
                orderFailCounter.increment();
                throw e;
            }
        });
    }
}
```

### 示例2: API 接口指标

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    private final MeterRegistry registry;
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            User user = userService.findById(id);
            
            // 记录成功的 API 调用
            registry.counter("api.calls",
                    "endpoint", "getUser",
                    "status", "success")
                    .increment();
            
            return user;
        } catch (Exception e) {
            // 记录失败的 API 调用
            registry.counter("api.calls",
                    "endpoint", "getUser",
                    "status", "error")
                    .increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("api.duration")
                    .tag("endpoint", "getUser")
                    .register(registry));
        }
    }
}
```

### 示例3: 缓存指标

```java
@Service
public class CacheService {
    private final AtomicInteger cacheSize = new AtomicInteger(0);
    private final Counter cacheHit;
    private final Counter cacheMiss;
    
    public CacheService(MeterRegistry registry) {
        // 缓存大小
        Gauge.builder("cache.size", cacheSize, AtomicInteger::get)
                .register(registry);
        
        // 缓存命中
        this.cacheHit = registry.counter("cache.hits");
        this.cacheMiss = registry.counter("cache.misses");
    }
    
    public Object get(String key) {
        Object value = cache.get(key);
        if (value != null) {
            cacheHit.increment();
        } else {
            cacheMiss.increment();
        }
        return value;
    }
}
```

## 📈 在 Prometheus 中查询

```promql
# 订单总数
business_orders_total

# 订单失败率
rate(business_orders_failed_total[5m]) / rate(business_orders_total[5m])

# 订单处理平均耗时（秒）
rate(business_order_process_duration_seconds_sum[5m]) / rate(business_order_process_duration_seconds_count[5m])

# 订单处理 P95 耗时
histogram_quantile(0.95, rate(business_order_process_duration_seconds_bucket[5m]))

# 活跃用户数
business_users_active

# 按标签过滤
business_orders_total{application="blogDemoApplication", status="success"}
```

## 🔗 测试接口

项目已提供以下测试接口：

```bash
# 1. 创建订单
curl -X POST "http://localhost:8080/api/business/order?userId=user001&amount=99.9"

# 2. 处理支付
curl -X POST "http://localhost:8080/api/business/payment?orderId=ORDER_123&amount=99.9"

# 3. 用户登录
curl -X POST "http://localhost:8080/api/business/login?userId=user001"

# 4. 用户登出
curl -X POST "http://localhost:8080/api/business/logout?userId=user001"

# 5. 模拟业务场景（批量测试）
curl -X POST "http://localhost:8080/api/business/simulate?count=100"

# 6. 记录自定义事件
curl -X POST "http://localhost:8080/api/business/event?type=user_action&status=success"
```

## 📊 在 Grafana 中可视化

创建 Panel 查询示例：

### 订单数量面板
- Metric: `business_orders_total`
- Legend: `{{application}} - Total Orders`

### 订单处理耗时面板
- Metric: `rate(business_order_process_duration_seconds_sum[5m]) / rate(business_order_process_duration_seconds_count[5m])`
- Legend: `Average Processing Time`

### 活跃用户数面板
- Metric: `business_users_active`
- Legend: `Active Users`

## 💡 最佳实践

1. **命名规范**: 使用小写字母和下划线，如 `business_orders_total`
2. **使用标签**: 通过标签区分不同维度，而不是创建多个指标
3. **单位**: 在描述中明确单位（秒、字节、元等）
4. **避免高基数**: 不要使用用户ID等高基数值作为标签
5. **定期清理**: Timer 和 DistributionSummary 会占用内存，注意控制数量

## 参考文档

- [Micrometer 官方文档](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus 最佳实践](https://prometheus.io/docs/practices/naming/)
