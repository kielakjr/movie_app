package com.kielakjr.movie_app.swipe;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import com.kielakjr.movie_app.movie.MovieService;
import com.kielakjr.movie_app.session.SessionService;
import com.kielakjr.movie_app.session.SwipeSessionState;
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
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private MovieService movieService;

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

        @Test
        void like_addsMovieToSeenIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

            assertThat(state.getSeenMovieIds()).containsExactly(42L);
        }

        @Test
        void like_incrementsLikesCount() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

            assertThat(state.getLikesCount()).isEqualTo(1);
        }

        @Test
        void like_doesNotAddToDislikedIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.LIKE), session);

            assertThat(state.getDislikedMovieIds()).isEmpty();
        }

        @Test
        void dislike_addsMovieToDislikedIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

            assertThat(state.getDislikedMovieIds()).containsExactly(42L);
        }

        @Test
        void dislike_doesNotIncrementLikesCount() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

            assertThat(state.getLikesCount()).isEqualTo(0);
        }

        @Test
        void dislike_doesAddToSeenIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.DISLIKE), session);

            assertThat(state.getSeenMovieIds()).containsExactly(42L);
        }

        @Test
        void skip_addsMovieToSeenIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

            assertThat(state.getSeenMovieIds()).containsExactly(42L);
        }

        @Test
        void skip_doesNotIncrementLikesCount() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

            assertThat(state.getLikesCount()).isEqualTo(0);
        }

        @Test
        void skip_doesNotAddToDislikedIds() {
            swipeService.swipe(new SwipeRequest(42L, SwipeAction.SKIP), session);

            assertThat(state.getDislikedMovieIds()).isEmpty();
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
    class GetNextFeed {
        @Test
        void returnsNextMovieFromService() {
            when(movieService.getNextMovie(any())).thenReturn(Optional.of(createMovieResponse(42L)));

            var response = swipeService.getNextFeed(session);

            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.title()).isEqualTo("Title 42");
        }

        @Test
        void noMoreMovies_throwsException() {
            when(movieService.getNextMovie(any())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> swipeService.getNextFeed(session));
        }
    }
}
