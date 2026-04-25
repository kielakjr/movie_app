package com.kielakjr.movie_app.recommend;

import org.springframework.stereotype.Service;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.Cluster;
import com.kielakjr.movie_app.cluster.ClusterService;
import com.kielakjr.movie_app.recommend.dto.RecommendMovieResponse;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

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
        List<RecommendMovie> recommendations = new ArrayList<>();
        int perClusterLimit = state.getClusters().size() * 5 < limit ? (int) Math.ceil((float) limit / state.getClusters().size()) : 5;
        for (Cluster cluster : state.getClusters()) {
            var clusterRecommendations = getRecommendedMoviesForCluster(cluster, perClusterLimit, seenMovieIds);
            recommendations.addAll(clusterRecommendations);
        }
        ClusterService.cosineSimilarity(new float[]{1, 0}, new float[]{0, 1});
        return recommendations.stream()
                .sorted((a, b) -> Float.compare(b.similarity(), a.similarity()))
                .limit(limit)
                .map(RecommendMovie::response)
                .toList();
    }

    private List<RecommendMovie> getRecommendedMoviesForCluster(Cluster cluster, int limit, Set<Long> seenMovieIds) {
        var similarMovies = movieService.findSimilar(cluster.getCentroid(), limit, seenMovieIds);
        if (similarMovies.isEmpty()) {
            return List.of();
        }
        var reasonMovie = movieService.getMovieByEmbedding(cluster.getCentroid()).orElse(null);
        if (reasonMovie == null) {
            throw new IllegalStateException("No movie found for cluster centroid");
        }
        List<RecommendMovie> recommendations = new ArrayList<>();
        for (MovieResponse movie : similarMovies) {
            var response = toRecommendMovieResponse(movie, reasonMovie);
            recommendations.add(new RecommendMovie(ClusterService.cosineSimilarity(movieService.getEmbeddingById(movie.id()), cluster.getCentroid()), response));
        }
        return recommendations;
    }

    private RecommendMovieResponse toRecommendMovieResponse(MovieResponse movie, MovieResponse reason) {
        return new RecommendMovieResponse(movie, reason);
    }

    private record RecommendMovie(float similarity, RecommendMovieResponse response) {
    }
}
