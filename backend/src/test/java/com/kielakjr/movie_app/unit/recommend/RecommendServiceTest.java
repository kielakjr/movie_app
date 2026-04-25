package com.kielakjr.movie_app.unit.recommend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.session.SwipeSessionState;
import com.kielakjr.movie_app.recommend.RecommendService;
import com.kielakjr.movie_app.cluster.Cluster;
import org.mockito.InjectMocks;

@ExtendWith(MockitoExtension.class)
public class RecommendServiceTest {
    @Mock
    private MovieService movieService;
    @Mock
    private SessionService sessionService;

    @InjectMocks
    private RecommendService recommendService;

    private SwipeSessionState state;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        state = new SwipeSessionState();
        session = new MockHttpSession();
        when(sessionService.getState(any())).thenReturn(state);
    }

    @Nested
    class GetRecommendedMovies {

        @Test
        void throwsIfNoClusters() {
            assertThatThrownBy(() -> recommendService.getRecommendedMovies(session, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User embedding not set");
        }

        @Test
        void callsMovieServiceWithClusterCentroid() {
            var cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{1.0f, 0.5f});
            state.getClusters().add(cluster);

            recommendService.getRecommendedMovies(session, 10);

            verify(movieService).findSimilar(eq(cluster.getCentroid()), anyInt(), eq(state.getSeenMovieIds()));

        }
    }
}
