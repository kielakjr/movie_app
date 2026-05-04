package com.kielakjr.movie_app.cluster;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClusterService {
    private static final float CLUSTER_DISTANCE_THRESHOLD = 0.6f;

    public void addToClusters(Long movieId, float[] embedding, List<Cluster> clusters) {
        Cluster bestCluster = findBestCluster(embedding, clusters);
        if (bestCluster == null || distanceToCluster(embedding, bestCluster) > CLUSTER_DISTANCE_THRESHOLD) {
            Cluster newCluster = new Cluster();
            newCluster.addMovie(movieId, embedding);
            clusters.add(newCluster);
        } else {
            bestCluster.addMovie(movieId, embedding);
        }
    }

    private Cluster findBestCluster(float[] embedding, List<Cluster> clusters) {
        Cluster bestCluster = null;
        float bestDistance = Float.MAX_VALUE;
        if (clusters.isEmpty()) {
            return null;
        }
        for (Cluster cluster : clusters) {
            float distance = distanceToCluster(embedding, cluster);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestCluster = cluster;
            }
        }
        return bestCluster;
    }

    private float distanceToCluster(float[] embedding, Cluster cluster) {
        if (cluster == null) {
            return Float.MAX_VALUE;
        }
        if (cluster.getCentroid() == null) {
            return Float.MAX_VALUE;
        }
        return cosineDistance(embedding, cluster.getCentroid());
    }

    public static float cosineSimilarity(float[] vecA, float[] vecB) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private float cosineDistance(float[] vecA, float[] vecB) {
        return 1.0f - cosineSimilarity(vecA, vecB);
    }
}
