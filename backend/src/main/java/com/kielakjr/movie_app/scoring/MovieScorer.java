package com.kielakjr.movie_app.scoring;

import com.kielakjr.movie_app.movie.dto.MovieResponse;

import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public final class MovieScorer {
    private static final double W_SIM = 0.55;
    private static final double W_POP = 0.30;
    private static final double W_NOISE = 0.15;

    public static double score(double similarity, double popNorm, double noise) {
        return W_SIM * similarity + W_POP * popNorm + W_NOISE * noise;
    }

    public static double maxLogPopularity(List<MovieResponse> movies) {
        double max = 0.0;
        for (MovieResponse m : movies) {
            max = Math.max(max, Math.log1p(safePopularity(m)));
        }
        return max;
    }

    public static double normalizedPopularity(MovieResponse movie, double maxLogPop) {
        if (maxLogPop <= 0.0) return 0.0;
        return Math.log1p(safePopularity(movie)) / maxLogPop;
    }

    public static double safePopularity(MovieResponse movie) {
        Double pop = movie.popularity();
        if (pop == null || pop < 0.0) return 0.0;
        return pop;
    }
}
