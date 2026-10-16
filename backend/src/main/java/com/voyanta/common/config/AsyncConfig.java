package com.voyanta.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution for long-running background work.
 *
 * Niyə lazımdır: plan generasiyası AI provider-ə bloklayan HTTP sorğusu edir.
 * Əgər o, sorğu thread-ində işləsəydi, provider yavaş/limitli olanda
 * "plan hazırlanır" sorğuları Tomcat worker thread-lərini tuturdu və
 * eyni JVM-dəki bütün endpoint-lər — o cümlədən POST /api/auth/oauth/google —
 * cavab verə bilmirdi. Yəni AI xidməsinin mövcudluğu autentifikasiyanı da
 * dayandırırdı. Worker indi ayrı, məhdud həcmli executor-da işləyir.
 *
 * Executor "məhdud" (bounded) seçilir ki, provider uçsaq və ya tələb yığılsa,
 * təsəvvüri yaddaş və thread sayı sonsuz artmasın. Kifayət qədər olmayanda
 * tapşırıq rədd edilir və plan FAILED işarələnir (bax PlanGenerationService).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** Adı kodda istifadə olunur — @Async bu adla həmin executor-u seçir. */
    public static final String PLAN_GENERATION_EXECUTOR = "planGenerationExecutor";

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 100;

    @Bean(name = PLAN_GENERATION_EXECUTOR)
    public Executor planGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("plan-gen-");
        // Dayanma zamanı başlayan işləri bitirməyə çalış — yarım qalan plan azadır.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
