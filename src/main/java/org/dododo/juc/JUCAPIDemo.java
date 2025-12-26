package org.dododo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class JUCAPIDemo {
    private final ReentrantLock reentrantLock = new ReentrantLock();

    private void testReentrantLock() {
        try {
            if (reentrantLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                try {
                    // 执行逻辑

                } finally {
                    reentrantLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void testReentrantReadWriteLock() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    }


    private void testRunnable() throws ExecutionException, InterruptedException {
        Runnable runnable = () -> System.out.println("Execute non return value function");
        Callable<Integer> callable = () -> {
            return 1 + 1;
        };

        ExecutorService executor = Executors.newFixedThreadPool(10);
        Future<?> future = executor.submit(runnable);
        future.get();

        FutureTask<Integer> futureTask = new FutureTask<>(callable);

        Thread thread = new Thread(futureTask);
        thread.start();
        futureTask.get();
    }

    public enum PoolType {
        Fixed("fixed", "call API - Executors.newFixedThreadPool" );

        private final String name;
        private final String description;

        PoolType(String name,String description) {
            this.name = name;
            this.description = description;
        }
    }

    static class ThreadPoolDemo {
        public static void testWorkStealPool() throws InterruptedException {
            // 1. 创建 WorkStealingPool
            // 默认并行度 = CPU 核心数
            ExecutorService executor = Executors.newWorkStealingPool();

            // 2. 提交 Runnable 或 Callable 任务 (像用普通线程池一样)
            for (int i = 0; i < 10; i++) {
                final int index = i;
                executor.submit(() -> {
                    System.out.println(Thread.currentThread().getName() + " 正在处理任务 " + index);
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                });
            }

            // 3. 注意！这里有一个大坑（守护线程）
            // newWorkStealingPool 创建的线程默认是 Daemon（守护）线程。
            // 如果主线程结束了，线程池里的任务会立马中断，不会像 FixedThreadPool 那样等待任务跑完。

            // 为了演示，让主线程睡一会儿，否则你看不到输出程序就退出了
//            Thread.sleep(5000);
        }

        public static void testForkJoinPool() {
            ForkJoinPool forkJoinPool = new ForkJoinPool();

            long startTime = System.currentTimeMillis();
            long res = forkJoinPool.invoke(new SumTask(0, 10000000L));
            long endTime = System.currentTimeMillis();
            log.info("计算结果: {}", res );
            log.info("耗时: {} ms", endTime - startTime );
        }

        public static class SumTask extends RecursiveTask<Long> {

            // 阈值：每个任务最多计算多少个数（决定了拆分的粒度）
            // 只要任务量大于这个值，就继续拆分
            private static final long THRESHOLD = 10000;

            private final long start;
            private final long end;

            public SumTask(long start, long end) {
                this.start = start;
                this.end = end;
            }

            @Override
            protected Long compute() {
                long length = end - start;

                // 1. 基准情况：任务足够小，直接计算，不再拆分
                if (length <= THRESHOLD) {
                    long sum = 0;
                    for (long i = start; i <= end; i++) {
                        sum += i;
                    }
                    return sum;
                }

                // 2. 递归情况：任务太大，一分为二
                else {
                    long middle = (start + end) / 2;

                    SumTask leftTask = new SumTask(start, middle);
                    SumTask rightTask = new SumTask(middle + 1, end);

                    // 执行子任务（Fork）
                    // 注意：通常的做法是 fork() 一个，当前线程 compute() 另一个，以减少线程开销
                    // 或者两个都 fork，然后 join
                    leftTask.fork();  // 异步执行 leftTask
                    rightTask.fork(); // 异步执行 rightTask

                    // 等待结果并合并（Join）
                    return leftTask.join() + rightTask.join();
                }
            }
        }

        public static void main(String []args) throws InterruptedException {
            // ThreadPoolDemo.testWorkStealPool();
            ThreadPoolDemo.testForkJoinPool();
        }
    }
}
