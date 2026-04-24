package com.kielakjr.movie_app.unit.cluster;

import com.kielakjr.movie_app.cluster.ClusterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClusterServiceTest {

    private ClusterService clusterService;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterService();
    }

    @Nested
    class UpdateUserCluster {

        @Test
        void returnsMovieEmbeddingWhenLikesCountIsZero() {
            float[] user = {4.0f, 8.0f};
            float[] movie = {2.0f, 6.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 0);

            assertThat(result).containsExactly(2.0f, 6.0f);
        }

        @Test
        void returnsMeanOfBothEmbeddingsWhenLikesCountIsOne() {
            float[] user = {2.0f, 4.0f};
            float[] movie = {4.0f, 8.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 1);

            assertThat(result[0]).isCloseTo(3.0f, within(0.001f));
            assertThat(result[1]).isCloseTo(6.0f, within(0.001f));
        }

        @Test
        void computesWeightedAverageCorrectly() {
            float[] user = {1.5f};
            float[] movie = {3.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 2);

            assertThat(result[0]).isCloseTo(2.0f, within(0.001f));
        }

        @Test
        void returnsNewArrayWithoutMutatingInputs() {
            float[] user = {1.0f, 2.0f};
            float[] movie = {3.0f, 4.0f};
            float[] userCopy = user.clone();
            float[] movieCopy = movie.clone();

            float[] result = clusterService.updateUserCluster(user, movie, 1);

            assertThat(result).isNotSameAs(user);
            assertThat(result).isNotSameAs(movie);
            assertThat(user).containsExactly(userCopy);
            assertThat(movie).containsExactly(movieCopy);
        }

        @Test
        void handlesMultiDimensionalEmbedding() {
            float[] user = {1.0f, 2.0f, 3.0f, 4.0f};
            float[] movie = {3.0f, 4.0f, 5.0f, 6.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 1);

            assertThat(result).hasSize(4);
            assertThat(result[0]).isCloseTo(2.0f, within(0.001f));
            assertThat(result[1]).isCloseTo(3.0f, within(0.001f));
            assertThat(result[2]).isCloseTo(4.0f, within(0.001f));
            assertThat(result[3]).isCloseTo(5.0f, within(0.001f));
        }

        @Test
        void handlesNegativeValues() {
            float[] user = {-2.0f};
            float[] movie = {2.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 1);

            assertThat(result[0]).isCloseTo(0.0f, within(0.001f));
        }

        @Test
        void handlesZeroEmbeddings() {
            float[] user = {0.0f, 0.0f};
            float[] movie = {0.0f, 0.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 5);

            assertThat(result).containsExactly(0.0f, 0.0f);
        }

        @Test
        void resultLengthMatchesInputLength() {
            float[] user = {1.0f, 2.0f, 3.0f};
            float[] movie = {4.0f, 5.0f, 6.0f};

            float[] result = clusterService.updateUserCluster(user, movie, 1);

            assertThat(result).hasSize(3);
        }

        @Test
        void nullUserEmbeddingReturnsMovieEmbeddingCopy() {
            float[] movie = {1.0f, 2.0f};

            float[] result = clusterService.updateUserCluster(null, movie, 1);

            assertThat(result).containsExactly(1.0f, 2.0f);
            assertThat(result).isNotSameAs(movie);
        }
    }
}
