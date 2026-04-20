package com.kielakjr.movie_app.tmdb.dto;

import java.util.List;

public record TmdbGenreListResponse(List<TmdbGenre> genres) {
    public record TmdbGenre(Integer id, String name) {}
}
