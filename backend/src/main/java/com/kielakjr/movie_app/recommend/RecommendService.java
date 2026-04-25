package com.kielakjr.movie_app.recommend;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.Cluster;
import com.kielakjr.movie_app.recommend.dto.RecommendMovieResponse;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendService {
    private final MovieService movieService;
    private final SessionService sessionService;

    public List<RecommendMovieResponse> getRecommendedMovies(HttpSession session, int limit) {
        var state = sessionService.getState(session);
        if (state.getClusters().isEmpty()) {
            throw new IllegalStateException("User embedding not set");
        }
        var seenMovieIds = state.getSeenMovieIds();
        List<RecommendMovieResponse> recommendations = new ArrayList<>();
        int perClusterLimit = limit / state.getClusters().size();
        for (Cluster cluster : state.getClusters()) {
            var clusterRecommendations = getRecommendedMoviesForCluster(cluster, perClusterLimit, seenMovieIds);
            recommendations.addAll(clusterRecommendations);
        }
        return recommendations;
    }

    private List<RecommendMovieResponse> getRecommendedMoviesForCluster(Cluster cluster, int limit, Set<Long> seenMovieIds) {
        var similarMovies = movieService.findSimilar(cluster.getCentroid(), limit, seenMovieIds);
        if (similarMovies.isEmpty()) {
            return List.of();
        }
        var reasonMovie = movieService.getMovieByEmbedding(cluster.getCentroid()).orElse(null);
        if (reasonMovie == null) {
            throw new IllegalStateException("No movie found for cluster centroid");
        }
        return similarMovies.stream()
                .map(m -> toRecommendMovieResponse(m, reasonMovie))
                .toList();
    }

    private RecommendMovieResponse toRecommendMovieResponse(MovieResponse movie, MovieResponse reason) {
        return new RecommendMovieResponse(movie, reason);
    }
}
