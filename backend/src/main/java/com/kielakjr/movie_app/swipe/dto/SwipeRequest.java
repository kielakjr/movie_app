package com.kielakjr.movie_app.swipe.dto;

import jakarta.validation.constraints.NotNull;

public record SwipeRequest(
    @NotNull(message = "Movie ID cannot be null")
    Long movieId,
    @NotNull(message = "Action cannot be null")
    SwipeAction action
) {

}
