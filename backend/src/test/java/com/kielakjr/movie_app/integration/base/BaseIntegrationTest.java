package com.kielakjr.movie_app.integration.base;

import com.kielakjr.movie_app.integration.config.ContainersConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static {
        ContainersConfig.POSTGRES.start();
        ContainersConfig.REDIS.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ContainersConfig.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ContainersConfig.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ContainersConfig.POSTGRES::getPassword);

        registry.add("spring.data.redis.host", ContainersConfig.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> ContainersConfig.REDIS.getMappedPort(6379));
    }
}
