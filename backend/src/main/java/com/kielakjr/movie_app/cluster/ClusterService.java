package com.kielakjr.movie_app.cluster;

import org.springframework.stereotype.Service;

@Service
public class ClusterService {

    public float[] updateUserCluster(float[] userEmbedding, float[] movieEmbedding, int likesCount) {
        if (userEmbedding == null) {
            return movieEmbedding.clone();
        }
        float[] updatedEmbedding = new float[userEmbedding.length];
        for (int i = 0; i < userEmbedding.length; i++) {
            updatedEmbedding[i] = (userEmbedding[i] * likesCount + movieEmbedding[i]) / (likesCount + 1);
        }
        return updatedEmbedding;
    }
}
