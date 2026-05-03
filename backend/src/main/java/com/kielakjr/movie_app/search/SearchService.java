package com.kielakjr.movie_app.search;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.embedding.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final MovieService movieService;
    private final EmbeddingClient embeddingClient;

    @Value("${embedding.enabled:true}")
    private boolean embeddingEnabled;

    public List<MovieResponse> searchSimilar(String query, int limit) {
        if (!embeddingEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Semantic search is currently disabled.");
        }
        var queryEmbedding = embeddingClient.embed(query);
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalStateException("Failed to generate embedding for the query.");
        }
        var similarMovies = movieService.findSimilar(queryEmbedding, limit);
        return similarMovies;
    }
}
