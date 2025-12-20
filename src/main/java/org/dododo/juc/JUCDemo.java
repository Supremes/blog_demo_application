package org.dododo.juc;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JUCDemo {
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
}
