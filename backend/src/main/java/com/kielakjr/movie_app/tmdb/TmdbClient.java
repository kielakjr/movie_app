package com.kielakjr.movie_app.tmdb;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.kielakjr.movie_app.tmdb.dto.*;

@Component
@RequiredArgsConstructor
public class TmdbClient {
    private final RestClient tmdbRestClient;

    public TmdbGenreListResponse getGenres() {
        return tmdbRestClient.get()
                .uri("/genre/movie/list")
                .retrieve()
                .body(TmdbGenreListResponse.class);
    }

    public TmdbPopularResponse getPopularMovies(int page) {
        if (page < 1 || page > 500)
            throw new IllegalArgumentException("page must be between 1 and 500");
        return tmdbRestClient.get()
                .uri(b -> b.path("/movie/popular").queryParam("page", page).build())
                .retrieve()
                .body(TmdbPopularResponse.class);
    }
}
