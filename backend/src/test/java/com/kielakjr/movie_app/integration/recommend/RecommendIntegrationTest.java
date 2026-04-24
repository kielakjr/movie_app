package com.kielakjr.movie_app.integration.recommend;

import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import com.kielakjr.movie_app.session.SwipeSessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecommendIntegrationTest extends BaseIntegrationTest {

    private static final int VECTOR_DIMS = 384;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        jdbcTemplate.execute("DELETE FROM movies");
    }

    @Nested
    class RecommendEndpointTests {

        @Test
        void recommend_returns400_withHelpfulMessage_whenNoUserEmbedding() throws Exception {
            mockMvc.perform(get("/api/recommend").session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("User embedding not set"));
        }

        @Test
        void recommend_returnsEmptyList_whenNoMoviesHaveEmbeddings() throws Exception {
            jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (201, 'No Embedding Movie', false)");
            setUserEmbedding(session, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void recommend_returnsMovies_whenMoviesHaveEmbeddings() throws Exception {
            insertMovieWithEmbedding(201L, "Action Movie", uniformVector(0.9f));
            insertMovieWithEmbedding(202L, "Drama Film", uniformVector(0.1f));
            setUserEmbedding(session, uniformVector(0.9f));

            mockMvc.perform(get("/api/recommend").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tmdb_id").exists())
                    .andExpect(jsonPath("$[0].title").exists());
        }

        @Test
        void recommend_respectsLimitParameter() throws Exception {
            for (int i = 1; i <= 5; i++) {
                insertMovieWithEmbedding(200L + i, "Movie " + i, uniformVector(0.5f));
            }
            setUserEmbedding(session, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").session(session).param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void recommend_usesDefaultLimitOfTen_whenLimitParamIsOmitted() throws Exception {
            for (int i = 1; i <= 15; i++) {
                insertMovieWithEmbedding(200L + i, "Movie " + i, uniformVector(0.5f));
            }
            setUserEmbedding(session, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(10));
        }

        @Test
        void recommend_returnsMoviesSortedBySimilarityToUserEmbedding() throws Exception {
            float[] similarEmbedding = uniformVector(0.9f);
            float[] dissimilarEmbedding = uniformVector(0.1f);
            insertMovieWithEmbedding(201L, "Very Similar Movie", similarEmbedding);
            insertMovieWithEmbedding(202L, "Less Similar Movie", dissimilarEmbedding);
            setUserEmbedding(session, uniformVector(0.9f));

            mockMvc.perform(get("/api/recommend").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tmdb_id").value(201));
        }

        @Test
        void recommend_sessionIsolation_embeddingInOneSessionDoesNotAffectAnother() throws Exception {
            MockHttpSession sessionWithEmbedding = new MockHttpSession();
            MockHttpSession sessionWithoutEmbedding = new MockHttpSession();
            setUserEmbedding(sessionWithEmbedding, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").session(sessionWithEmbedding))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/recommend").session(sessionWithoutEmbedding))
                    .andExpect(status().isBadRequest());
        }
    }

    private void setUserEmbedding(MockHttpSession httpSession, float[] embedding) {
        SwipeSessionState state = new SwipeSessionState();
        state.setUserEmbedding(embedding);
        httpSession.setAttribute("STATE", state);
    }

    private float[] uniformVector(float value) {
        float[] v = new float[VECTOR_DIMS];
        Arrays.fill(v, value);
        return v;
    }

    private void insertMovieWithEmbedding(long tmdbId, String title, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO movies (tmdb_id, title, adult, embedding) VALUES (?, ?, false, CAST(? AS vector))",
                tmdbId, title, toVectorString(embedding)
        );
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}
