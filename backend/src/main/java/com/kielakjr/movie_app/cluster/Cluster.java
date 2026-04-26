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
        int dims = movieEmbeddings.get(0).length;
        centroid = new float[dims];
        for (float[] embedding : movieEmbeddings) {
            for (int i = 0; i < dims; i++) {
                centroid[i] += embedding[i];
            }
        }
        for (int i = 0; i < dims; i++) {
            centroid[i] /= movieEmbeddings.size();
        }
    }
}
