package com.kielakjr.movie_app.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.kielakjr.movie_app.tmdb.dto.*;

@Component
public class TmdbClient {
    private final RestClient restClient;

    public TmdbClient(
        RestClient.Builder builder,
        @Value("${tmdb.api-key}") String apiKey,
        @Value("${tmdb.base-url}") String baseUrl
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("TMDB API key must be provided");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("TMDB base URL must be provided");
        }
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public TmdbGenreListResponse getGenres() {
        return restClient.get()
                .uri("/genre/movie/list")
                .retrieve()
                .body(TmdbGenreListResponse.class);
    }

    public TmdbPopularResponse getPopularMovies(int page) {
        if (page < 1) throw new IllegalArgumentException("Page number must be >= 1");
        if (page > 500) throw new IllegalArgumentException("Page number must be <= 500");
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbPopularResponse.class);
    }
}
