package com.neo.nexora.config;

import com.neo.nexora.service.queue.user.UserDeletionProducerImpl;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.sql.DataSource;

/**
 * Scheduler configuration that enables Spring's {@code @Scheduled} task execution
 * and distributed locking via <a href="https://github.com/lukas-krecan/ShedLock">ShedLock</a>.
 *
 * <h2>Why this exists</h2>
 * <p>The user deletion pipeline relies on a scheduled job
 * ({@link UserDeletionProducerImpl#scheduleUserDeletion()})
 * that runs daily at 2:00 AM. This config class:</p>
 * <ol>
 *   <li><b>Enables scheduling</b> — {@code @EnableScheduling} activates Spring's task scheduler
 *       so that {@code @Scheduled} annotations are detected and executed.</li>
 *   <li><b>Provides a thread pool</b> — a pool of 4 threads ensures that multiple scheduled
 *       tasks can run concurrently without blocking each other.</li>
 *   <li><b>Prevents duplicate execution</b> — {@code @EnableSchedulerLock} + ShedLock ensures
 *       that in a multi-instance deployment (e.g., 3 replicas behind a load balancer),
 *       only <b>one instance</b> runs the deletion job at a time. The lock is held for at most 10 minutes.</li>
 * </ol>
 *
 * <h2>Graceful shutdown</h2>
 * <p>{@code waitForTasksToCompleteOnShutdown = true} and {@code awaitTerminationSeconds = 30}
 * ensure that if the application is shut down while a scheduled task is running, it waits
 * up to 30 seconds for the task to finish before forcefully terminating.</p>
 *
 * @see UserDeletionProducerImpl  The scheduled job this config enables
 * @see com.neo.nexora.config.RabbitMQConfig                            Queue infrastructure the job publishes to
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig implements SchedulingConfigurer {

    /**
     * Configures the thread pool used to run {@code @Scheduled} methods.
     *
     * <ul>
     *   <li><b>Pool size = 4</b> — up to 4 scheduled tasks can execute concurrently.</li>
     *   <li><b>Thread prefix = "scheduler-thread-"</b> — makes threads identifiable in logs and thread dumps.</li>
     *   <li><b>Wait for shutdown = true</b> — running tasks are allowed to complete before the app exits.</li>
     *   <li><b>Await termination = 30s</b> — maximum wait time during shutdown; after 30s, threads are interrupted.</li>
     * </ul>
     *
     * @param taskRegistrar the registrar provided by Spring to register the custom task scheduler
     */
    @Override
    public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.setThreadNamePrefix("scheduler-thread-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(30);
        taskScheduler.initialize();

        taskRegistrar.setTaskScheduler(taskScheduler);
    }


    /**
     * Creates the ShedLock {@link LockProvider} backed by a JDBC table.
     *
     * <p>ShedLock stores lock records in a database table (typically {@code shedlock}).
     * When a scheduled task starts, ShedLock inserts/updates a row with the lock name
     * and expiry time. Other instances see the row and skip execution.</p>
     *
     * <ul>
     *   <li>{@code usingDbTime()} — uses the database server's clock for lock expiry,
     *       avoiding issues with clock drift between application instances.</li>
     * </ul>
     *
     * @param dataSource the application's JDBC DataSource (auto-configured by Spring Boot)
     * @return a JDBC-based {@link LockProvider} for ShedLock
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
