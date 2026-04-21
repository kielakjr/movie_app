package com.kielakjr.movie_app.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable).map(MovieService::toMovieResponse);
    }

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

    private static MovieResponse toMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .tmdbId(movie.getTmdbId())
                .title(movie.getTitle())
                .overview(movie.getOverview())
                .releaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null)
                .originalLanguage(movie.getOriginalLanguage())
                .adult(movie.isAdult())
                .posterPath(movie.getPosterPath())
                .backdropPath(movie.getBackdropPath())
                .genres(movie.getGenres() != null ? movie.getGenres().toArray(new String[0]) : new String[0])
                .popularity(movie.getPopularity())
                .voteAverage(movie.getVoteAverage())
                .voteCount(movie.getVoteCount())
                .build();
    }
}
