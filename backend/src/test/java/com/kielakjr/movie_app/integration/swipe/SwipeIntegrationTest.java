package com.kielakjr.movie_app.integration.swipe;

import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SwipeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (101, 'Movie 1', false)");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (102, 'Movie 2', false)");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (103, 'Movie 3', false)");
    }

    @Test
    void getNext_returnsMovie_whenMoviesExist() throws Exception {
        mockMvc.perform(get("/api/swipe/next").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void getNext_throwsWhenNoDatabaseMovies() throws Exception {
        jdbcTemplate.execute("DELETE FROM movies");

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/swipe/next").session(session)).andReturn()
        );
    }

    @Test
    void getNext_throwsWhenAllMoviesHaveBeenSwiped() throws Exception {
        for (long tmdbId : new long[]{101L, 102L, 103L}) {
            mockMvc.perform(post("/api/swipe")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"movie_id\": %d, \"action\": \"SKIP\"}".formatted(tmdbId)))
                    .andExpect(status().isOk());
        }

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/swipe/next").session(session)).andReturn()
        );
    }

    @Test
    void swipeLike_filtersMovieFromNextFeed() throws Exception {
        long firstTmdbId = getTmdbIdFromNextFeed(session);

        swipe(session, firstTmdbId, "LIKE");

        long secondTmdbId = getTmdbIdFromNextFeed(session);

        if (firstTmdbId == secondTmdbId) {
            throw new AssertionError("LIKE did not filter movie from next feed");
        }
    }

    @Test
    void swipeDislike_filtersMovieFromNextFeed() throws Exception {
        long firstTmdbId = getTmdbIdFromNextFeed(session);

        swipe(session, firstTmdbId, "DISLIKE");

        long secondTmdbId = getTmdbIdFromNextFeed(session);

        if (firstTmdbId == secondTmdbId) {
            throw new AssertionError("DISLIKE did not filter movie from next feed");
        }
    }

    @Test
    void swipeSkip_filtersMovieFromNextFeed() throws Exception {
        long firstTmdbId = getTmdbIdFromNextFeed(session);

        swipe(session, firstTmdbId, "SKIP");

        long secondTmdbId = getTmdbIdFromNextFeed(session);

        if (firstTmdbId == secondTmdbId) {
            throw new AssertionError("SKIP did not filter movie from next feed");
        }
    }

    @Test
    void sessionIsolation_swipesInOneSessionDoNotAffectAnother() throws Exception {
        MockHttpSession sessionA = new MockHttpSession();
        MockHttpSession sessionB = new MockHttpSession();

        for (long tmdbId : new long[]{101L, 102L, 103L}) {
            swipe(sessionA, tmdbId, "SKIP");
        }

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/swipe/next").session(sessionA)).andReturn()
        );

        mockMvc.perform(get("/api/swipe/next").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists());
    }

    @Test
    void swipe_returns200_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": 101, \"action\": \"LIKE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void swipe_returns400_whenMovieIdIsNull() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": null, \"action\": \"LIKE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swipe_returns400_whenActionIsNull() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": 101, \"action\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swipe_returns400_whenActionIsInvalid() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": 101, \"action\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swipe_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private long getTmdbIdFromNextFeed(MockHttpSession httpSession) throws Exception {
        String response = mockMvc.perform(get("/api/swipe/next").session(httpSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractTmdbId(response);
    }

    private void swipe(MockHttpSession httpSession, long tmdbId, String action) throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(httpSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"%s\"}".formatted(tmdbId, action)))
                .andExpect(status().isOk());
    }

    private long extractTmdbId(String json) {
        String field = "\"tmdb_id\":";
        int index = json.indexOf(field);
        if (index == -1) throw new IllegalStateException("No tmdb_id in response: " + json);
        int start = index + field.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }
}
