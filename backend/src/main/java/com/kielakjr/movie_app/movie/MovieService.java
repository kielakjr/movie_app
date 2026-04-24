package com.kielakjr.movie_app.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.movie_app.movie.dto.MovieResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
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

    @Transactional
    public void batchUpdateEmbeddings(Map<Long, float[]> updates) {
        updates.forEach((id, embedding) ->
                movieRepository.updateEmbedding(id, toVectorString(embedding)));
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> findSimilar(float[] queryEmbedding, int limit) {
        return movieRepository.findSimilar(toVectorString(queryEmbedding), limit).stream()
                .map(MovieService::toMovieResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MovieResponse> getUnseenMovie(Set<Long> seenIds) {
        return movieRepository.findUnseen(seenIds, PageRequest.of(0, 1))
                .stream().findFirst().map(MovieService::toMovieResponse);
    }

    @Transactional(readOnly = true)
    public float[] getEmbeddingById(Long id) {
        String raw = movieRepository.getEmbeddingByIdRaw(id);
        if (raw == null) return null;
        String[] parts = raw.substring(1, raw.length() - 1).split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    public static String toVectorString(float[] embedding) {
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
