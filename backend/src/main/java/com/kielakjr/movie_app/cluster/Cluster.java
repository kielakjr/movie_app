package com.kielakjr.movie_app.cluster;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
public class Cluster implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final float RECENCY_DECAY = 0.9f;

    private List<float[]> movieEmbeddings = new ArrayList<>();
    private float[] centroid;

    public void addMovieEmbedding(float[] embedding) {
        movieEmbeddings.add(embedding);
        updateCentroid();
    }

    private void updateCentroid() {
        if (movieEmbeddings.isEmpty()) {
            centroid = null;
            return;
        }
        int n = movieEmbeddings.size();
        int dims = movieEmbeddings.get(0).length;
        centroid = new float[dims];
        float weightSum = 0.0f;
        for (int idx = 0; idx < n; idx++) {
            float weight = (float) Math.pow(RECENCY_DECAY, n - 1 - idx);
            float[] embedding = movieEmbeddings.get(idx);
            for (int i = 0; i < dims; i++) {
                centroid[i] += embedding[i] * weight;
            }
            weightSum += weight;
        }
        for (int i = 0; i < dims; i++) {
            centroid[i] /= weightSum;
        }
    }
}
