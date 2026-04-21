package com.kielakjr.movie_app.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.embedding.EmbeddingClient;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
    @Mock
    private MovieService movieService;
    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private SearchService searchService;

    @Nested
    class SearchSimilar {

        @Test
        void callsEmbeddingClientWithQuery() {
            String query = "test query";
            searchService.searchSimilar(query, 5);
            verify(embeddingClient).embed(query);
        }
    }
}
