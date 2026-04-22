package com.kielakjr.movie_app.unit.tmdb;

import com.kielakjr.movie_app.tmdb.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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

    @Nested
    class GetGenres {

        @Test
        void returnsDeserializedGenreList() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/genre/movie/list"))
                    .andRespond(withSuccess("""
                            {"genres": [{"id": 28, "name": "Action"}]}
                            """, MediaType.APPLICATION_JSON));

            var result = client.getGenres();

            assertNotNull(result);
            assertEquals(1, result.genres().size());
            assertEquals("Action", result.genres().get(0).name());
        }
    }

    @Nested
    class GetPopularMovies {

        @Nested
        class Validation {

            @Test
            void pageZero_throwsIllegalArgument() {
                assertThrows(IllegalArgumentException.class, () -> client.getPopularMovies(0));
            }

            @Test
            void page501_throwsIllegalArgument() {
                assertThrows(IllegalArgumentException.class, () -> client.getPopularMovies(501));
            }
        }

        @Test
        void emptyResults_returnsPageWithEmptyList() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/popular?page=1"))
                    .andRespond(withSuccess("""
                            {"page": 1, "results": []}
                            """, MediaType.APPLICATION_JSON));

            var result = client.getPopularMovies(1);

            assertNotNull(result);
            assertEquals(1, result.page());
        }

        @Test
        void withResults_deserializesAllMovieFields() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/popular?page=1"))
                    .andRespond(withSuccess("""
                            {
                              "page": 1,
                              "results": [{
                                "id": 12345,
                                "title": "Mad Max",
                                "overview": "Post-apocalyptic action.",
                                "release_date": "2015-05-14",
                                "original_language": "en",
                                "adult": false,
                                "poster_path": "/poster.jpg",
                                "backdrop_path": "/backdrop.jpg",
                                "genre_ids": [28, 53],
                                "popularity": 90.5,
                                "vote_average": 7.6,
                                "vote_count": 15000
                              }]
                            }
                            """, MediaType.APPLICATION_JSON));

            var result = client.getPopularMovies(1);
            var movie = result.results().get(0);

            assertEquals(1, result.results().size());
            assertEquals(12345L, movie.id());
            assertEquals("Mad Max", movie.title());
            assertEquals(2, movie.genre_ids().size());
        }
    }
}
