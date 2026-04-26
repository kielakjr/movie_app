package com.kielakjr.movie_app.unit.seed;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.movie.Movie;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.seed.SeedService;
import com.kielakjr.movie_app.tmdb.TmdbClient;
import com.kielakjr.movie_app.tmdb.TmdbImageUrlBuilder;
import com.kielakjr.movie_app.tmdb.dto.TmdbGenreListResponse;
import com.kielakjr.movie_app.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.kielakjr.movie_app.tmdb.dto.TmdbKeywordsResponse;
import com.kielakjr.movie_app.tmdb.dto.TmdbMovieResponse;
import com.kielakjr.movie_app.tmdb.dto.TmdbMovieResponse.TmdbMovie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedServiceTest {

    @Mock private TmdbClient tmdbClient;
    @Mock private TmdbImageUrlBuilder imageUrlBuilder;
    @Mock private MovieService movieService;
    @Mock private EmbeddingClient embeddingClient;

    @InjectMocks
    private SeedService seedService;

    private TmdbGenreListResponse genres(int id, String name) {
        return new TmdbGenreListResponse(List.of(new TmdbGenre(id, name)));
    }

    private TmdbMovie movie(long id, String title, String releaseDate, List<Integer> genreIds) {
        return new TmdbMovie(false, null, genreIds, id, "en", "An overview.", 7.0, "/poster.jpg", releaseDate, title, 7.5, 1000);
    }

    private Movie savedMovie(long id, long tmdbId, String title) {
        return Movie.builder().id(id).tmdbId(tmdbId).title(title).adult(false).genres(List.of()).build();
    }

    private TmdbKeywordsResponse emptyKeywords() {
        return new TmdbKeywordsResponse(0L, new TmdbKeywordsResponse.TmdbKeyword[0]);
    }

    private TmdbKeywordsResponse keywords(String... names) {
        var kws = new TmdbKeywordsResponse.TmdbKeyword[names.length];
        for (int i = 0; i < names.length; i++) {
            kws[i] = new TmdbKeywordsResponse.TmdbKeyword(i, names[i]);
        }
        return new TmdbKeywordsResponse(0L, kws);
    }

    @Nested
    class SeedPopularMovies {

        @Nested
        class Deduplication {

            @Test
            void newMovies_areSavedAndCounted() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(101L, "Mad Max", "2015-05-14", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn("https://img/poster.jpg");
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 101L, "Mad Max")));
                when(tmdbClient.getKeywords(101L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[]{0.1f});

                assertThat(seedService.seedPopularMovies(1)).isEqualTo(1);
                verify(movieService).saveAll(argThat(list -> list.size() == 1));
            }

            @Test
            void existingMovies_areFilteredOut() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(101L, "Already Seeded", "2015-01-01", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of(101L));

                assertThat(seedService.seedPopularMovies(1)).isEqualTo(0);
                verify(movieService, never()).saveAll(any());
            }

            @Test
            void duplicateTmdbIdAcrossPages_isSavedOnce() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(99L, "Film", "2020-01-01", List.of(28)))));
                when(tmdbClient.getPopularMovies(2)).thenReturn(new TmdbMovieResponse(2, List.of(movie(99L, "Film", "2020-01-01", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 99L, "Film")));
                when(tmdbClient.getKeywords(99L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[0]);

                assertThat(seedService.seedPopularMovies(2)).isEqualTo(1);
                verify(movieService).saveAll(argThat(list -> list.size() == 1));
            }
        }

        @Nested
        class EmbeddingGeneration {

            @Test
            void successfulEmbedding_updatesMovieRecord() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(1L, "Film", "2020-01-01", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(10L, 1L, "Film")));
                when(tmdbClient.getKeywords(1L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[]{0.1f, 0.2f});

                seedService.seedPopularMovies(1);

                verify(movieService).batchUpdateEmbeddings(argThat(map -> map.containsKey(10L)));
            }

            @Test
            void failedEmbedding_movieStillSavedWithoutEmbedding() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(2L, "No Embed", "2020-06-01", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(2L, 2L, "No Embed")));
                when(tmdbClient.getKeywords(2L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[0]);

                assertThat(seedService.seedPopularMovies(1)).isEqualTo(1);
                verify(movieService, never()).batchUpdateEmbeddings(any());
            }

            @Test
            void keywords_areIncludedInEmbeddingText() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(5L, "Inception", "2010-07-16", List.of(28)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(50L, 5L, "Inception")));
                when(tmdbClient.getKeywords(5L)).thenReturn(keywords("dream", "heist"));

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                when(embeddingClient.embed(textCaptor.capture())).thenReturn(new float[]{0.1f});

                seedService.seedPopularMovies(1);

                assertThat(textCaptor.getValue()).contains("Keywords:").contains("dream").contains("heist");
            }
        }

        @Nested
        class MovieMapping {

            @Test
            void unknownGenreIds_areSkippedInGenreList() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(77L, "Mystery", "2021-01-01", List.of(28, 999)))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(tmdbClient.getKeywords(77L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[]{0.1f});

                ArgumentCaptor<List<Movie>> captor = ArgumentCaptor.forClass(List.class);
                when(movieService.saveAll(captor.capture())).thenReturn(List.of(savedMovie(3L, 77L, "Mystery")));

                seedService.seedPopularMovies(1);

                assertThat(captor.getValue().get(0).getGenres()).containsExactly("Action");
            }

            @Test
            void nullReleaseDate_doesNotThrow() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(1L, "No Date", null, List.of()))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 1L, "No Date")));
                when(tmdbClient.getKeywords(1L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[0]);

                assertThat(seedService.seedPopularMovies(1)).isEqualTo(1);
            }

            @Test
            void malformedReleaseDate_doesNotThrow() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(2L, "Bad Date", "not-a-date", List.of()))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(2L, 2L, "Bad Date")));
                when(tmdbClient.getKeywords(2L)).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[0]);

                assertThat(seedService.seedPopularMovies(1)).isEqualTo(1);
            }
        }

        @Nested
        class MultiPageFetch {

            @Test
            void fetchesEachPageFromTmdb() {
                when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
                when(tmdbClient.getPopularMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(1L, "A", "2020-01-01", List.of()))));
                when(tmdbClient.getPopularMovies(2)).thenReturn(new TmdbMovieResponse(2, List.of(movie(2L, "B", "2021-01-01", List.of()))));
                when(movieService.findAllTmdbIds()).thenReturn(Set.of());
                when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
                when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
                when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 1L, "A"), savedMovie(2L, 2L, "B")));
                when(tmdbClient.getKeywords(anyLong())).thenReturn(emptyKeywords());
                when(embeddingClient.embed(any())).thenReturn(new float[0]);

                assertThat(seedService.seedPopularMovies(2)).isEqualTo(2);
                verify(tmdbClient).getPopularMovies(1);
                verify(tmdbClient).getPopularMovies(2);
            }
        }
    }

    @Nested
    class SeedTopRatedMovies {

        @Test
        void callsTopRatedEndpoint_notPopular() {
            when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(200L, "Godfather", "1972-03-24", List.of(28)))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of());
            when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
            when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
            when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 200L, "Godfather")));
            when(tmdbClient.getKeywords(200L)).thenReturn(emptyKeywords());
            when(embeddingClient.embed(any())).thenReturn(new float[0]);

            seedService.seedTopRatedMovies(1);

            verify(tmdbClient).getTopRatedMovies(1);
            verify(tmdbClient, never()).getPopularMovies(anyInt());
        }

        @Test
        void newMovies_areSavedAndCounted() {
            when(tmdbClient.getGenres()).thenReturn(genres(18, "Drama"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(300L, "Shawshank", "1994-09-23", List.of(18)))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of());
            when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
            when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
            when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 300L, "Shawshank")));
            when(tmdbClient.getKeywords(300L)).thenReturn(emptyKeywords());
            when(embeddingClient.embed(any())).thenReturn(new float[]{0.5f});

            assertThat(seedService.seedTopRatedMovies(1)).isEqualTo(1);
        }

        @Test
        void existingMovies_areFilteredOut() {
            when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(400L, "Already Here", "2000-01-01", List.of(28)))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of(400L));

            assertThat(seedService.seedTopRatedMovies(1)).isEqualTo(0);
            verify(movieService, never()).saveAll(any());
        }

        @Test
        void multiPageFetch_callsEachPage() {
            when(tmdbClient.getGenres()).thenReturn(genres(28, "Action"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(1L, "A", "2020-01-01", List.of()))));
            when(tmdbClient.getTopRatedMovies(2)).thenReturn(new TmdbMovieResponse(2, List.of(movie(2L, "B", "2021-01-01", List.of()))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of());
            when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
            when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
            when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 1L, "A"), savedMovie(2L, 2L, "B")));
            when(tmdbClient.getKeywords(anyLong())).thenReturn(emptyKeywords());
            when(embeddingClient.embed(any())).thenReturn(new float[0]);

            assertThat(seedService.seedTopRatedMovies(2)).isEqualTo(2);
            verify(tmdbClient).getTopRatedMovies(1);
            verify(tmdbClient).getTopRatedMovies(2);
        }

        @Test
        void embeddingGenerated_updatesMovieRecord() {
            when(tmdbClient.getGenres()).thenReturn(genres(18, "Drama"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(5L, "Citizen Kane", "1941-09-05", List.of(18)))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of());
            when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
            when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
            when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(50L, 5L, "Citizen Kane")));
            when(tmdbClient.getKeywords(5L)).thenReturn(emptyKeywords());
            when(embeddingClient.embed(any())).thenReturn(new float[]{0.9f});

            seedService.seedTopRatedMovies(1);

            verify(movieService).batchUpdateEmbeddings(argThat(map -> map.containsKey(50L)));
        }

        @Test
        void duplicateTmdbIdAcrossPages_isSavedOnce() {
            when(tmdbClient.getGenres()).thenReturn(genres(18, "Drama"));
            when(tmdbClient.getTopRatedMovies(1)).thenReturn(new TmdbMovieResponse(1, List.of(movie(99L, "Dup", "2000-01-01", List.of(18)))));
            when(tmdbClient.getTopRatedMovies(2)).thenReturn(new TmdbMovieResponse(2, List.of(movie(99L, "Dup", "2000-01-01", List.of(18)))));
            when(movieService.findAllTmdbIds()).thenReturn(Set.of());
            when(imageUrlBuilder.posterUrl(any())).thenReturn(null);
            when(imageUrlBuilder.backdropUrl(any())).thenReturn(null);
            when(movieService.saveAll(any())).thenReturn(List.of(savedMovie(1L, 99L, "Dup")));
            when(tmdbClient.getKeywords(99L)).thenReturn(emptyKeywords());
            when(embeddingClient.embed(any())).thenReturn(new float[0]);

            assertThat(seedService.seedTopRatedMovies(2)).isEqualTo(1);
            verify(movieService).saveAll(argThat(list -> list.size() == 1));
        }
    }
}
