package com.bgssai.shortai.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 全局异步线程池配置（Standards §4：严禁在业务代码里自行 new Thread / Executors，所有线程池在这里
 * 统筹声明并命名）。本产品新增异步线程池时一律加在这个类里，不要另起一处。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 执行 AI 生成任务（出图 / 出视频 / 配音 / 合成）。
     * <p>
     * 没有它 {@code ShortAiTaskWorker.tick()} 会把 provider 调用同步跑在调度线程上：上游一慢，
     * 整条队列就跟着停摆；而且线程被占住时连「这条任务超时了」都没人去判。
     * <p>
     * 队列刻意留得浅：任务本来就落在 short_ai_task 表里，排队归数据库管，内存队列只是取任务的
     * 缓冲。满了就让 tick() 把任务退回 QUEUED，下一轮再取，不要在内存里堆积。
     * 关停时不等待卡住的上游调用，未完成的任务由 reapStaleTasks() 判超时。
     */
    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(8);
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("bgssai-ai-task-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    /**
     * 执行剪辑工程导出的打包（剪映草稿 / FCPXML / EDL）。
     * <p>
     * 与 aiTaskExecutor 分开是刻意的：导出要把一集几百 MB 素材从对象存储逐个拉回来再压缩，
     * 单个任务能占住线程几十秒。混在生成任务池里，一次整剧导出就能把出图出视频的队列堵死，
     * 而这两件事的紧要程度完全不同——生成在花钱等结果，导出只是换个封装拿走。
     * <p>
     * 池子做得小：导出是低频动作，且每个任务都吃内存（整包在内存里压），并发高了先撑爆的是堆。
     * 队列满时直接抛给调用方，由 export 接口报错让用户稍后重试，好过在内存里排一长队。
     */
    @Bean("editExportExecutor")
    public ThreadPoolTaskExecutor editExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(6);
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("bgssai-edit-export-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    /**
     * 执行健康组件检查，使每次检查都能被赋予一个硬截止时间（Standards §13.5）。
     * <p>
     * 没有它探针可能永久挂住：Druid 的 {@code maxWait} 默认「无限等待」，数据库不可达时
     * {@code getConnection()} 永不返回，请求线程就此长驻。反复探测会一条条泄漏 Tomcat 线程，
     * 直到连存活端点都不再应答——健康检查自己成了故障。
     * <p>
     * 刻意做得很小：每个请求逐个提交检查，且 HealthProbeRunner 的 TTL 缓存已把批次频率压得很低。
     * 允许核心线程超时回收，空闲进程就不会常驻健康线程；关停时也不等待卡住的探针。
     */
    @Bean("healthProbeExecutor")
    public ThreadPoolTaskExecutor healthProbeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("bgssai-health-probe-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
