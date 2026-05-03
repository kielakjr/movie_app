package com.kielakjr.movie_app.unit.search;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
    @Mock
    private MovieService movieService;
    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private SearchService searchService;

    @BeforeEach
    void enableEmbedding() {
        ReflectionTestUtils.setField(searchService, "embeddingEnabled", true);
    }

    @Nested
    class SearchSimilar {

        @Test
        void callsEmbeddingClientWithQuery() {
            String query = "test query";
            when(embeddingClient.embed(query)).thenReturn(new float[]{0.1f, 0.2f});
            searchService.searchSimilar(query, 5);
            verify(embeddingClient).embed(query);
        }

        @Test
        void whenEmbeddingDisabled_throwsServiceUnavailable() {
            ReflectionTestUtils.setField(searchService, "embeddingEnabled", false);
            ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> searchService.searchSimilar("anything", 5)
            );
            assertEquals(503, ex.getStatusCode().value());
            verifyNoInteractions(embeddingClient, movieService);
        }
    }
}
