package com.kielakjr.movie_app.unit.search;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.search.SearchService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            when(embeddingClient.embed(query)).thenReturn(new float[]{0.1f, 0.2f});
            searchService.searchSimilar(query, 5);
            verify(embeddingClient).embed(query);
        }
    }
}
