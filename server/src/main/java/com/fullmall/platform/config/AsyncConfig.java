package com.fullmall.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 告警派发线程池。
 *
 * <p>为什么不能同步发：告警是网络调用（群机器人/短信），放在业务请求线程上会把
 * 一次支付回调拖慢十几秒，还可能因为下游超时把业务事务一起拖垮（AGENTS §10.3）。
 * 队列满时用 CallerRuns 兜底会让请求线程变慢，所以队列给小、拒绝时打日志由调用方记录——
 * 这里显式指定 AbortPolicy，让「告警没发出去」变成可见的失败而不是静默丢弃。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("alertExecutor")
    public Executor alertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("alert-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
