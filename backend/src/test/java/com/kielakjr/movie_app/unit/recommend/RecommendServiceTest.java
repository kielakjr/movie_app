package com.kielakjr.movie_app.unit.recommend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import org.mockito.ArgumentMatchers;

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
import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.cluster.Cluster;
import org.mockito.InjectMocks;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;

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

    private MovieResponse createMovieResponse(Long id) {
        return new MovieResponse(id, 100L + id, "Title " + id, "Overview " + id, "2024-01-01", "en", false, "/poster" + id + ".jpg", "/backdrop" + id + ".jpg", new String[]{"Genre1", "Genre2"}, 10.0, 8.0, 100);
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
        void skipsMovieWhenNoReasonMovieFound() {
            Cluster cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{0.1f, 0.2f});
            state.getClusters().add(cluster);
            when(movieService.findSimilar(any(), anyInt(), any())).thenReturn(List.of(createMovieResponse(2L)));
            when(movieService.getEmbeddingsByIds(any())).thenReturn(Map.of(2L, new float[]{0.1f, 0.2f}));
            when(movieService.getMovieByEmbedding(any())).thenReturn(Optional.empty());
            var recommendations = recommendService.getRecommendedMovies(session, 10);
            assertThat(recommendations).isEmpty();
        }

        @Test
        void returnsRecommendations() {
            Cluster cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{0.1f, 0.2f});
            state.getClusters().add(cluster);
            when(movieService.getMovieByEmbedding(any())).thenReturn(Optional.of(createMovieResponse(1L)));
            when(movieService.findSimilar(any(), anyInt(), any())).thenReturn(List.of(createMovieResponse(2L)));
            when(movieService.getEmbeddingsByIds(any())).thenReturn(Map.of(2L, new float[]{0.1f, 0.2f}));
            var recommendations = recommendService.getRecommendedMovies(session, 10);
            verify(movieService).findSimilar(any(), eq(10), any());
            assertThat(recommendations).hasSize(1);
            assertThat(recommendations.get(0).movie().id()).isEqualTo(2L);
            assertThat(recommendations.get(0).reason().id()).isEqualTo(1L);
        }

        @Test
        void likedAndDislikedMovies_areBothExcludedFromRecommendations() {
            Cluster cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{0.1f, 0.2f});
            state.getClusters().add(cluster);
            state.getLikedMovieIds().add(10L);
            state.getDislikedMovieIds().add(20L);

            when(movieService.findSimilar(any(), anyInt(), any())).thenReturn(List.of());

            recommendService.getRecommendedMovies(session, 10);

            verify(movieService).findSimilar(any(), anyInt(), ArgumentMatchers.<Set<Long>>argThat(ids -> ids.contains(10L) && ids.contains(20L)));
        }

        @Test
        void popularityInfluencesOrdering_whenSimilarityIsTied() {
            Cluster cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{1.0f, 0.0f});
            state.getClusters().add(cluster);

            MovieResponse lowPop = new MovieResponse(1L, 101L, "Low", "ov", "2024-01-01", "en",
                    false, "/p1.jpg", "/b1.jpg", new String[]{}, 1.0, 8.0, 100);
            MovieResponse highPop = new MovieResponse(2L, 102L, "High", "ov", "2024-01-01", "en",
                    false, "/p2.jpg", "/b2.jpg", new String[]{}, 1000.0, 8.0, 100);

            when(movieService.findSimilar(any(), anyInt(), any())).thenReturn(List.of(lowPop, highPop));
            when(movieService.getEmbeddingsByIds(any())).thenReturn(Map.of(
                    1L, new float[]{1.0f, 0.0f},
                    2L, new float[]{1.0f, 0.0f}));
            when(movieService.getMovieByEmbedding(any())).thenReturn(Optional.of(lowPop));

            RecommendService spied = spy(recommendService);
            doReturn(0.0).when(spied).nextRandom();

            var recommendations = spied.getRecommendedMovies(session, 10);

            assertThat(recommendations).hasSize(2);
            assertThat(recommendations.get(0).movie().id()).isEqualTo(2L);
            assertThat(recommendations.get(1).movie().id()).isEqualTo(1L);
        }

        @Test
        void onlyDislikedMovies_areExcludedWhenNoLikes() {
            Cluster cluster = new Cluster();
            cluster.addMovieEmbedding(new float[]{0.1f, 0.2f});
            state.getClusters().add(cluster);
            state.getDislikedMovieIds().add(99L);

            when(movieService.findSimilar(any(), anyInt(), any())).thenReturn(List.of());

            recommendService.getRecommendedMovies(session, 10);

            verify(movieService).findSimilar(any(), anyInt(), ArgumentMatchers.<Set<Long>>argThat(ids -> ids.contains(99L)));
        }

    }
}
