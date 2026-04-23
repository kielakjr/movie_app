package com.kielakjr.movie_app.integration.search;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchIntegrationTest extends BaseIntegrationTest {

    private static final int VECTOR_DIMS = 384;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    EmbeddingClient embeddingClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM movies");
    }

    @Test
    void similar_returns400_whenQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/search/similar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returns400_whenQueryIsBlank() throws Exception {
        mockMvc.perform(get("/api/search/similar").param("query", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returns400_whenLimitIsBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/search/similar")
                        .param("query", "action")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returns400_whenLimitIsAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/search/similar")
                        .param("query", "action")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returns400_whenEmbeddingClientReturnsEmpty() throws Exception {
        when(embeddingClient.embed(anyString())).thenReturn(new float[0]);

        mockMvc.perform(get("/api/search/similar").param("query", "action"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returns400_whenEmbeddingClientReturnsNull() throws Exception {
        when(embeddingClient.embed(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/search/similar").param("query", "action"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similar_returnsEmptyList_whenNoMoviesHaveEmbeddings() throws Exception {
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (201, 'No Embedding Movie', false)");

        when(embeddingClient.embed(anyString())).thenReturn(uniformVector(0.1f));

        mockMvc.perform(get("/api/search/similar").param("query", "action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void similar_returnsMovies_whenMoviesHaveEmbeddings() throws Exception {
        insertMovieWithEmbedding(201L, "Action Movie", uniformVector(0.9f));
        insertMovieWithEmbedding(202L, "Drama Film", uniformVector(0.1f));

        when(embeddingClient.embed(anyString())).thenReturn(uniformVector(0.9f));

        mockMvc.perform(get("/api/search/similar").param("query", "action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tmdb_id").exists())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void similar_respectsLimitParameter() throws Exception {
        insertMovieWithEmbedding(201L, "Movie 1", uniformVector(0.5f));
        insertMovieWithEmbedding(202L, "Movie 2", uniformVector(0.5f));
        insertMovieWithEmbedding(203L, "Movie 3", uniformVector(0.5f));
        insertMovieWithEmbedding(204L, "Movie 4", uniformVector(0.5f));
        insertMovieWithEmbedding(205L, "Movie 5", uniformVector(0.5f));

        when(embeddingClient.embed(anyString())).thenReturn(uniformVector(0.5f));

        mockMvc.perform(get("/api/search/similar")
                        .param("query", "action")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void similar_usesDefaultLimitOfTen_whenLimitParamIsOmitted() throws Exception {
        for (int i = 1; i <= 15; i++) {
            insertMovieWithEmbedding(200L + i, "Movie " + i, uniformVector(0.5f));
        }

        when(embeddingClient.embed(anyString())).thenReturn(uniformVector(0.5f));

        mockMvc.perform(get("/api/search/similar").param("query", "action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10));
    }

    private float[] uniformVector(float value) {
        float[] v = new float[VECTOR_DIMS];
        Arrays.fill(v, value);
        return v;
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }

    private void insertMovieWithEmbedding(long tmdbId, String title, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO movies (tmdb_id, title, adult, embedding) VALUES (?, ?, false, CAST(? AS vector))",
                tmdbId, title, toVectorString(embedding)
        );
    }
}
