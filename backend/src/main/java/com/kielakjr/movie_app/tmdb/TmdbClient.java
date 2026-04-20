package com.kielakjr.movie_app.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.kielakjr.movie_app.tmdb.dto.*;

import jakarta.annotation.PostConstruct;

@Component
public class TmdbClient {
    private RestClient restClient;

    @Value("${tmdb.base-url}")
    private String TMDB_BASE_URL;

    @Value("${tmdb.api-key}")
    private String tmdbToken;

    @PostConstruct
    public void init() {
        if (tmdbToken == null || tmdbToken.isBlank()) {
            throw new IllegalStateException("TMDB API token must be provided");
        }
        this.restClient = RestClient.builder()
                .baseUrl(TMDB_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + tmdbToken)
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
