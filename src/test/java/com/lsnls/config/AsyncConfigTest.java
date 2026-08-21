package com.lsnls.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    @Test
    void creaLosCuatroExecutors() {
        AsyncConfig config = new AsyncConfig();
        Executor audit = config.auditTaskExecutor();
        Executor validation = config.validationTaskExecutor();
        Executor export = config.exportTaskExecutor();
        Executor critical = config.criticalTaskExecutor();

        assertTrue(audit instanceof ThreadPoolTaskExecutor);
        assertTrue(validation instanceof ThreadPoolTaskExecutor);
        assertTrue(export instanceof ThreadPoolTaskExecutor);
        assertTrue(critical instanceof ThreadPoolTaskExecutor);
        assertNotNull(((ThreadPoolTaskExecutor) audit).getThreadNamePrefix());
        ((ThreadPoolTaskExecutor) audit).shutdown();
        ((ThreadPoolTaskExecutor) validation).shutdown();
        ((ThreadPoolTaskExecutor) export).shutdown();
        ((ThreadPoolTaskExecutor) critical).shutdown();
    }
}
