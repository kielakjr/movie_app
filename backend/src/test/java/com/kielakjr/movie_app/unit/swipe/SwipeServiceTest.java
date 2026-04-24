package com.kielakjr.movie_app.unit.swipe;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.session.SwipeSessionState;
import com.kielakjr.movie_app.cluster.ClusterService;
import com.kielakjr.movie_app.swipe.SwipeService;
import com.kielakjr.movie_app.swipe.dto.SwipeAction;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private MovieService movieService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private SwipeService swipeService;

    private SwipeSessionState state;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        state = new SwipeSessionState();
        session = new MockHttpSession();
        when(sessionService.getState(any())).thenReturn(state);
    }

    private MovieResponse createMovieResponse(Long id) {
        return new MovieResponse(id, 100L + id, "Title " + id, "Overview " + id, "2024-01-01", "en", false, "/poster" + id + ".jpg", "/backdrop" + id + ".jpg", new String[]{"Genre1", "Genre2"}, 10.0, 8.0, 100);
    }

    @Nested
    class Swipe {

        @Nested
        class Like {

            private static final float[] MOVIE_EMBEDDING = {1.0f, 0.5f};
            private static final float[] UPDATED_EMBEDDING = {0.8f, 0.4f};

            @BeforeEach
            void setUp() {
                when(movieService.getEmbeddingById(any())).thenReturn(MOVIE_EMBEDDING);
                when(clusterService.updateUserCluster(any(), any(), anyInt())).thenReturn(UPDATED_EMBEDDING);
            }

            @Test
            void addsMovieToSeenIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                assertThat(state.getSeenMovieIds()).containsExactly(42L);
            }

            @Test
            void addsMovieToLikedIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                assertThat(state.getLikedMovieIds()).containsExactly(42L);
            }

            @Test
            void incrementsLikesCount() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                assertThat(state.getLikesCount()).isEqualTo(1);
            }

            @Test
            void doesNotAddToDislikedIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                assertThat(state.getDislikedMovieIds()).isEmpty();
            }

            @Test
            void fetchesEmbeddingByMovieId() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                verify(movieService).getEmbeddingById(42L);
            }

            @Test
            void updatesUserEmbeddingInState() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                assertThat(state.getUserEmbedding()).isEqualTo(UPDATED_EMBEDDING);
            }

            @Test
            void callsClusterServiceWithInitialNullEmbeddingAndIncrementedCount() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                verify(clusterService).updateUserCluster(isNull(), eq(MOVIE_EMBEDDING), eq(1));
            }

            @Test
            void subsequentLikePassesPreviousUpdatedEmbeddingToCluster() {
                swipeService.swipe(new SwipeRequest(1L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(2L, SwipeAction.LIKE), session);

                verify(clusterService).updateUserCluster(eq(UPDATED_EMBEDDING), eq(MOVIE_EMBEDDING), eq(2));
            }

            @Test
            void callsClusterServiceOncePerLike() {
                swipeService.swipe(new SwipeRequest(1L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(2L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(3L, SwipeAction.LIKE), session);

                verify(clusterService, times(3)).updateUserCluster(any(), any(), anyInt());
            }

            @Test
            void multipleLikes_accumulateLikesCountAndSeenIds() {
                swipeService.swipe(new SwipeRequest(1L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(2L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(3L, SwipeAction.LIKE), session);

                assertThat(state.getLikesCount()).isEqualTo(3);
                assertThat(state.getSeenMovieIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
            }
        }

        @Nested
        class Dislike {

            @Test
            void addsMovieToDislikedIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                assertThat(state.getDislikedMovieIds()).containsExactly(42L);
            }

            @Test
            void doesNotIncrementLikesCount() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                assertThat(state.getLikesCount()).isEqualTo(0);
            }

            @Test
            void addsToSeenIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                assertThat(state.getSeenMovieIds()).containsExactly(42L);
            }

            @Test
            void doesNotCallClusterService() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                verify(clusterService, never()).updateUserCluster(any(), any(), anyInt());
            }
        }

        @Nested
        class Skip {

            @Test
            void addsMovieToSeenIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

                assertThat(state.getSeenMovieIds()).containsExactly(42L);
            }

            @Test
            void doesNotIncrementLikesCount() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

                assertThat(state.getLikesCount()).isEqualTo(0);
            }

            @Test
            void doesNotAddToDislikedIds() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

                assertThat(state.getDislikedMovieIds()).isEmpty();
            }

            @Test
            void doesNotCallClusterService() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

                verify(clusterService, never()).updateUserCluster(any(), any(), anyInt());
            }
        }
    }

    @Nested
    class GetNextFeed {

        @Test
        void returnsNextMovieFromService() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.of(createMovieResponse(42L)));

            var response = swipeService.getNextFeed(session);

            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.title()).isEqualTo("Title 42");
        }

        @Test
        void noMoreMovies_throwsException() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> swipeService.getNextFeed(session));
        }
    }
}
