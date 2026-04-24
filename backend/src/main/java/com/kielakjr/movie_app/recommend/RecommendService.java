package com.kielakjr.movie_app.recommend;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendService {
    private final MovieService movieService;
    private final SessionService sessionService;

    public List<MovieResponse> getRecommendedMovies(HttpSession session, int limit) {
        var state = sessionService.getState(session);
        if (state.getUserEmbedding() == null) {
            throw new IllegalStateException("User embedding not set");
        }

        return movieService.findSimilar(state.getUserEmbedding(), limit, state.getSeenMovieIds());
    }
}
