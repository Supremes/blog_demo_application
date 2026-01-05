# Grafana Dashboard 创建指南

## 📊 方法一：导入预配置的 Dashboard（推荐）

### 步骤：

1. **访问 Grafana**
   ```
   http://localhost:3000
   用户名: admin
   密码: admin
   ```

2. **添加 Prometheus 数据源**（首次配置）
   - 点击左侧菜单 ⚙️ **Configuration** → **Data sources**
   - 点击 **Add data source**
   - 选择 **Prometheus**
   - 配置：
     - **Name**: `prometheus`
     - **URL**: `http://prometheus:9090`
     - 点击 **Save & test**，确保显示 "Data source is working"

3. **导入 Dashboard**
   - 点击左侧菜单 **+** → **Import dashboard** 或 **Dashboards** → **Import**
   - 有三种导入方式：

   **方式 A: 上传 JSON 文件**
   ```bash
   # 直接上传文件
   grafana-dashboards/business-metrics-dashboard.json
   ```

   **方式 B: 通过浏览器复制粘贴**
   - 打开 `business-metrics-dashboard.json` 文件
   - 复制整个 JSON 内容
   - 在 Grafana 导入页面粘贴

   **方式 C: 使用 curl 命令导入**
   ```bash
   # 在项目根目录执行
   curl -X POST \
     http://admin:admin@localhost:3000/api/dashboards/db \
     -H "Content-Type: application/json" \
     -d @grafana-dashboards/business-metrics-dashboard.json
   ```

4. **选择数据源**
   - 在导入页面，确保 **Prometheus** 选择正确的数据源
   - 点击 **Import**

5. **查看 Dashboard**
   - Dashboard 会自动打开
   - 包含 7 个预配置的面板：
     - 订单创建速率
     - 订单总数
     - 当前活跃用户
     - 订单处理耗时（P95/P99）
     - 订单失败率
     - 支付处理耗时
     - 待处理队列大小

---

## 📝 方法二：手动创建 Dashboard

### 第一步：创建新 Dashboard

1. 点击左侧菜单 **+** → **Create Dashboard**
2. 点击 **Add visualization**
3. 选择数据源 **Prometheus**

### 第二步：添加面板示例

#### 面板 1: 订单总数（Stat 类型）

**Panel 配置：**
- **Title**: `订单总数`
- **Panel type**: `Stat`
- **Metric**: 
  ```promql
  business_orders_total{application="blogDemoApplication"}
  ```
- **Options**:
  - Graph mode: Area
  - Color mode: Value
  - Text mode: Auto

**效果**: 显示当前订单总数的大数字

---

#### 面板 2: 订单创建速率（Time series 类型）

**Panel 配置：**
- **Title**: `订单创建速率`
- **Panel type**: `Time series`
- **Metric**: 
  ```promql
  rate(business_orders_total{application="blogDemoApplication"}[5m]) * 60
  ```
- **Legend**: `订单创建速率 (每分钟)`
- **Unit**: `short`

**效果**: 显示每分钟创建的订单数趋势图

---

#### 面板 3: 活跃用户数（Gauge 类型）

**Panel 配置：**
- **Title**: `当前活跃用户`
- **Panel type**: `Stat` 或 `Gauge`
- **Metric**: 
  ```promql
  business_users_active{application="blogDemoApplication"}
  ```
- **Thresholds**:
  - Green: 0
  - Yellow: 10
  - Red: 50

**效果**: 实时显示活跃用户数

---

#### 面板 4: 订单处理耗时（Time series 类型）

**Panel 配置：**
- **Title**: `订单处理耗时`
- **Panel type**: `Time series`
- **Metrics**: 
  
  Query A - 平均耗时:
  ```promql
  rate(business_order_process_duration_seconds_sum{application="blogDemoApplication"}[5m]) 
  / 
  rate(business_order_process_duration_seconds_count{application="blogDemoApplication"}[5m])
  ```
  
  Query B - P95 耗时:
  ```promql
  histogram_quantile(0.95, rate(business_order_process_duration_seconds_bucket{application="blogDemoApplication"}[5m]))
  ```
  
  Query C - P99 耗时:
  ```promql
  histogram_quantile(0.99, rate(business_order_process_duration_seconds_bucket{application="blogDemoApplication"}[5m]))
  ```

- **Unit**: `seconds (s)`
- **Legend**: 显示在表格模式

**效果**: 显示订单处理的平均、P95、P99 耗时

---

#### 面板 5: 订单失败率（Time series 类型）

**Panel 配置：**
- **Title**: `订单失败率`
- **Panel type**: `Time series`
- **Metric**: 
  ```promql
  rate(business_orders_failed_total{application="blogDemoApplication"}[5m]) 
  / 
  rate(business_orders_total{application="blogDemoApplication"}[5m])
  ```
- **Unit**: `percent (0.0-1.0)`
- **Thresholds**:
  - Green: < 0.05 (5%)
  - Yellow: 0.05 - 0.10
  - Red: > 0.10

**效果**: 实时显示订单失败率

---

#### 面板 6: 支付处理耗时（Time series 类型）

**Panel 配置：**
- **Title**: `支付处理耗时`
- **Panel type**: `Time series`
- **Metric**: 
  ```promql
  rate(business_payment_duration_seconds_sum{application="blogDemoApplication"}[5m]) 
  / 
  rate(business_payment_duration_seconds_count{application="blogDemoApplication"}[5m])
  ```
- **Unit**: `seconds (s)`

**效果**: 显示平均支付处理耗时

---

### 第三步：配置变量（Variables）

让 Dashboard 支持多应用切换：

1. 点击 Dashboard 右上角 ⚙️ **Settings**
2. 选择 **Variables** 标签
3. 点击 **Add variable**
4. 配置：
   - **Name**: `application`
   - **Label**: `Application`
   - **Type**: `Query`
   - **Data source**: `Prometheus`
   - **Query**: 
     ```promql
     label_values(business_orders_total, application)
     ```
   - **Refresh**: `On Dashboard Load`
5. 点击 **Apply**

然后在所有查询中使用 `$application` 变量：
```promql
business_orders_total{application="$application"}
```

---

### 第四步：配置 Dashboard 设置

1. 点击右上角 ⚙️ **Settings**
2. **General**:
   - **Name**: `业务指标监控`
   - **Tags**: `business`, `metrics`
3. **Time options**:
   - **Timezone**: `Browser Time`
   - **Auto refresh**: `10s` 或 `30s`
   - **Time range**: `Last 1 hour`
4. 点击 **Save dashboard**

---

## 🎨 常用面板类型说明

### 1. **Stat** - 统计值
- 适用于：显示单一数值
- 示例：总订单数、当前用户数

### 2. **Time series** - 时序图
- 适用于：显示数据随时间变化的趋势
- 示例：QPS、耗时、错误率

### 3. **Gauge** - 仪表盘
- 适用于：显示百分比或范围内的值
- 示例：CPU使用率、内存使用率

### 4. **Bar gauge** - 条形图
- 适用于：对比多个值
- 示例：不同服务的请求量对比

### 5. **Table** - 表格
- 适用于：显示详细的数据列表
- 示例：错误日志、慢查询

### 6. **Pie chart** - 饼图
- 适用于：显示占比
- 示例：不同状态订单的占比

---

## 🔍 常用 PromQL 查询示例

### 基础查询
```promql
# 获取当前值
business_orders_total

# 按标签过滤
business_orders_total{application="blogDemoApplication"}

# 多条件过滤
business_orders_total{application="blogDemoApplication", status="success"}
```

### 速率计算
```promql
# 每秒速率
rate(business_orders_total[5m])

# 每分钟速率
rate(business_orders_total[5m]) * 60

# 瞬时速率
irate(business_orders_total[1m])
```

### 聚合函数
```promql
# 总和
sum(business_orders_total)

# 按标签分组求和
sum by (application) (business_orders_total)

# 平均值
avg(business_users_active)

# 最大值
max(business_users_active)

# 最小值
min(business_users_active)
```

### 计算百分比
```promql
# 失败率
rate(business_orders_failed_total[5m]) / rate(business_orders_total[5m])

# 成功率
1 - (rate(business_orders_failed_total[5m]) / rate(business_orders_total[5m]))
```

### 时间窗口统计
```promql
# 5分钟内的变化量
increase(business_orders_total[5m])

# 过去1小时的平均值
avg_over_time(business_users_active[1h])

# 过去5分钟的最大值
max_over_time(business_users_active[5m])
```

### Timer 类型指标
```promql
# 平均耗时
rate(business_order_process_duration_seconds_sum[5m]) 
/ 
rate(business_order_process_duration_seconds_count[5m])

# P95 耗时
histogram_quantile(0.95, rate(business_order_process_duration_seconds_bucket[5m]))

# P99 耗时
histogram_quantile(0.99, rate(business_order_process_duration_seconds_bucket[5m]))

# 请求频率
rate(business_order_process_duration_seconds_count[5m])
```

---

## 💡 实用技巧

### 1. 设置告警规则

在面板上设置告警：
1. 编辑面板
2. 切换到 **Alert** 标签
3. 创建告警规则：
   ```
   WHEN avg() OF query(A, 5m, now) IS ABOVE 0.1
   ```
4. 配置通知渠道（Email、Slack、钉钉等）

### 2. 使用模板变量

在查询中使用变量使 Dashboard 更灵活：
```promql
business_orders_total{application="$application", instance="$instance"}
```

### 3. 添加注释

为重要事件添加标记：
- Dashboards → Annotations
- 配置时间点和说明

### 4. 导出和分享

**导出 JSON**:
- Dashboard settings → JSON Model
- 复制 JSON 配置

**分享链接**:
- 点击 Share dashboard
- 获取链接或嵌入代码

### 5. 使用行（Rows）组织面板

对相关面板分组：
- 添加 Row
- 将相关面板拖入同一行
- 可折叠展开

---

## 🚀 快速测试

生成测试数据：
```bash
# 模拟 100 个业务操作
curl -X POST "http://localhost:8080/api/business/simulate?count=100"

# 持续生成数据
while true; do 
  curl -X POST "http://localhost:8080/api/business/simulate?count=10"
  sleep 5
done
```

---

## 📚 参考资源

- [Grafana 官方文档](https://grafana.com/docs/grafana/latest/)
- [PromQL 查询语法](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboard 最佳实践](https://grafana.com/docs/grafana/latest/best-practices/)
- [预配置的 Dashboard 市场](https://grafana.com/grafana/dashboards/)

---

## 🎯 下一步

1. ✅ 导入预配置的业务指标 Dashboard
2. 📊 根据实际需求自定义面板
3. 🔔 配置告警规则
4. 📤 将 Dashboard 分享给团队
5. 🔄 定期优化查询性能
