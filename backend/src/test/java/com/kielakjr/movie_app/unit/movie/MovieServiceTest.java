package com.kielakjr.movie_app.unit.movie;

import com.kielakjr.movie_app.movie.Movie;
import com.kielakjr.movie_app.movie.MovieRepository;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Nested
    class ToVectorString {

        @Test
        void normalArray_producesCorrectFormat() {
            assertThat(MovieService.toVectorString(new float[]{1.0f, 2.5f, -0.5f}))
                    .isEqualTo("[1.0,2.5,-0.5]");
        }

        @Test
        void emptyArray_producesEmptyBrackets() {
            assertThat(MovieService.toVectorString(new float[0])).isEqualTo("[]");
        }

        @Test
        void singleElement_producesCorrectFormat() {
            assertThat(MovieService.toVectorString(new float[]{0.0f})).isEqualTo("[0.0]");
        }
    }

    @Nested
    class GetAllMovies {

        @Test
        void mapsAllEntityFieldsToDto() {
            Movie movie = Movie.builder()
                    .id(1L).tmdbId(100L).title("Inception")
                    .overview("A thief enters dreams")
                    .releaseDate(LocalDate.of(2010, 7, 16))
                    .originalLanguage("en").adult(false)
                    .posterPath("/poster.jpg").backdropPath("/backdrop.jpg")
                    .genres(List.of("Action", "Sci-Fi"))
                    .popularity(85.0).voteAverage(8.8).voteCount(20000)
                    .build();

            PageRequest pageable = PageRequest.of(0, 10);
            when(movieRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(movie)));

            Page<MovieResponse> result = movieService.getAllMovies(pageable);

            assertThat(result.getContent()).hasSize(1);
            MovieResponse dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.tmdbId()).isEqualTo(100L);
            assertThat(dto.title()).isEqualTo("Inception");
            assertThat(dto.releaseDate()).isEqualTo("2010-07-16");
            assertThat(dto.genres()).containsExactly("Action", "Sci-Fi");
            assertThat(dto.adult()).isFalse();
        }

        @Test
        void nullReleaseDate_mapsToNull() {
            Movie movie = Movie.builder()
                    .id(1L).tmdbId(1L).title("Unknown").adult(false)
                    .releaseDate(null).genres(List.of())
                    .build();

            PageRequest pageable = PageRequest.of(0, 10);
            when(movieRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(movie)));

            assertThat(movieService.getAllMovies(pageable).getContent().get(0).releaseDate()).isNull();
        }

        @Test
        void nullGenres_mapsToEmptyArray() {
            Movie movie = Movie.builder()
                    .id(1L).tmdbId(1L).title("Unknown").adult(false)
                    .genres(null)
                    .build();

            PageRequest pageable = PageRequest.of(0, 10);
            when(movieRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(movie)));

            assertThat(movieService.getAllMovies(pageable).getContent().get(0).genres()).isEmpty();
        }
    }

    @Nested
    class FindAllTmdbIds {

        @Test
        void delegatesToRepository() {
            when(movieRepository.findAllTmdbIds()).thenReturn(Set.of(1L, 2L, 3L));
            assertThat(movieService.findAllTmdbIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
    }

    @Nested
    class GetUnseenMovie {

        @Test
        void mapsRepositoryResultToDto() {
            Movie movie = Movie.builder()
                    .id(42L).tmdbId(4242L).title("Title 42")
                    .overview("Overview").releaseDate(LocalDate.of(2020, 1, 1))
                    .originalLanguage("en").adult(false)
                    .posterPath("/poster.jpg").backdropPath("/backdrop.jpg")
                    .genres(List.of("Genre")).popularity(10.0)
                    .voteAverage(5.0).voteCount(100)
                    .build();

            when(movieRepository.findUnseen(any(), any())).thenReturn(List.of(movie));

            var result = movieService.getUnseenMovie(Set.of()).orElseThrow();
            assertThat(result.id()).isEqualTo(42L);
            assertThat(result.tmdbId()).isEqualTo(4242L);
            assertThat(result.title()).isEqualTo("Title 42");
        }

        @Test
        void returnsEmptyWhenRepositoryFindsNothing() {
            when(movieRepository.findUnseen(any(), any())).thenReturn(List.of());

            assertThat(movieService.getUnseenMovie(Set.of(101L, 102L))).isEmpty();
        }

        @Test
        void passesSeenIdsToRepository() {
            when(movieRepository.findUnseen(any(), any())).thenReturn(List.of());

            movieService.getUnseenMovie(Set.of(101L, 102L));

            verify(movieRepository).findUnseen(eq(Set.of(101L, 102L)), any());
        }

    }

    @Nested
    class FindUnseenPool {

        private Movie buildMovie(long id) {
            return Movie.builder()
                    .id(id).tmdbId(id * 10).title("Movie " + id)
                    .overview("Overview").adult(false)
                    .genres(List.of()).popularity(3.0)
                    .voteAverage(5.0).voteCount(100)
                    .build();
        }

        @Test
        void delegatesToRepositoryWithLimit() {
            when(movieRepository.findUnseen(any(), any())).thenReturn(List.of(buildMovie(1L), buildMovie(2L)));

            var pool = movieService.findUnseenPool(Set.of(99L), 50);

            assertThat(pool).hasSize(2);
            assertThat(pool.get(0).id()).isEqualTo(1L);
            verify(movieRepository).findUnseen(eq(Set.of(99L)), eq(PageRequest.of(0, 50)));
        }

        @Test
        void returnsEmptyWhenRepositoryFindsNothing() {
            when(movieRepository.findUnseen(any(), any())).thenReturn(List.of());

            assertThat(movieService.findUnseenPool(Set.of(), 25)).isEmpty();
        }
    }

    @Nested
    class GetMovieByEmbedding {

        @Test
        void mapsRepositoryResultToDto() {
            float[] embedding = new float[]{0.1f, 0.2f};
            String vecString = "[0.1,0.2]";
            Movie movie = Movie.builder()
                    .id(99L).tmdbId(999L).title("Embedded Movie")
                    .overview("Overview").releaseDate(LocalDate.of(2021, 5, 5))
                    .originalLanguage("en").adult(false)
                    .posterPath("/poster.jpg").backdropPath("/backdrop.jpg")
                    .genres(List.of("Genre")).popularity(20.0)
                    .voteAverage(7.5).voteCount(500)
                    .build();

            when(movieRepository.findByEmbedding(vecString)).thenReturn(Optional.of(movie));

            var result = movieService.getMovieByEmbedding(embedding).orElseThrow();
            assertThat(result.id()).isEqualTo(99L);
            assertThat(result.tmdbId()).isEqualTo(999L);
            assertThat(result.title()).isEqualTo("Embedded Movie");
        }

        @Test
        void returnsEmptyWhenRepositoryFindsNothing() {
            float[] embedding = new float[]{0.1f, 0.2f};
            String vecString = "[0.1,0.2]";

            when(movieRepository.findByEmbedding(vecString)).thenReturn(Optional.empty());

            assertThat(movieService.getMovieByEmbedding(embedding)).isEmpty();
        }

        @Test
        void convertsEmbeddingToVectorString() {
            float[] embedding = new float[]{0.3f, 0.4f};
            String vecString = "[0.3,0.4]";

            when(movieRepository.findByEmbedding(vecString)).thenReturn(Optional.empty());

            movieService.getMovieByEmbedding(embedding);

            verify(movieRepository).findByEmbedding(eq(vecString));
        }
    }
}
