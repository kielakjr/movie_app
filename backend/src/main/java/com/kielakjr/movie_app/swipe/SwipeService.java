package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.ClusterService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SwipeService {
    private final SessionService sessionService;
    private final MovieService movieService;
    private final ClusterService clusterService;
    private static final float EXPLORATION_RATE = 0.2f;
    private static final float DISLIKE_EXPLOITATION_RATE = 0.15f;

    public Optional<MovieResponse> swipe(SwipeRequest request, HttpSession httpSession) {
        var state = sessionService.getState(httpSession);

        switch (request.action()) {
            case LIKE -> {
                state.getSeenMovieIds().add(request.movieId());
                state.getLikedMovieIds().add(request.movieId());
                state.setLikesCount(state.getLikesCount() + 1);
                var movieEmbedding = movieService.getEmbeddingById(request.movieId());
                if (movieEmbedding != null) {
                    clusterService.addToClusters(movieEmbedding, state.getClusters());
                }
            }
            case DISLIKE -> {
                state.getSeenMovieIds().add(request.movieId());
                state.getDislikedMovieIds().add(request.movieId());
                var movieEmbedding = movieService.getEmbeddingById(request.movieId());
                if (movieEmbedding != null) {
                    clusterService.addToClusters(movieEmbedding, state.getDislikedClusters());
                }
            }
            case SKIP -> state.getSeenMovieIds().add(request.movieId());
        }

        var excludeIds = new HashSet<>(state.getSeenMovieIds());
        return getNextMovie(httpSession, excludeIds);
    }

    public Optional<MovieResponse> getNextFeed(HttpSession httpSession) {
        var state = sessionService.getState(httpSession);
        var excludeIds = new HashSet<>(state.getSeenMovieIds());
        return getNextMovie(httpSession, excludeIds);
    }

    private Optional<MovieResponse> getNextMovie(HttpSession httpSession, Set<Long> excludeIds) {
        var state = sessionService.getState(httpSession);
        var rand = nextRandom();
        if (state.getClusters().isEmpty() || rand < EXPLORATION_RATE) {
            if (rand < DISLIKE_EXPLOITATION_RATE && !state.getDislikedClusters().isEmpty()) {
                var clusterNum = (int) (nextRandom() * state.getDislikedClusters().size());
                var similarMovies = movieService.findLeastSimilar(state.getDislikedClusters().get(clusterNum).getCentroid(), 1, excludeIds);
                if (similarMovies.isEmpty()) {
                    return movieService.getUnseenMovie(excludeIds);
                } else {
                    return Optional.of(similarMovies.get(0));
                }
            }
            return movieService.getUnseenMovie(excludeIds);
        } else {
            var clusters = state.getClusters();
            var clusterNum = (int) (nextRandom() * clusters.size());
            var similarMovies = movieService.findSimilar(clusters.get(clusterNum).getCentroid(), 1, excludeIds);
            if (similarMovies.isEmpty()) {
                return movieService.getUnseenMovie(excludeIds);
            } else {
                return Optional.of(similarMovies.get(0));
            }
        }
    }

    public double nextRandom() {
        return Math.random();
    }
}
