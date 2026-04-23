package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;

@Service
@RequiredArgsConstructor
public class SwipeService {
    private final SessionService sessionService;
    private final MovieService movieService;

    public void swipe(SwipeRequest request, HttpSession httpSession) {
        var state = sessionService.getState(httpSession);

        switch (request.action()) {
            case LIKE -> {
                state.getSeenMovieIds().add(request.movieId());
                state.getLikedMovieIds().add(request.movieId());
                state.setLikesCount(state.getLikesCount() + 1);
            }
            case DISLIKE -> {
                state.getSeenMovieIds().add(request.movieId());
                state.getDislikedMovieIds().add(request.movieId());
            }
            case SKIP -> state.getSeenMovieIds().add(request.movieId());
        }
    }

    public MovieResponse getNextFeed(HttpSession httpSession) {
        var state = sessionService.getState(httpSession);
        var seenIds = state.getSeenMovieIds();
        var movie = movieService.getUnseenMovie(seenIds);
        if (movie.isEmpty()) {
            throw new RuntimeException("No more movies available");
        }
        return movie.get();
    }
}
