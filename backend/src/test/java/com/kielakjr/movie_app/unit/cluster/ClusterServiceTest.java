package com.kielakjr.movie_app.unit.cluster;

import com.kielakjr.movie_app.cluster.ClusterService;
import com.kielakjr.movie_app.cluster.Cluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.util.ArrayList;
import java.util.List;

class ClusterServiceTest {

    private ClusterService clusterService;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterService();
    }

    @Nested
    class AddToClustersTests {
        @Test
        void shouldAddToExistingCluster() {
            Cluster cluster = new Cluster();
            float[] embedding1 = {0.1f, 0.2f, 0.3f};
            float[] embedding2 = {0.11f, 0.21f, 0.31f};
            cluster.addMovieEmbedding(embedding1);

            clusterService.addToClusters(embedding2, List.of(cluster));

            assertThat(cluster.getMovieEmbeddings()).hasSize(2);
            assertThat(cluster.getCentroid()[0]).isCloseTo(0.105f, within(0.001f));
            assertThat(cluster.getCentroid()[1]).isCloseTo(0.205f, within(0.001f));
            assertThat(cluster.getCentroid()[2]).isCloseTo(0.305f, within(0.001f));
        }

        @Test
        void shouldCreateNewCluster() {
            Cluster cluster = new Cluster();
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.0f, 1.0f, 0.0f};
            cluster.addMovieEmbedding(embedding1);

            List<Cluster> clusters = new ArrayList<>(List.of(cluster));
            clusterService.addToClusters(embedding2, clusters);

            assertThat(clusters).hasSize(2);
            assertThat(cluster.getMovieEmbeddings()).hasSize(1);
            assertThat(cluster.getCentroid()[0]).isCloseTo(1.0f, within(0.001f));
            assertThat(cluster.getCentroid()[1]).isCloseTo(0.0f, within(0.001f));
            assertThat(cluster.getCentroid()[2]).isCloseTo(0.0f, within(0.001f));
        }


    }
}
