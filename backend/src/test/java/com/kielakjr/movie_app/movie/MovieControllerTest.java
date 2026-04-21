package com.kielakjr.movie_app.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    private MovieResponse sampleMovie() {
        return MovieResponse.builder()
                .id(1L).tmdbId(42L).title("The Matrix")
                .overview("Follow the white rabbit.")
                .releaseDate("1999-03-31").originalLanguage("en")
                .adult(false).posterPath("/poster.jpg").backdropPath("/backdrop.jpg")
                .genres(new String[]{"Action", "Sci-Fi"})
                .popularity(90.0).voteAverage(8.7).voteCount(15000)
                .build();
    }

    @Nested
    class GetAllMovies {

        @Test
        void returnsOkWithMovieContent() throws Exception {
            var page = new PageImpl<>(List.of(sampleMovie()), PageRequest.of(0, 20), 1);
            when(movieService.getAllMovies(any())).thenReturn(page);

            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("The Matrix"))
                    .andExpect(jsonPath("$.content[0].tmdb_id").value(42));
        }

        @Test
        void emptyPage_returnsEmptyContentArray() throws Exception {
            when(movieService.getAllMovies(any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Nested
        class Pagination {

            @Test
            void noParams_usesDefaultPageZeroAndSizeTwenty() throws Exception {
                when(movieService.getAllMovies(any())).thenReturn(new PageImpl<>(List.of()));

                mockMvc.perform(get("/api/movies")).andExpect(status().isOk());

                verify(movieService).getAllMovies(PageRequest.of(0, 20));
            }

            @Test
            void customParams_forwardedToService() throws Exception {
                when(movieService.getAllMovies(any())).thenReturn(new PageImpl<>(List.of()));

                mockMvc.perform(get("/api/movies").param("page", "2").param("size", "5"))
                        .andExpect(status().isOk());

                verify(movieService).getAllMovies(PageRequest.of(2, 5));
            }
        }
    }
}
