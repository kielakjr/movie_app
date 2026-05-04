package com.kielakjr.movie_app.cluster;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class Cluster implements Serializable {
    private static final long serialVersionUID = 2L;

    private static final float RECENCY_DECAY = 0.9f;

    private List<Long> movieIds = new ArrayList<>();
    private float[] runningSum;
    private float weightSum = 0.0f;

    public void addMovie(Long movieId, float[] embedding) {
        movieIds.add(movieId);
        if (runningSum == null) {
            runningSum = new float[embedding.length];
        }
        for (int i = 0; i < embedding.length; i++) {
            runningSum[i] = runningSum[i] * RECENCY_DECAY + embedding[i];
        }
        weightSum = weightSum * RECENCY_DECAY + 1.0f;
    }

    public float[] getCentroid() {
        if (runningSum == null || weightSum == 0.0f) {
            return null;
        }
        float[] centroid = new float[runningSum.length];
        for (int i = 0; i < runningSum.length; i++) {
            centroid[i] = runningSum[i] / weightSum;
        }
        return centroid;
    }
}
