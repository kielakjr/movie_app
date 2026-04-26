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

    @Nested
    class GetKeywords {

        @Test
        void hitsCorrectUrlAndDeserializesKeywords() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/278/keywords"))
                    .andRespond(withSuccess("""
                            {
                              "id": 278,
                              "keywords": [
                                {"id": 1, "name": "hope"},
                                {"id": 2, "name": "prison"}
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var result = client.getKeywords(278L);

            assertNotNull(result);
            assertEquals(278L, result.id());
            assertEquals(2, result.keywords().length);
            assertEquals("hope", result.keywords()[0].name());
            assertEquals("prison", result.keywords()[1].name());
        }

        @Test
        void emptyKeywords_returnsEmptyArray() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/1/keywords"))
                    .andRespond(withSuccess("""
                            {"id": 1, "keywords": []}
                            """, MediaType.APPLICATION_JSON));

            var result = client.getKeywords(1L);

            assertNotNull(result);
            assertEquals(0, result.keywords().length);
        }
    }

    @Nested
    class GetTopRatedMovies {

        @Nested
        class Validation {

            @Test
            void pageZero_throwsIllegalArgument() {
                assertThrows(IllegalArgumentException.class, () -> client.getTopRatedMovies(0));
            }

            @Test
            void page501_throwsIllegalArgument() {
                assertThrows(IllegalArgumentException.class, () -> client.getTopRatedMovies(501));
            }

            @Test
            void page1_isAccepted() {
                mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/top_rated?page=1"))
                        .andRespond(withSuccess("""
                                {"page": 1, "results": []}
                                """, MediaType.APPLICATION_JSON));

                assertDoesNotThrow(() -> client.getTopRatedMovies(1));
            }

            @Test
            void page500_isAccepted() {
                mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/top_rated?page=500"))
                        .andRespond(withSuccess("""
                                {"page": 500, "results": []}
                                """, MediaType.APPLICATION_JSON));

                assertDoesNotThrow(() -> client.getTopRatedMovies(500));
            }
        }

        @Test
        void hitsTopRatedEndpoint_notPopular() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/top_rated?page=1"))
                    .andRespond(withSuccess("""
                            {"page": 1, "results": []}
                            """, MediaType.APPLICATION_JSON));

            client.getTopRatedMovies(1);

            mockServer.verify();
        }

        @Test
        void emptyResults_returnsPageWithEmptyList() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/top_rated?page=1"))
                    .andRespond(withSuccess("""
                            {"page": 1, "results": []}
                            """, MediaType.APPLICATION_JSON));

            var result = client.getTopRatedMovies(1);

            assertNotNull(result);
            assertEquals(1, result.page());
            assertNotNull(result.results());
            assertEquals(0, result.results().size());
        }

        @Test
        void withResults_deserializesAllMovieFields() {
            mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/top_rated?page=1"))
                    .andRespond(withSuccess("""
                            {
                              "page": 1,
                              "results": [{
                                "id": 278,
                                "title": "The Shawshank Redemption",
                                "overview": "Two imprisoned men bond.",
                                "release_date": "1994-09-23",
                                "original_language": "en",
                                "adult": false,
                                "poster_path": "/shawshank.jpg",
                                "backdrop_path": "/backdrop.jpg",
                                "genre_ids": [18],
                                "popularity": 120.0,
                                "vote_average": 8.7,
                                "vote_count": 25000
                              }]
                            }
                            """, MediaType.APPLICATION_JSON));

            var result = client.getTopRatedMovies(1);
            var movie = result.results().get(0);

            assertEquals(1, result.results().size());
            assertEquals(278L, movie.id());
            assertEquals("The Shawshank Redemption", movie.title());
            assertEquals(8.7, movie.vote_average());
            assertEquals(1, movie.genre_ids().size());
        }
    }
}
