package com.kielakjr.movie_app.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class SearchControllerTest {
    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
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
    class SearchSimilar {

        @Test
        void returnsSearchResultsFromService() throws Exception {
            String query = "test query";
            List<MovieResponse> results = List.of(sampleMovie());
            when(searchService.searchSimilar(query, 5)).thenReturn(results);

            mockMvc.perform(get("/api/search/similar")
                            .param("query", query)
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(results.size()))
                    .andExpect(jsonPath("$[0].id").value(results.get(0).id()))
                    .andExpect(jsonPath("$[0].tmdbId").value(results.get(0).tmdbId()))
                    .andExpect(jsonPath("$[0].title").value(results.get(0).title()));
        }

        @Test
        void missingQueryParameterReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/search/similar")
                            .param("limit", "5"))
                    .andExpect(status().isBadRequest());
        }

    }
}
