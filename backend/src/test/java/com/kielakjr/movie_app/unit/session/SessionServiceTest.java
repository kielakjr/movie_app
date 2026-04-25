package com.kielakjr.movie_app.unit.session;

import com.kielakjr.movie_app.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class SessionServiceTest {

    private SessionService sessionService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService();
        session = new MockHttpSession();
    }

    @Nested
    class GetState {
        @Test
        void shouldReturnEmptyCollectionsForNewSession() {
            var state = sessionService.getState(session);
            assertThat(state.getSeenMovieIds()).isEmpty();
            assertThat(state.getLikedMovieIds()).isEmpty();
            assertThat(state.getDislikedMovieIds()).isEmpty();
            assertThat(state.getClusters()).isEmpty();
            assertThat(state.getLikesCount()).isZero();
        }

        @Test
        void shouldReturnSameStateOnSubsequentCalls() {
            var state1 = sessionService.getState(session);
            state1.getSeenMovieIds().add(1L);
            state1.getLikedMovieIds().add(2L);
            state1.getDislikedMovieIds().add(3L);
            state1.getClusters().add(null);
            state1.setLikesCount(5);

            var state2 = sessionService.getState(session);
            assertThat(state2.getSeenMovieIds()).containsExactly(1L);
            assertThat(state2.getLikedMovieIds()).containsExactly(2L);
            assertThat(state2.getDislikedMovieIds()).containsExactly(3L);
            assertThat(state2.getClusters()).hasSize(1);
            assertThat(state2.getLikesCount()).isEqualTo(5);
        }
    }
}
