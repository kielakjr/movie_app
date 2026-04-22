package com.kielakjr.movie_app.movie;

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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    class GetNextMovie {

        @Test
        void returnsNextMovieFromRepository() {
            Movie movie = Movie.builder()
                    .id(42L).tmdbId(4242L).title("Title 42")
                    .overview("Overview").releaseDate(LocalDate.of(2020, 1, 1))
                    .originalLanguage("en").adult(false)
                    .posterPath("/poster.jpg").backdropPath("/backdrop.jpg")
                    .genres(List.of("Genre")).popularity(10.0)
                    .voteAverage(5.0).voteCount(100)
                    .build();

            when(movieRepository.findAll(Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(movie)));

            var result = movieService.getNextMovie(Set.of()).orElseThrow();
            assertThat(result.id()).isEqualTo(42L);
            assertThat(result.tmdbId()).isEqualTo(4242L);
            assertThat(result.title()).isEqualTo("Title 42");
        }

        @Test
        void seenMoviesAreFilteredOut() {
            Movie movie1 = Movie.builder().id(1L).tmdbId(101L).title("Movie 1").build();
            Movie movie2 = Movie.builder().id(2L).tmdbId(102L).title("Movie 2").build();

            when(movieRepository.findAll(Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(movie1, movie2)));

            var result = movieService.getNextMovie(Set.of(101L)).orElseThrow();
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.tmdbId()).isEqualTo(102L);
            assertThat(result.title()).isEqualTo("Movie 2");
        }

    }
}
