package com.neo.nexora.scheduler;


import com.neo.nexora.entity.User;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountDeletionScheduler {
    private final UserRepository userRepository;
    private final UserService userService;



    @Async("schedulerTaskExecutor")
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredAccounts() {
        log.info("Running account deletion job...");

        List<User> users = userRepository.findUsersScheduledForDeletion(LocalDateTime.now());

        if (users.isEmpty()) {
            log.info("No accounts to delete.");
            return;
        }

        int successfulDeletions = 0;

        for (User user : users) {
            try {
                userService.deleteUserById(user.getId());
                log.info("Deleted user ID: {}", user.getId());
                successfulDeletions++;
            } catch (Exception e) {
                log.error("Failed to delete user ID: {}", user.getId(), e);
            }
        }

        log.info("Deletion job complete. Deleted {} accounts.", successfulDeletions);
    }


    @Scheduled(cron = "0 0 9 * * *") // every day at 9am
    public void sendDeletionReminders() {
        LocalDateTime sevenDaysFromNow = LocalDateTime.now().plusDays(7);
        //TODO applying mailing service
    }
}
