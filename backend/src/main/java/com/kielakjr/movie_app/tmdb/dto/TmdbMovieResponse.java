package com.kielakjr.movie_app.tmdb.dto;

import java.util.List;

public record TmdbMovieResponse(Integer page, List<TmdbMovie> results) {
    public record TmdbMovie(
            Boolean adult,
            String backdrop_path,
            List<Integer> genre_ids,
            Long id,
            String original_language,
            String overview,
            Double popularity,
            String poster_path,
            String release_date,
            String title,
            Double vote_average,
            Integer vote_count
    ) {}
}
