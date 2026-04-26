package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.ClusterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeService {
    private final SessionService sessionService;
    private final MovieService movieService;
    private final ClusterService clusterService;
    private static final float EXPLORATION_RATE = 0.2f;

    public void swipe(SwipeRequest request, HttpSession httpSession) {
        var state = sessionService.getState(httpSession);
        int seenBefore = state.getSeenMovieIds().size();

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
            }
            case SKIP -> state.getSeenMovieIds().add(request.movieId());
        }

        log.info("[swipe] sid={} action={} movieId={} seen {} -> {} ids={} clusters={}",
                httpSession.getId(), request.action(), request.movieId(),
                seenBefore, state.getSeenMovieIds().size(),
                state.getSeenMovieIds(), state.getClusters().size());
    }

    public Optional<MovieResponse> getNextFeed(HttpSession httpSession) {
        var state = sessionService.getState(httpSession);
        var excludeIds = new HashSet<>(state.getSeenMovieIds());
        log.info("[next] sid={} seen={} ids={}", httpSession.getId(), excludeIds.size(), excludeIds);
        var result = getNextMovie(httpSession, excludeIds);
        logResult("next", httpSession.getId(), excludeIds, result);
        return result;
    }

    public Optional<MovieResponse> peekNextFeed(HttpSession httpSession, Long excludeId) {
        var state = sessionService.getState(httpSession);
        var excludeIds = new HashSet<>(state.getSeenMovieIds());
        excludeIds.add(excludeId);
        log.info("[peek] sid={} excludeId={} seen+exclude={} ids={}",
                httpSession.getId(), excludeId, excludeIds.size(), excludeIds);
        var result = getNextMovie(httpSession, excludeIds);
        logResult("peek", httpSession.getId(), excludeIds, result);
        return result;
    }

    private Optional<MovieResponse> getNextMovie(HttpSession httpSession, Set<Long> excludeIds) {
        var state = sessionService.getState(httpSession);
        var rand = Math.random();
        if (state.getClusters().isEmpty() || rand < EXPLORATION_RATE) {
            log.info("[getNext] sid={} path=EXPLORE clusters={} rand={}",
                    httpSession.getId(), state.getClusters().size(), rand);
            return movieService.getUnseenMovie(excludeIds);
        } else {
            var clusters = state.getClusters();
            var clusterNum = (int) (Math.random() * clusters.size());
            log.info("[getNext] sid={} path=EXPLOIT clusterIdx={}/{} rand={}",
                    httpSession.getId(), clusterNum, clusters.size(), rand);
            var similarMovies = movieService.findSimilar(clusters.get(clusterNum).getCentroid(), 1, excludeIds);
            if (similarMovies.isEmpty()) {
                log.info("[getNext] sid={} EXPLOIT empty, falling back to EXPLORE", httpSession.getId());
                return movieService.getUnseenMovie(excludeIds);
            } else {
                return Optional.of(similarMovies.get(0));
            }
        }
    }

    private void logResult(String tag, String sid, Set<Long> excludeIds, Optional<MovieResponse> result) {
        if (result.isEmpty()) {
            log.info("[{}-result] sid={} returned=EMPTY", tag, sid);
            return;
        }
        Long id = result.get().id();
        boolean leak = excludeIds.contains(id);
        log.info("[{}-result] sid={} returned={} title=\"{}\" LEAK={}",
                tag, sid, id, result.get().title(), leak);
        if (leak) {
            log.error("[{}-LEAK] sid={} returned movie id={} which IS in excludeIds={}",
                    tag, sid, id, excludeIds);
        }
    }
}
