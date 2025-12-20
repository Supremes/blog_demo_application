package org.dododo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class StandardLockDemo {
    // 1. 模拟一个数据库连接对象
    static class MockConnection {
        private final String name;
        public MockConnection(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    // 2. 连接池类
    static class MiniPool {
        private final LinkedList<MockConnection> pool = new LinkedList<>();
        private final int maxParams;

        // 【核心组件 1】ReentrantLock：AQS 的具体实现，负责守门
        private final Lock lock = new ReentrantLock();

        // 【核心组件 2】Condition：AQS 的等待队列机制
        private final Condition notEmpty = lock.newCondition(); // 池子非空
        private final Condition notFull = lock.newCondition();  // 池子未满

        // 【核心组件 3】AtomicInteger：CAS 的具体应用
        // 用来统计指标，不需要加重型锁，性能更高
        private final AtomicInteger requestCount = new AtomicInteger(0);

        public MiniPool(int size) {
            this.maxParams = size;
            for (int i = 0; i < size; i++) {
                pool.add(new MockConnection("Conn-" + i));
            }
        }

        // 获取连接
        public MockConnection borrowConnection(String threadName) throws InterruptedException {
            // CAS 操作：记录请求数（无锁，高性能）
            requestCount.incrementAndGet();

            // AQS 操作：尝试获取锁（如果有人在操作池子，这里会阻塞或自旋）
            lock.lock();
            try {
                // 如果池子空了，必须等待
                while (pool.isEmpty()) {
                    log.info("{} 发现池子空了 -> 进入等待队列 (Condition.await)", threadName);
                    // 【核心点】释放锁，当前线程挂起（Park），进入 Condition 等待队列
                    notEmpty.await();
                }

                // 拿走第一个连接
                MockConnection conn = pool.removeFirst();
                log.info("{} 拿到连接: {} (当前剩余: {})", threadName, conn, pool.size());

                // 拿走连接后，池子不满了，唤醒等待归还的线程
                notFull.signal();
                return conn;
            } finally {
                lock.unlock(); // 必须在 finally 中释放锁
            }
        }

        // 归还连接
        public void returnConnection(MockConnection conn, String threadName) {
           lock.lock();
           try {
               while (pool.size() >= maxParams) {
                   log.warn("{} 发现池子满了 -> 等待空位", threadName);
                   notFull.await();
               }
               pool.addLast(conn);
               notEmpty.signal();
           } catch (Exception e) {

           } finally {
               lock.unlock();
           }
        }

        public MockConnection borrowConnectionWithSynchronized(String threadName) {
            requestCount.incrementAndGet();
            synchronized (pool) {
               while (pool.isEmpty()) {
                   try {
                       log.info("{} 发现池子空了 -> 进入等待队列 (Condition.await)", threadName);
                       pool.wait();

                   } catch (InterruptedException e) {
                       log.warn("met exception : {}", e.getLocalizedMessage());
                       return null;
                   }
               }
                MockConnection connection = pool.removeFirst();
                log.info("{} 拿到连接: {} (当前剩余: {})", threadName, connection, pool.size());

                pool.notifyAll();  // ✅ 用 notifyAll 唤醒所有等待线程
                return connection;
            }
        }

        public void returnConnectionWithSynchronized(MockConnection connection) {
            synchronized (pool) {
                while (pool.size() >= maxParams) {
                    try {
                        log.warn("发现池子满了 -> 等待空位");
                        pool.wait();
                    } catch (InterruptedException e) {
                        log.warn("met exception : {}", e.getLocalizedMessage());
                        return;
                    }
                }
                pool.addLast(connection);
                log.info("归还连接: {} (当前剩余: {})", connection, pool.size());

                pool.notifyAll(); // ✅ 用 notifyAll 唤醒所有等待线程
            }
        }

        public int getTotalRequests() {
            return requestCount.get();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 初始化只有 3 个连接的池子
        MiniPool dbPool = new MiniPool(3);

        // 创建一个线程池，模拟 10 个并发用户
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= 10; i++) {
            String threadName = "线程-" + i;
            executor.submit(() -> {
                try {
                    // 1. 抢连接
                    MockConnection conn = dbPool.borrowConnection(threadName);

                    // 2. 模拟业务耗时 (1秒)
                    Thread.sleep(1000);

                    // 3. 还连接
                    dbPool.returnConnection(conn, threadName);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        log.debug("线程池关闭结果: {}", executor.awaitTermination(10, TimeUnit.SECONDS));
        log.info("--------------------------------");
        log.info("总请求次数 (CAS统计): {}", dbPool.getTotalRequests());
    }
}
