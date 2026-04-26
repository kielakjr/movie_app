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

    public TmdbMovieResponse getPopularMovies(int page) {
        if (page < 1 || page > 500)
            throw new IllegalArgumentException("page must be between 1 and 500");
        return tmdbRestClient.get()
                .uri(b -> b.path("/movie/popular").queryParam("page", page).build())
                .retrieve()
                .body(TmdbMovieResponse.class);
    }

    public TmdbMovieResponse getTopRatedMovies(int page) {
        if (page < 1 || page > 500)
            throw new IllegalArgumentException("page must be between 1 and 500");
        return tmdbRestClient.get()
                .uri(b -> b.path("/movie/top_rated").queryParam("page", page).build())
                .retrieve()
                .body(TmdbMovieResponse.class);
    }

    public TmdbKeywordsResponse getKeywords(long movieId) {
        var response = tmdbRestClient.get()
                .uri("/movie/{movieId}/keywords", movieId)
                .retrieve()
                .body(TmdbKeywordsResponse.class);
        return response;
    }
}
