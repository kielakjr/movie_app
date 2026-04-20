package com.kielakjr.movie_app.tmdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class TmdbClientTest {

    private TmdbClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        client = new TmdbClient(builder.build());
    }

    @Test
    void shouldFetchGenres() {
        String json = """
            {
              "genres": [
                { "id": 28, "name": "Action" }
              ]
            }
        """;

        mockServer.expect(requestTo("https://api.themoviedb.org/3/genre/movie/list"))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.getGenres();

        assertNotNull(result);
        assertEquals(1, result.genres().size());
        assertEquals("Action", result.genres().get(0).name());
    }

    @Test
    void shouldFetchPopularMovies() {
        String json = """
            {
              "page": 1,
              "results": []
            }
        """;

        mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/popular?page=1"))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.getPopularMovies(1);

        assertNotNull(result);
        assertEquals(1, result.page());
    }
}
