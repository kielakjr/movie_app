package com.kielakjr.movie_app.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class ContainersConfig {

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("test_db")
                    .withUsername("test")
                    .withPassword("test");

    public static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);
}
