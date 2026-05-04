package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.Cluster;
import com.kielakjr.movie_app.cluster.ClusterService;
import com.kielakjr.movie_app.scoring.MovieScorer;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SwipeService {
    private final SessionService sessionService;
    private final MovieService movieService;
    private final ClusterService clusterService;

    private static final float EXPLORATION_RATE = 0.3f;
    private static final int EXPLOIT_POOL_SIZE = 25;
    private static final int EXPLORE_POOL_SIZE = 50;
    private static final float DISLIKE_SIMILARITY_THRESHOLD = 0.5f;

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
        sessionService.save(httpSession, state);

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
        boolean explore = state.getClusters().isEmpty() || nextRandom() < EXPLORATION_RATE;
        if (explore) {
            return pickFromExplorePool(state.getDislikedClusters(), excludeIds);
        }
        var clusters = state.getClusters();
        var cluster = clusters.get((int) (nextRandom() * clusters.size()));
        return pickFromExploitPool(cluster, excludeIds);
    }

    private Optional<MovieResponse> pickFromExploitPool(Cluster cluster, Set<Long> excludeIds) {
        var candidates = movieService.findSimilar(cluster.getCentroid(), EXPLOIT_POOL_SIZE, excludeIds);
        if (candidates.isEmpty()) {
            return movieService.getUnseenMovie(excludeIds);
        }
        Map<Long, float[]> embeddings = movieService.getEmbeddingsByIds(
                candidates.stream().map(MovieResponse::id).collect(Collectors.toSet()));
        double maxLogPop = MovieScorer.maxLogPopularity(candidates);
        MovieResponse best = null;
        double bestScore = -Double.MAX_VALUE;
        for (MovieResponse candidate : candidates) {
            float[] embedding = embeddings.get(candidate.id());
            double similarity = embedding == null ? 0.0
                    : ClusterService.cosineSimilarity(embedding, cluster.getCentroid());
            double popNorm = MovieScorer.normalizedPopularity(candidate, maxLogPop);
            double score = MovieScorer.score(similarity, popNorm, nextRandom());
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<MovieResponse> pickFromExplorePool(List<Cluster> dislikedClusters, Set<Long> excludeIds) {
        var pool = movieService.findUnseenPool(excludeIds, EXPLORE_POOL_SIZE);
        if (pool.isEmpty()) {
            return movieService.getUnseenMovie(excludeIds);
        }
        var filtered = filterAwayFromDisliked(pool, dislikedClusters);
        var working = filtered.isEmpty() ? pool : filtered;
        return Optional.of(weightedPickByPopularity(working));
    }

    private List<MovieResponse> filterAwayFromDisliked(List<MovieResponse> pool, List<Cluster> dislikedClusters) {
        if (dislikedClusters.isEmpty()) {
            return pool;
        }
        Map<Long, float[]> embeddings = movieService.getEmbeddingsByIds(
                pool.stream().map(MovieResponse::id).collect(Collectors.toSet()));
        List<MovieResponse> kept = new ArrayList<>();
        for (MovieResponse movie : pool) {
            float[] embedding = embeddings.get(movie.id());
            if (embedding == null) {
                kept.add(movie);
                continue;
            }
            boolean tooClose = false;
            for (Cluster c : dislikedClusters) {
                if (c.getCentroid() == null) continue;
                if (ClusterService.cosineSimilarity(embedding, c.getCentroid()) > DISLIKE_SIMILARITY_THRESHOLD) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                kept.add(movie);
            }
        }
        return kept;
    }

    private MovieResponse weightedPickByPopularity(List<MovieResponse> pool) {
        double total = 0.0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            weights[i] = Math.log1p(MovieScorer.safePopularity(pool.get(i))) + 1.0;
            total += weights[i];
        }
        double pick = nextRandom() * total;
        double cumulative = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights[i];
            if (pick <= cumulative) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }

    public double nextRandom() {
        return Math.random();
    }
}
