package org.dododo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

// 0. 准备一个简单的商品类
class Product {
    String name;
    double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }
}

public class StreamDemo {
    public static void main(String[] args) {

        // ==========================================
        // 1. Supplier (供给者): 无中生有，提供数据源
        // ==========================================
        // 模拟从数据库加载数据
        Supplier<List<Product>> productLoader = () -> {
            List<Product> list = new ArrayList<>();
            list.add(new Product("iPhone 15", 6000));
            list.add(new Product("小米手环", 200));
            list.add(new Product("MacBook Pro", 15000));
            list.add(new Product("洗发水", 50));
            return list;
        };

        // ==========================================
        // 2. Predicate (断言): 非黑即白，用于筛选
        // ==========================================
        // 逻辑：价格是否大于 5000
        Predicate<Product> isExpensive = product -> product.price > 5000;

        // ==========================================
        // 3. Function (函数): 有去有回，用于转换
        // ==========================================
        // 逻辑：输入 Product 对象，输出一个 String (打折后的广告语)
        Function<Product, String> makeAd = product -> {
            double discountPrice = product.price * 0.9; // 打9折
            return "【特惠】" + product.name + " 现价: " + discountPrice;
        };

        // ==========================================
        // 4. Consumer (消费者): 只吃不吐，用于最终操作
        // ==========================================
        // 逻辑：接收字符串并打印 (模拟发送推送)
        Consumer<String> sendPushMsg = msg -> System.out.println("推送发送成功 -> " + msg);


        // ==========================================
        // 核心：在 Stream 流中串联它们
        // ==========================================
        System.out.println("--- 开始处理订单流 ---");

        List<Product> productList = productLoader.get(); // 1. 获取数据 (Supplier)

        productList.stream()
                .filter(isExpensive)  // 2. 筛选 (Predicate) -> 只有 iPhone 和 MacBook 留下来
                .map(makeAd)          // 3. 转换 (Function)  -> 变成 String
                .forEach(sendPushMsg);// 4. 消费 (Consumer)  -> 打印结果

        System.out.println("--- 处理结束 ---");
    }
}