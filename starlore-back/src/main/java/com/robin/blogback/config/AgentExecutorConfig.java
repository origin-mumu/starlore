package com.robin.blogback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AgentExecutorConfig {

    @Bean(name = "multiAgentExecutor", destroyMethod = "shutdown")
    public ExecutorService multiAgentExecutor(
            @Value("${app.agent.executor.core-size:4}") int coreSize,
            @Value("${app.agent.executor.max-size:8}") int maxSize,
            @Value("${app.agent.executor.queue-capacity:64}") int queueCapacity) {
        AtomicInteger threadNumber = new AtomicInteger();
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable,
                            "multi-agent-stream-" + threadNumber.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }
}
