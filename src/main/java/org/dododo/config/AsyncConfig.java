package org.dododo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("myExecutor")
    public Executor myExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(10);  // 核心线程数
        t.setMaxPoolSize(30);   // 最大线程数
        t.setKeepAliveSeconds(10);
        t.setQueueCapacity(20);
        t.setThreadNamePrefix("Darren-executors-");
        // 拒绝策略：由调用者所在的线程执行
        t.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        t.initialize();
        return t;
    }
}
