package com.kielakjr.movie_app.unit.swipe;

import com.kielakjr.movie_app.cluster.Cluster;
import com.kielakjr.movie_app.cluster.ClusterService;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.session.SwipeSessionState;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        return new MovieResponse(id, 100L + id, "Title " + id, "Overview " + id, "2024-01-01", "en",
                false, "/poster" + id + ".jpg", "/backdrop" + id + ".jpg", new String[]{"Genre1", "Genre2"}, 10.0, 8.0, 100);
    }

    @Nested
    class Swipe {

        @Nested
        class Like {

            private static final float[] MOVIE_EMBEDDING = {1.0f, 0.5f};

            @BeforeEach
            void setUp() {
                when(movieService.getEmbeddingById(any())).thenReturn(MOVIE_EMBEDDING);
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
            void addsMovieToClusters() {
                swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

                verify(clusterService).addToClusters(eq(MOVIE_EMBEDDING), eq(state.getClusters()));
            }

            @Test
            void callsClusterServiceOncePerLike() {
                swipeService.swipe(new SwipeRequest(1L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(2L, SwipeAction.LIKE), session);
                swipeService.swipe(new SwipeRequest(3L, SwipeAction.LIKE), session);

                verify(clusterService, times(3)).addToClusters(any(), any());
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
            void whenEmbeddingIsNull_doesNotCallClusterService() {
                when(movieService.getEmbeddingById(42L)).thenReturn(null);

                swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                verify(clusterService, never()).addToClusters(any(), any());
            }

            @Nested
            class WithEmbedding {

                private static final float[] MOVIE_EMBEDDING = {0.5f, 0.8f};

                @BeforeEach
                void setUp() {
                    when(movieService.getEmbeddingById(any())).thenReturn(MOVIE_EMBEDDING);
                }

                @Test
                void addsToDislikedClusters() {
                    swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                    verify(clusterService).addToClusters(eq(MOVIE_EMBEDDING), eq(state.getDislikedClusters()));
                }

                @Test
                void doesNotAddToLikedClusters() {
                    swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                    verify(clusterService, never()).addToClusters(any(), same(state.getClusters()));
                }

                @Test
                void fetchesEmbeddingByMovieId() {
                    swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

                    verify(movieService).getEmbeddingById(42L);
                }
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

                verify(clusterService, never()).addToClusters(any(), any());
            }
        }
    }

    @Nested
    class GetNextFeed {

        @Test
        void returnsNextMovieFromService() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.of(createMovieResponse(42L)));

            var response = swipeService.getNextFeed(session);

            assertThat(response).isPresent();
            assertThat(response.get().id()).isEqualTo(42L);
            assertThat(response.get().title()).isEqualTo("Title 42");
        }

        @Test
        void noMoreMovies_returnsEmpty() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.empty());

            var response = swipeService.getNextFeed(session);

            assertThat(response).isEmpty();
        }
    }

    @Nested
    class SwipeReturnsNextMovie {

        @Test
        void returnsNextUnseenMovieAfterSwipe() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.of(createMovieResponse(99L)));

            var result = swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(99L);
        }

        @Test
        void excludesJustSwipedMovieFromNext() {
            when(movieService.getUnseenMovie(any())).thenAnswer(invocation -> {
                Set<Long> excluded = invocation.getArgument(0);
                assertThat(excluded).contains(42L);
                return Optional.of(createMovieResponse(99L));
            });

            swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);
        }

        @Test
        void returnsEmptyWhenNoMovieAvailable() {
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.empty());

            var result = swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DislikeExploitation {

        private static final float[] CENTROID = {0.5f, 0.5f};
        private SwipeService spyService;

        @BeforeEach
        void setUp() {
            Cluster dislikedCluster = new Cluster();
            dislikedCluster.addMovieEmbedding(CENTROID);
            state.getDislikedClusters().add(dislikedCluster);
            spyService = spy(swipeService);
        }

        @Test
        void whenRandBelowDislikeExploitationRate_callsFindLeastSimilar() {
            doReturn(0.05, 0.0).when(spyService).nextRandom();
            when(movieService.findLeastSimilar(any(), anyInt(), any()))
                    .thenReturn(List.of(createMovieResponse(55L)));

            var result = spyService.getNextFeed(session);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(55L);
            verify(movieService).findLeastSimilar(any(), eq(1), any());
        }

        @Test
        void whenFindLeastSimilarReturnsEmpty_fallsBackToUnseen() {
            doReturn(0.05, 0.0).when(spyService).nextRandom();
            when(movieService.findLeastSimilar(any(), anyInt(), any())).thenReturn(List.of());
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.of(createMovieResponse(77L)));

            var result = spyService.getNextFeed(session);

            assertThat(result.get().id()).isEqualTo(77L);
            verify(movieService).getUnseenMovie(any());
        }

        @Test
        void whenRandAboveDislikeExploitationRate_skipsToUnseen() {
            doReturn(0.17).when(spyService).nextRandom();
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.empty());

            spyService.getNextFeed(session);

            verify(movieService, never()).findLeastSimilar(any(), anyInt(), any());
            verify(movieService).getUnseenMovie(any());
        }

        @Test
        void whenDislikedClustersEmpty_skipsDislikeExploitation() {
            state.getDislikedClusters().clear();
            doReturn(0.05).when(spyService).nextRandom();
            when(movieService.getUnseenMovie(any())).thenReturn(Optional.empty());

            spyService.getNextFeed(session);

            verify(movieService, never()).findLeastSimilar(any(), anyInt(), any());
        }
    }
}
