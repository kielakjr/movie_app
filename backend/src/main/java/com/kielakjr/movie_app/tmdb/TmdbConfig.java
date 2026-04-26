package com.kielakjr.movie_app.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TmdbConfig {

    private final TmdbRateLimitInterceptor tmdbRateLimitInterceptor;

    @Bean
    RestClient tmdbRestClient(
            @Value("${tmdb.api-key}") String apiKey,
            @Value("${tmdb.base-url}") String baseUrl) {
        if (apiKey.isBlank()) throw new IllegalStateException("tmdb.api-key must be set");
        return RestClient.builder()
                .requestInterceptor(tmdbRateLimitInterceptor)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
