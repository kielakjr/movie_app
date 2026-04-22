package com.kielakjr.movie_app.unit.swipe;

import com.kielakjr.movie_app.swipe.SwipeController;
import com.kielakjr.movie_app.swipe.SwipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SwipeControllerTest {

    @Mock
    private SwipeService swipeService;

    @InjectMocks
    private SwipeController swipeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(swipeController).build();
    }

    @Nested
    class Swipe {

        @Test
        void likeRequest_returnsOk() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": 1, "action": "LIKE"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        void dislikeRequest_returnsOk() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": 1, "action": "DISLIKE"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        void skipRequest_returnsOk() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": 1, "action": "SKIP"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        void nullMovieId_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": null, "action": "LIKE"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void nullAction_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": 1, "action": null}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownAction_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/swipe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"movieId": 1, "action": "INVALID"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}
