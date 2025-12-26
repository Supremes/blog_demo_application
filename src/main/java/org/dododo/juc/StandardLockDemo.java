package org.dododo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JUC 标准锁示例：用“连接池”场景串起 ReentrantLock / Condition / AQS 等核心概念。
 *
 * <p><b>1) ReentrantLock 与 AQS</b>
 * <ul>
 *   <li>{@link ReentrantLock} 的核心是 {@code AbstractQueuedSynchronizer(AQS)}。</li>
 *   <li>AQS 内部用一个 {@code state}（volatile int）表示同步状态；加锁/解锁围绕 CAS + 队列协作完成。</li>
 *   <li>当线程获取锁失败，会被封装成一个 {@code Node} 进入 AQS 的<strong>同步队列</strong>（FIFO，CLH 变体的双向链表），
 *       随后通过 {@code LockSupport.park()} 挂起，等待前驱唤醒。</li>
 *   <li>释放锁时，AQS 会尝试唤醒同步队列中合适的后继节点（{@code LockSupport.unpark()}），实现“阻塞式”竞争。</li>
 * </ul>
 *
 * <p><b>2) Condition：条件队列（等待队列）</b>
 * <ul>
 *   <li>{@code lock.newCondition()} 会创建与该 Lock 绑定的 {@code ConditionObject}。</li>
 *   <li>{@link Condition#await()} 的关键语义：
 *       <ol>
 *         <li>当前线程必须持有锁；</li>
 *         <li>将当前线程节点加入该 Condition 的<strong>条件队列</strong>；</li>
 *         <li>完全释放锁（使其他线程可进入临界区）；</li>
 *         <li>线程 park 挂起；</li>
 *         <li>被 {@link Condition#signal()} / {@link Condition#signalAll()} 转移到 AQS 同步队列后，
 *             还需要在同步队列中重新竞争锁，成功后 await 才返回。</li>
 *       </ol>
 *   </li>
 *   <li>{@link Condition#signal()} 的本质：把条件队列头节点转移到 AQS 同步队列，等待后续“重新抢锁”。</li>
 * </ul>
 *
 * <p><b>3) synchronized + wait/notify 对照</b>
 * <ul>
 *   <li>{@code synchronized} 基于对象监视器（Monitor）。</li>
 *   <li>{@link Object#wait()} 会释放监视器并进入该对象的 Wait Set（可视为“条件队列”）。</li>
 *   <li>{@link Object#notify}/{@link Object#notifyAll} 会把等待线程从 Wait Set 移到 Entry List，
 *       之后仍需重新竞争监视器才能继续执行。</li>
 *   <li>Lock/Condition 的优势之一：一个 Lock 可创建多个 Condition（多条件队列），表达力强于单一 Wait Set。</li>
 * </ul>
 *
 * <p><b>4) AtomicInteger：CAS/无锁统计</b>
 * <ul>
 *   <li>{@link AtomicInteger#incrementAndGet()} 基于 CAS（底层 Unsafe/VarHandle）实现无锁原子更新，
 *       适合这类“计数器/指标”场景，避免使用重型互斥影响吞吐。</li>
 * </ul>
 */
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

        // 【核心组件 1】ReentrantLock：AQS 的具体实现，临界区“守门人”
        // - 竞争失败的线程进入 AQS 同步队列（FIFO），park 挂起等待
        // - unlock 时 unpark 后继节点，让其继续在队列上竞争
        private final Lock lock = new ReentrantLock();

        // 【核心组件 2】Condition：与 Lock 绑定的“条件队列”（注意：不是 AQS 同步队列）
        // - notEmpty：等待“池子变为非空”
        // - notFull：等待“池子出现空位”
        private final Condition notEmpty = lock.newCondition();
        private final Condition notFull = lock.newCondition();

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

            // AQS 操作：尝试获取锁（失败则入同步队列并 park 挂起，等待被唤醒后继续竞争）
            lock.lock();
            try {
                // 如果池子空了，必须等待
                while (pool.isEmpty()) {
                    log.info("{} 发现池子空了 -> 进入等待队列 (Condition.await)", threadName);
                    // 【核心点】await 做了三件事：
                    // 1) 当前线程加入 notEmpty 条件队列
                    // 2) 完全释放 lock（让其他线程能归还连接/改变条件）
                    // 3) park 挂起；被 signal 转移到 AQS 同步队列后，还要重新抢到 lock 才能返回
                    notEmpty.await();
                }

                // 拿走第一个连接
                MockConnection conn = pool.removeFirst();
                log.info("{} 拿到连接: {} (当前剩余: {})", threadName, conn, pool.size());

                // 拿走连接后，池子不满了，唤醒等待归还的线程
                // signal 的语义：把 notFull 条件队列的一个等待者转移到 AQS 同步队列
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
                   // await：释放 lock 并进入 notFull 条件队列，直到被 signal 后再回到同步队列抢锁
                   notFull.await();
               }
               pool.addLast(conn);
               // 归还后池子“非空”，唤醒一个等待取连接的线程
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
                       // 对照组：synchronized + Object.wait
                       // - wait 会释放监视器并进入该对象的 Wait Set
                       // - notify/notifyAll 只是“移动到可竞争队列”，线程仍需重新抢到监视器
                       log.info("{} 发现池子空了 -> 进入等待队列 (Object.wait)", threadName);
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
