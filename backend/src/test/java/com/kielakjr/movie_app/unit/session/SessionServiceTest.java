package com.kielakjr.movie_app.unit.session;

import com.kielakjr.movie_app.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Set;

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
        void noExistingState_likesCountIsZero() {
            assertThat(sessionService.getState(session).getLikesCount()).isEqualTo(0);
        }

        @Test
        void noExistingState_seenMovieIdsIsEmpty() {
            assertThat(sessionService.getState(session).getSeenMovieIds()).isEmpty();
        }

        @Test
        void noExistingState_dislikedMovieIdsIsEmpty() {
            assertThat(sessionService.getState(session).getDislikedMovieIds()).isEmpty();
        }

        @Test
        void noExistingState_userEmbeddingIsNull() {
            assertThat(sessionService.getState(session).getUserEmbedding()).isNull();
        }

        @Test
        void calledTwice_returnsSameInstance() {
            assertThat(sessionService.getState(session)).isSameAs(sessionService.getState(session));
        }

        @Test
        void modifiedLikesCount_persistsAcrossCalls() {
            sessionService.getState(session).setLikesCount(5);

            assertThat(sessionService.getState(session).getLikesCount()).isEqualTo(5);
        }

        @Test
        void modifiedSeenMovieIds_persistsAcrossCalls() {
            sessionService.getState(session).setSeenMovieIds(Set.of(1L, 2L));

            assertThat(sessionService.getState(session).getSeenMovieIds()).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        void modifiedUserEmbedding_persistsAcrossCalls() {
            sessionService.getState(session).setUserEmbedding(new float[]{0.1f, 0.2f});

            assertThat(sessionService.getState(session).getUserEmbedding()).containsExactly(0.1f, 0.2f);
        }

        @Test
        void differentSessions_returnDifferentInstances() {
            MockHttpSession other = new MockHttpSession();

            assertThat(sessionService.getState(session)).isNotSameAs(sessionService.getState(other));
        }

        @Test
        void differentSessions_haveIndependentState() {
            MockHttpSession other = new MockHttpSession();
            sessionService.getState(session).setLikesCount(10);

            assertThat(sessionService.getState(other).getLikesCount()).isEqualTo(0);
        }
    }
}
