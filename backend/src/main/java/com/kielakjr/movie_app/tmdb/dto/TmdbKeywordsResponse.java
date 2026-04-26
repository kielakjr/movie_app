package com.kielakjr.movie_app.tmdb.dto;

public record TmdbKeywordsResponse(
    long id,
    TmdbKeyword[] keywords
) {
    public record TmdbKeyword(
        long id,
        String name
    ) {}
}
