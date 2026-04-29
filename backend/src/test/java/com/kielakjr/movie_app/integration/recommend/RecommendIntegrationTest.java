package com.kielakjr.movie_app.integration.recommend;

import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import com.kielakjr.movie_app.session.SwipeSessionState;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kielakjr.movie_app.cluster.Cluster;
class RecommendIntegrationTest extends BaseIntegrationTest {

    private static final int VECTOR_DIMS = 384;
    private static final String SESSION_COOKIE_NAME = "MOVIE_APP_SESSION";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SessionRepository<? extends Session> sessionRepository;

    Cookie cookie;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM movies");
        cookie = openSession();
    }

    @Nested
    class WithDirectEmbedding {

        @Test
        void recommend_returns400_withHelpfulMessage_whenNoUserEmbedding() throws Exception {
            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("User embedding not set"));
        }

        @Test
        void recommend_returnsEmptyList_whenNoMoviesHaveEmbeddings() throws Exception {
            jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (201, 'No Embedding Movie', false)");
            seedUserEmbedding(cookie, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void recommend_returnsMovies_whenMoviesHaveEmbeddings() throws Exception {
            insertMovieWithEmbedding(201L, "Action Movie", uniformVector(0.9f));
            insertMovieWithEmbedding(202L, "Drama Film", uniformVector(0.1f));
            seedUserEmbedding(cookie, uniformVector(0.9f));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].movie.tmdb_id").exists())
                    .andExpect(jsonPath("$[0].movie.title").exists());
        }

        @Test
        void recommend_respectsLimitParameter() throws Exception {
            for (int i = 1; i <= 5; i++) {
                insertMovieWithEmbedding(200L + i, "Movie " + i, uniformVector(0.5f));
            }
            seedUserEmbedding(cookie, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").cookie(cookie).param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void recommend_usesDefaultLimitOfTen_whenLimitParamIsOmitted() throws Exception {
            for (int i = 1; i <= 15; i++) {
                insertMovieWithEmbedding(200L + i, "Movie " + i, uniformVector(0.5f));
            }
            seedUserEmbedding(cookie, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(10));
        }

        @Test
        void recommend_sessionIsolation_embeddingInOneSessionDoesNotAffectAnother() throws Exception {
            Cookie cookieWithEmbedding = openSession();
            Cookie cookieWithoutEmbedding = openSession();
            seedUserEmbedding(cookieWithEmbedding, uniformVector(0.5f));

            mockMvc.perform(get("/api/recommend").cookie(cookieWithEmbedding))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/recommend").cookie(cookieWithoutEmbedding))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AfterSwipingFlow {

        @Test
        void afterLikingMovieWithEmbedding_userEmbeddingIsBuilt_andRecommendationsAreReturned() throws Exception {
            insertMovieWithEmbedding(201L, "Liked Movie", genreAVector());
            insertMovieWithEmbedding(202L, "Another Movie", genreAVector());

            swipeLike(getMovieId(201L));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].movie.tmdb_id").value(202));
        }

        @Test
        void afterLikingMovieWithoutEmbedding_userEmbeddingIsNotSet_andRecommendationsFail() throws Exception {
            jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (201, 'No Embedding', false)");

            swipeLike(getMovieId(201L));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("User embedding not set"));
        }

        @Test
        void afterLiking_likedAndNotSeenAreRemovedFromRecommendations() throws Exception {
            insertMovieWithEmbedding(201L, "Liked", genreAVector());
            insertMovieWithEmbedding(202L, "Skipped", genreAVector());
            insertMovieWithEmbedding(203L, "Unseen", genreAVector());

            swipeLike(getMovieId(201L));
            swipeSkip(getMovieId(202L));

            mockMvc.perform(get("/api/recommend").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].movie.tmdb_id").value(202));
        }

        @Test
        void afterLikingGenreAMovies_genreAIsRecommendedBeforeGenreB() throws Exception {
            insertMovieWithEmbedding(201L, "Genre A liked", genreAVector());
            insertMovieWithEmbedding(202L, "Genre A unseen", genreAVector());
            insertMovieWithEmbedding(203L, "Genre B unseen", genreBVector());

            swipeLike(getMovieId(201L));

            mockMvc.perform(get("/api/recommend?limit=2").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].movie.tmdb_id").value(202))
                    .andExpect(jsonPath("$[1].movie.tmdb_id").value(203));
        }

        @Test
        void afterMultipleLikesOfSameGenre_genreStaysTopRecommendation() throws Exception {
            insertMovieWithEmbedding(201L, "Genre A liked 1", genreAVector());
            insertMovieWithEmbedding(202L, "Genre A liked 2", genreAVector());
            insertMovieWithEmbedding(203L, "Genre A unseen", genreAVector());
            insertMovieWithEmbedding(204L, "Genre B unseen", genreBVector());

            swipeLike(getMovieId(201L));
            swipeLike(getMovieId(202L));

            mockMvc.perform(get("/api/recommend?limit=2").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].movie.tmdb_id").value(203))
                    .andExpect(jsonPath("$[1].movie.tmdb_id").value(204));
        }
    }

    private Cookie openSession() throws Exception {
        var result = mockMvc.perform(get("/api/swipe/next"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Cookie c = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        if (c == null) throw new IllegalStateException("Session cookie not issued — Spring Session is not wired up");
        return c;
    }

    private void seedUserEmbedding(Cookie c, float[] embedding) {
        SwipeSessionState state = new SwipeSessionState();
        var cluster = new Cluster();
        cluster.addMovieEmbedding(embedding);
        state.getClusters().add(cluster);

        String sessionId = decodeSessionId(c.getValue());
        Session session = sessionRepository.findById(sessionId);
        if (session == null) throw new IllegalStateException("Session not found in repository: " + sessionId);
        session.setAttribute("STATE", state);
        saveTyped(session);
    }

    @SuppressWarnings("unchecked")
    private <S extends Session> void saveTyped(Session session) {
        ((SessionRepository<S>) sessionRepository).save((S) session);
    }

    private String decodeSessionId(String cookieValue) {
        try {
            return new String(Base64.getDecoder().decode(cookieValue));
        } catch (IllegalArgumentException e) {
            return cookieValue;
        }
    }

    private void swipeLike(long movieId) throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"LIKE\"}".formatted(movieId)))
                .andExpect(status().is2xxSuccessful());
    }

    private void swipeSkip(long movieId) throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"SKIP\"}".formatted(movieId)))
                .andExpect(status().is2xxSuccessful());
    }

    private long getMovieId(long tmdbId) {
        return jdbcTemplate.queryForObject("SELECT id FROM movies WHERE tmdb_id = ?", Long.class, tmdbId);
    }

    private float[] uniformVector(float value) {
        float[] v = new float[VECTOR_DIMS];
        Arrays.fill(v, value);
        return v;
    }

    private float[] genreAVector() {
        float[] v = new float[VECTOR_DIMS];
        Arrays.fill(v, 0, VECTOR_DIMS / 2, 1.0f);
        return v;
    }

    private float[] genreBVector() {
        float[] v = new float[VECTOR_DIMS];
        Arrays.fill(v, VECTOR_DIMS / 2, VECTOR_DIMS, 1.0f);
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
