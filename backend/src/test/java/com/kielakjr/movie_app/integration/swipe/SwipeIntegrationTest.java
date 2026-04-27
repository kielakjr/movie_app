package com.kielakjr.movie_app.integration.swipe;

import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
    void getUnseen_returnsMovie_whenMoviesExist() throws Exception {
        mockMvc.perform(get("/api/swipe/next").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void getUnseen_returnsNoContent_whenNoDatabaseMovies() throws Exception {
        jdbcTemplate.execute("DELETE FROM movies");

        mockMvc.perform(get("/api/swipe/next").session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUnseen_returnsNoContent_whenAllMoviesHaveBeenSwiped() throws Exception {
        for (long id : allMovieIds()) {
            swipe(session, id, "SKIP");
        }

        mockMvc.perform(get("/api/swipe/next").session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void swipeLike_filtersMovieFromNextFeed() throws Exception {
        long firstId = getIdFromNextFeed(session);

        swipe(session, firstId, "LIKE");

        long secondId = getIdFromNextFeed(session);

        if (firstId == secondId) {
            throw new AssertionError("LIKE did not filter movie from next feed");
        }
    }

    @Test
    void swipeDislike_filtersMovieFromNextFeed() throws Exception {
        long firstId = getIdFromNextFeed(session);

        swipe(session, firstId, "DISLIKE");

        long secondId = getIdFromNextFeed(session);

        if (firstId == secondId) {
            throw new AssertionError("DISLIKE did not filter movie from next feed");
        }
    }

    @Test
    void swipeSkip_filtersMovieFromNextFeed() throws Exception {
        long firstId = getIdFromNextFeed(session);

        swipe(session, firstId, "SKIP");

        long secondId = getIdFromNextFeed(session);

        if (firstId == secondId) {
            throw new AssertionError("SKIP did not filter movie from next feed");
        }
    }

    @Test
    void sessionIsolation_swipesInOneSessionDoNotAffectAnother() throws Exception {
        MockHttpSession sessionA = new MockHttpSession();
        MockHttpSession sessionB = new MockHttpSession();

        for (long id : allMovieIds()) {
            swipe(sessionA, id, "SKIP");
        }

        mockMvc.perform(get("/api/swipe/next").session(sessionA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/swipe/next").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists());
    }

    @Test
    void swipe_returnsNextMovie_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": 101, \"action\": \"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists());
    }

    @Test
    void swipe_returnsNoContent_whenAllMoviesSwiped() throws Exception {
        for (long id : allMovieIds()) {
            swipe(session, id, "SKIP");
        }
        long anyId = allMovieIds().get(0);

        mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"SKIP\"}".formatted(anyId)))
                .andExpect(status().isNoContent());
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

    @Test
    void swipe_returnsDifferentMovie_thanTheJustSwipedOne() throws Exception {
        long currentId = getIdFromNextFeed(session);

        String response = mockMvc.perform(post("/api/swipe")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"SKIP\"}".formatted(currentId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long nextId = extractLongField(response, "\"id\":");
        assertThat(nextId).isNotEqualTo(currentId);
    }

    private long getIdFromNextFeed(MockHttpSession httpSession) throws Exception {
        String response = mockMvc.perform(get("/api/swipe/next").session(httpSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractLongField(response, "\"id\":");
    }

    private void swipe(MockHttpSession httpSession, long movieId, String action) throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .session(httpSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"%s\"}".formatted(movieId, action)))
                .andExpect(status().is2xxSuccessful());
    }

    private List<Long> allMovieIds() {
        return jdbcTemplate.queryForList("SELECT id FROM movies ORDER BY tmdb_id", Long.class);
    }

    private long extractLongField(String json, String field) {
        int index = json.indexOf(field);
        if (index == -1) throw new IllegalStateException("Field " + field + " not found in: " + json);
        int start = index + field.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }
}
