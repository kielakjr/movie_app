package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;

@Service
@RequiredArgsConstructor
public class SwipeService {
    private final SessionService sessionService;

    public void swipe(SwipeRequest request, HttpSession httpSession) {
        var state = sessionService.getState(httpSession);

        switch (request.action()) {
            case LIKE -> {
                state.getSeenMovieIds().add(request.movieId());
                state.setLikesCount(state.getLikesCount() + 1);
            }
            case DISLIKE -> state.getDislikedMovieIds().add(request.movieId());
            case SKIP -> state.getSeenMovieIds().add(request.movieId());
        }
    }
}
