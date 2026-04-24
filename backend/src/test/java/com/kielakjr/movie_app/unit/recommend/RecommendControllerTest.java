package com.kielakjr.movie_app.unit.recommend;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.kielakjr.movie_app.recommend.RecommendController;
import com.kielakjr.movie_app.recommend.RecommendService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RecommendControllerTest {
    @Mock
    private RecommendService recommendService;

    @InjectMocks
    private RecommendController recommendController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recommendController).build();
    }

    @Nested
    class GetRecommendedMovies {
        @Test
        void validRequest_returnsOk() throws Exception {
            mockMvc.perform(get("/api/recommend")
                            .param("limit", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        void missingLimitParam_returnsOk() throws Exception {
            mockMvc.perform(get("/api/recommend"))
                    .andExpect(status().isOk());
        }

        @Test
        void negativeLimitParam_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommend")
                            .param("limit", "-5"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void zeroLimitParam_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommend")
                            .param("limit", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void nonIntegerLimitParam_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommend")
                            .param("limit", "abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void limitExceedingMax_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommend")
                            .param("limit", "101"))
                    .andExpect(status().isBadRequest());
        }

    }
}
