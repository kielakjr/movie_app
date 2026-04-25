package com.kielakjr.movie_app.recommend.dto;

import com.kielakjr.movie_app.movie.dto.MovieResponse;

public record RecommendMovieResponse(
    MovieResponse movie,
    MovieResponse reason
) {
}
