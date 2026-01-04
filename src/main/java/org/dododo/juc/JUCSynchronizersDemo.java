package org.dododo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class JUCSynchronizersDemo {
    public static final int parties = 3;

    public JUCSynchronizersDemo() {
    }

    public static void testCyclicBarrier() {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(parties, () -> {
           log.info("线程全部就绪，开干！");
        });

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            Runnable runnable = () -> {
                log.info("线程 {} 已就绪", finalI + 1);
                try {
                    Thread.sleep(2000 );
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    log.error("Catch by InterruptedException: {}", e.getLocalizedMessage());
                } catch (BrokenBarrierException e) {
                    log.error("Catch by BrokenBarrierException: {}", e.getLocalizedMessage());
                }
            };
            executorService.submit(runnable);
        }
    }

    public static void testCountDownLatcher() {
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0 ; i < 3; i++) {
            int finalI = i;
            Runnable runnable = () -> {
                try {
                    Thread.sleep(2000 );
                    log.info("线程 {} 正在执行....", finalI + 1);
                    latch.countDown();
                } catch (InterruptedException e) {
                    log.error("Catch by InterruptedException: {}", e.getLocalizedMessage());
                }
            };
            executorService.submit(runnable);
        }
        log.info("主线程等待中.....");
        try {
            latch.await();
            log.info("主线程等待成功!!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testSemaphore() {
        Semaphore semaphore = new Semaphore(3);
        try {
            semaphore.acquire();
            log.info("信号量已捕捉，执行逻辑代码");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    public static void main(String []args) {
        log.info("测试同步器");
        testCountDownLatcher();
    }
}
