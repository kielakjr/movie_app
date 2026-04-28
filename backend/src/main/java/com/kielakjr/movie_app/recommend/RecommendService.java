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
import java.util.HashSet;

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
        var seenMovieIds = new HashSet<>(state.getLikedMovieIds());
        var dislikedMovieIds = new HashSet<>(state.getDislikedMovieIds());
        seenMovieIds.addAll(dislikedMovieIds);
        int perClusterLimit = state.getClusters().size() * 5 < limit ? (int) Math.ceil((float) limit / state.getClusters().size()) : 5;
        List<List<RecommendMovie>> perCluster = new ArrayList<>();
        for (Cluster cluster : state.getClusters()) {
            var clusterRecommendations = new ArrayList<>(getRecommendedMoviesForCluster(cluster, perClusterLimit, seenMovieIds));
            clusterRecommendations.sort((a, b) -> Float.compare(b.similarity(), a.similarity()));
            perCluster.add(clusterRecommendations);
        }
        List<RecommendMovieResponse> result = new ArrayList<>();
        boolean addedAny = true;
        while (result.size() < limit && addedAny) {
            addedAny = false;
            for (List<RecommendMovie> clusterRecs : perCluster) {
                if (result.size() >= limit) break;
                if (!clusterRecs.isEmpty()) {
                    result.add(clusterRecs.remove(0).response());
                    addedAny = true;
                }
            }
        }
        return result;
    }

    private List<RecommendMovie> getRecommendedMoviesForCluster(Cluster cluster, int limit, Set<Long> seenMovieIds) {
        var similarMovies = movieService.findSimilar(cluster.getCentroid(), limit, seenMovieIds);
        if (similarMovies.isEmpty()) {
            return List.of();
        }
        List<RecommendMovie> recommendations = new ArrayList<>();
        for (MovieResponse movie : similarMovies) {
            float[] movieEmbedding = movieService.getEmbeddingById(movie.id());
            float[] reasonEmbedding = findClosestClusterEmbedding(movieEmbedding, cluster.getMovieEmbeddings());
            var reasonMovie = movieService.getMovieByEmbedding(reasonEmbedding).orElse(null);
            if (reasonMovie == null) {
                throw new IllegalStateException("No movie found for reason embedding");
            }
            var response = toRecommendMovieResponse(movie, reasonMovie);
            recommendations.add(new RecommendMovie(ClusterService.cosineSimilarity(movieEmbedding, cluster.getCentroid()), response));
        }
        return recommendations;
    }

    private float[] findClosestClusterEmbedding(float[] target, List<float[]> clusterEmbeddings) {
        float[] best = null;
        float bestSimilarity = -Float.MAX_VALUE;
        for (float[] candidate : clusterEmbeddings) {
            float similarity = ClusterService.cosineSimilarity(target, candidate);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                best = candidate;
            }
        }
        return best;
    }

    private RecommendMovieResponse toRecommendMovieResponse(MovieResponse movie, MovieResponse reason) {
        return new RecommendMovieResponse(movie, reason);
    }

    private record RecommendMovie(float similarity, RecommendMovieResponse response) {
    }
}
