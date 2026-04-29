package com.kielakjr.movie_app.integration.session;

import com.kielakjr.movie_app.integration.base.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionRedisIntegrationTest extends BaseIntegrationTest {

    private static final String SESSION_COOKIE_NAME = "MOVIE_APP_SESSION";
    private static final String SPRING_SESSION_KEY_PREFIX = "spring:session:";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        redis = new StringRedisTemplate(redisConnectionFactory);

        Set<String> keys = redis.keys(SPRING_SESSION_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }

        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (201, 'Movie A', false)");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (202, 'Movie B', false)");
        jdbcTemplate.update("INSERT INTO movies (tmdb_id, title, adult) VALUES (203, 'Movie C', false)");
    }

    @Test
    void firstRequest_setsSessionCookieAndCreatesKeyInRedis() throws Exception {
        long initialKeyCount = countSessionKeys();

        MvcResult result = mockMvc.perform(get("/api/swipe/next"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertThat(cookie)
                .as("Server must issue a %s cookie on first request", SESSION_COOKIE_NAME)
                .isNotNull();
        assertThat(cookie.getValue()).isNotBlank();

        long afterKeyCount = countSessionKeys();
        assertThat(afterKeyCount)
                .as("A spring:session:* key should be created in Redis after a request that touches the session")
                .isGreaterThan(initialKeyCount);
    }

    @Test
    void sessionStatePersistsAcrossRequests_viaRedis() throws Exception {
        Cookie cookie = openSession();

        long firstId = idFromNextFeed(cookie);

        swipe(cookie, firstId, "LIKE");

        long secondId = idFromNextFeed(cookie);
        assertThat(secondId)
                .as("Movie liked in previous request must not reappear — proves session state survived via Redis")
                .isNotEqualTo(firstId);
    }

    @Test
    void allSwipesPersist_acrossManyRequestsOnSameCookie() throws Exception {
        Cookie cookie = openSession();

        for (long id : allMovieIds()) {
            swipe(cookie, id, "SKIP");
        }

        mockMvc.perform(get("/api/swipe/next").cookie(cookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void differentCookies_haveIsolatedStateInRedis() throws Exception {
        Cookie cookieA = openSession();
        Cookie cookieB = openSession();

        assertThat(cookieA.getValue())
                .as("Distinct sessions must have distinct cookie values")
                .isNotEqualTo(cookieB.getValue());

        for (long id : allMovieIds()) {
            swipe(cookieA, id, "SKIP");
        }

        mockMvc.perform(get("/api/swipe/next").cookie(cookieA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/swipe/next").cookie(cookieB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists());
    }

    @Test
    void reset_clearsSessionStateInRedis() throws Exception {
        Cookie cookie = openSession();

        for (long id : allMovieIds()) {
            swipe(cookie, id, "SKIP");
        }

        mockMvc.perform(get("/api/swipe/next").cookie(cookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/session/reset").cookie(cookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/swipe/next").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdb_id").exists());
    }

    @Test
    void sessionData_isStoredInRedis_notJustInMemory() throws Exception {
        Cookie cookie = openSession();
        long firstId = idFromNextFeed(cookie);
        swipe(cookie, firstId, "LIKE");

        Set<String> keys = redis.keys(SPRING_SESSION_KEY_PREFIX + "*");
        assertThat(keys)
                .as("Spring Session must write session keys into Redis")
                .isNotNull()
                .isNotEmpty();

        boolean foundStateAttribute = keys.stream().anyMatch(k -> {
            var hashOps = redis.opsForHash();
            var entries = hashOps.entries(k);
            return entries.keySet().stream()
                    .map(Object::toString)
                    .anyMatch(field -> field.contains("STATE"));
        });
        assertThat(foundStateAttribute)
                .as("At least one session hash in Redis should contain the STATE attribute set by SessionService")
                .isTrue();
    }

    @Test
    void sessionCookie_isReusedAcrossRequests_andRedisKeyDoesNotMultiply() throws Exception {
        Cookie cookie = openSession();
        long initialKeys = countSessionKeys();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/swipe/next").cookie(cookie))
                    .andExpect(status().is2xxSuccessful());
        }

        long afterKeys = countSessionKeys();
        assertThat(afterKeys)
                .as("Reusing the same session cookie must not create new Redis session entries on each request")
                .isEqualTo(initialKeys);
    }

    private Cookie openSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/swipe/next"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long idFromNextFeed(Cookie cookie) throws Exception {
        String body = mockMvc.perform(get("/api/swipe/next").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractLongField(body, "\"id\":");
    }

    private void swipe(Cookie cookie, long movieId, String action) throws Exception {
        mockMvc.perform(post("/api/swipe")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movie_id\": %d, \"action\": \"%s\"}".formatted(movieId, action)))
                .andExpect(status().is2xxSuccessful());
    }

    private List<Long> allMovieIds() {
        return jdbcTemplate.queryForList("SELECT id FROM movies ORDER BY tmdb_id", Long.class);
    }

    private long countSessionKeys() {
        Set<String> keys = redis.keys(SPRING_SESSION_KEY_PREFIX + "*");
        return keys == null ? 0 : keys.size();
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
