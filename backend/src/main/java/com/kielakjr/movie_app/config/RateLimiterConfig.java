package com.kielakjr.movie_app.config;

import org.springframework.context.annotation.Configuration;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public Bucket tmdbBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(20)
                        .refillGreedy(20, Duration.ofSeconds(1))
                )
                .build();
    }
}
