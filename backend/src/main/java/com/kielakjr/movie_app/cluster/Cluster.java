package com.kielakjr.movie_app.cluster;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

@Component
@Getter
@NoArgsConstructor
@Slf4j
public class Cluster {
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
