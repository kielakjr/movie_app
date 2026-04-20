package com.kielakjr.movie_app.seed;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.tmdb.TmdbClient;
import com.kielakjr.movie_app.tmdb.TmdbImageUrlBuilder;
import com.kielakjr.movie_app.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.kielakjr.movie_app.tmdb.dto.TmdbPopularResponse.TmdbMovie;
import com.kielakjr.movie_app.movie.Movie;
import com.kielakjr.movie_app.movie.MovieService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {
    private final TmdbClient tmdbClient;
    private final TmdbImageUrlBuilder imageUrlBuilder;
    private final MovieService movieService;
    private final EmbeddingClient embeddingClient;

    public int seedPopularMovies(int pages) {
        if (pages < 1 || pages > 500)
            throw new IllegalArgumentException("pages must be between 1 and 500");

        Map<Integer, String> genreMap = tmdbClient.getGenres().genres().stream()
                .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));

        List<TmdbMovie> fetched = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            var results = tmdbClient.getPopularMovies(page).results();
            if (results != null) fetched.addAll(results);
        }

        var existingIds = movieService.findAllTmdbIds();
        List<Movie> toSave = fetched.stream()
                .filter(m -> !existingIds.contains(m.id()))
                .map(m -> buildMovie(m, genreMap))
                .toList();

        if (toSave.isEmpty()) {
            log.info("No new movies to seed");
            return 0;
        }

        List<Movie> saved = movieService.saveAll(toSave);

        for (Movie movie : saved) {
            String text = movie.getTitle() + ". " + (movie.getOverview() != null ? movie.getOverview() : "");
            float[] embedding = embeddingClient.embed(text);
            if (embedding.length > 0) {
                movieService.updateEmbedding(movie.getId(), embedding);
            }
        }

        log.info("Seeded {} new movies", saved.size());
        return saved.size();
    }

    private Movie buildMovie(TmdbMovie m, Map<Integer, String> genreMap) {
        return Movie.builder()
                .tmdbId(m.id())
                .title(m.title())
                .overview(m.overview())
                .releaseDate(parseReleaseDate(m.release_date()))
                .originalLanguage(m.original_language())
                .adult(m.adult())
                .posterPath(imageUrlBuilder.posterUrl(m.poster_path()))
                .backdropPath(imageUrlBuilder.backdropUrl(m.backdrop_path()))
                .genres(m.genre_ids().stream().map(genreMap::get).filter(Objects::nonNull).toList())
                .popularity(m.popularity())
                .voteAverage(m.vote_average())
                .voteCount(m.vote_count())
                .build();
    }

    private LocalDate parseReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) return null;
        try {
            return LocalDate.parse(releaseDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
