package com.kielakjr.movie_app.swipe;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.Cluster;
import com.kielakjr.movie_app.cluster.ClusterService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

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
    private static final float W_SIM = 0.55f;
    private static final float W_POP = 0.30f;
    private static final float W_NOISE = 0.15f;

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
        double maxLogPop = maxLogPopularity(candidates);
        MovieResponse best = null;
        double bestScore = -Double.MAX_VALUE;
        for (MovieResponse candidate : candidates) {
            float[] embedding = movieService.getEmbeddingById(candidate.id());
            double similarity = embedding == null ? 0.0
                    : ClusterService.cosineSimilarity(embedding, cluster.getCentroid());
            double popNorm = normalizedPopularity(candidate, maxLogPop);
            double score = W_SIM * similarity + W_POP * popNorm + W_NOISE * nextRandom();
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
        List<MovieResponse> kept = new ArrayList<>();
        for (MovieResponse movie : pool) {
            float[] embedding = movieService.getEmbeddingById(movie.id());
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
            weights[i] = Math.log1p(safePopularity(pool.get(i))) + 1.0;
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

    private static double normalizedPopularity(MovieResponse movie, double maxLogPop) {
        if (maxLogPop <= 0.0) return 0.0;
        return Math.log1p(safePopularity(movie)) / maxLogPop;
    }

    private static double maxLogPopularity(List<MovieResponse> movies) {
        double max = 0.0;
        for (MovieResponse m : movies) {
            max = Math.max(max, Math.log1p(safePopularity(m)));
        }
        return max;
    }

    private static double safePopularity(MovieResponse movie) {
        Double pop = movie.popularity();
        if (pop == null || pop < 0.0) return 0.0;
        return pop;
    }

    public double nextRandom() {
        return Math.random();
    }
}
