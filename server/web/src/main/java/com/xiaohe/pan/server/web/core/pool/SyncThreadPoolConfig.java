package com.xiaohe.pan.server.web.core.pool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
public class SyncThreadPoolConfig {
    /**
     * 心跳线程池（处理高频短任务）
     * @return
     */
    @Bean(name = "heartbeatPool")
    public ThreadPoolTaskExecutor heartbeatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("heartbeat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 数据同步线程池（处理IO密集型任务）
     * @return
     */
    @Bean(name = "syncPool")
    public ThreadPoolTaskExecutor syncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}