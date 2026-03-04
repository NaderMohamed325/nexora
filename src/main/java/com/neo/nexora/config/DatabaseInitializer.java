package com.neo.nexora.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        entityManager.createNativeQuery("CREATE EXTENSION IF NOT EXISTS pg_trgm").executeUpdate();
        entityManager.createNativeQuery(
                "CREATE INDEX IF NOT EXISTS idx_users_name_trgm " +
                        "ON users USING GIN (username gin_trgm_ops)"
        ).executeUpdate();
    }

}
