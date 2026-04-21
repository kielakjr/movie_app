package com.kielakjr.movie_app.search;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.embedding.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final MovieService movieService;
    private final EmbeddingClient embeddingClient;

    @Transactional(readOnly = true)
    public List<MovieResponse> searchSimilar(String query, int limit) {
        var queryEmbedding = embeddingClient.embed(query);
        var similarMovies = movieService.findSimilar(queryEmbedding, limit);
        return similarMovies;
    }
}
