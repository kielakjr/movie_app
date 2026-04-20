package com.kielakjr.movie_app.movie.dto;

import lombok.Builder;

@Builder
public record MovieResponse(
    Long id,
    Long tmdbId,
    String title,
    String overview,
    String releaseDate,
    String originalLanguage,
    boolean adult,
    String posterPath,
    String backdropPath,
    String[] genres,
    Double popularity,
    Double voteAverage,
    Integer voteCount
) {
}
