package com.kielakjr.movie_app.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public Set<Long> findAllTmdbIds() {
        return movieRepository.findAllTmdbIds();
    }

    @Transactional
    public List<Movie> saveAll(List<Movie> movies) {
        return movieRepository.saveAll(movies);
    }

    @Transactional
    public void updateEmbedding(Long movieId, float[] embedding) {
        movieRepository.updateEmbedding(movieId, toVectorString(embedding));
    }

    @Transactional(readOnly = true)
    public List<Movie> findSimilar(float[] queryEmbedding, int limit) {
        return movieRepository.findSimilar(toVectorString(queryEmbedding), limit);
    }

    static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}
