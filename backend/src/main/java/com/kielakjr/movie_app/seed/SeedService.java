package com.kielakjr.movie_app.seed;

import com.kielakjr.movie_app.embedding.EmbeddingClient;
import com.kielakjr.movie_app.tmdb.TmdbClient;
import com.kielakjr.movie_app.tmdb.TmdbImageUrlBuilder;
import com.kielakjr.movie_app.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.kielakjr.movie_app.tmdb.dto.TmdbKeywordsResponse;
import com.kielakjr.movie_app.tmdb.dto.TmdbMovieResponse;
import com.kielakjr.movie_app.tmdb.dto.TmdbMovieResponse.TmdbMovie;
import com.kielakjr.movie_app.movie.Movie;
import com.kielakjr.movie_app.movie.MovieService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {

    private final TmdbClient tmdbClient;
    private final TmdbImageUrlBuilder imageUrlBuilder;
    private final MovieService movieService;
    private final EmbeddingClient embeddingClient;
    private final Clock clock;

    public int seedPopularMovies(int pages) {
        return seedMovies(pages, tmdbClient::getPopularMovies);
    }

    public int seedTopRatedMovies(int pages) {
        return seedMovies(pages, tmdbClient::getTopRatedMovies);
    }

    private int seedMovies(int pages, IntFunction<TmdbMovieResponse> fetcher) {

        Map<Integer, String> genreMap = tmdbClient.getGenres().genres().stream()
                .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));

        List<TmdbMovie> fetched = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            var response = fetcher.apply(page);
            if (response != null && response.results() != null) {
                fetched.addAll(response.results());
            }
        }

        Map<Long, TmdbMovie> uniqueFetched = fetched.stream()
                .collect(Collectors.toMap(
                        TmdbMovie::id,
                        Function.identity(),
                        (existing, duplicate) -> existing
                ));

        Set<Long> existingIds = movieService.findAllTmdbIds();
        LocalDate today = LocalDate.now(clock);

        List<Movie> toSave = uniqueFetched.values().stream()
                .filter(m -> !existingIds.contains(m.id()))
                .filter(m -> !isUnreleased(m.release_date(), today))
                .map(m -> buildMovie(m, genreMap))
                .toList();

        if (toSave.isEmpty()) {
            log.info("No new movies to seed");
            return 0;
        }

        log.info(
                "Existing movies: {}, Fetched: {}, Unique: {}, To save: {}",
                existingIds.size(),
                fetched.size(),
                uniqueFetched.size(),
                toSave.size()
        );

        List<Movie> saved = movieService.saveAll(toSave);

        List<String> texts = new ArrayList<>(saved.size());
        for (Movie movie : saved) {
            var keywordsResponse = tmdbClient.getKeywords(movie.getTmdbId());
            var keywords = Arrays.stream(keywordsResponse.keywords())
                    .map(TmdbKeywordsResponse.TmdbKeyword::name)
                    .toList();
            String text = movie.getTitle() + ". "
                    + String.join(", ", movie.getGenres()) + ". "
                    + (movie.getOverview() != null ? movie.getOverview() : "")
                    + " Keywords: " + String.join(", ", keywords);
            texts.add(text);
        }

        List<float[]> batchEmbeddings = embeddingClient.embedBatch(texts);
        Map<Long, float[]> embeddings = new HashMap<>();
        int matchCount = Math.min(saved.size(), batchEmbeddings.size());
        if (batchEmbeddings.size() != saved.size()) {
            log.warn("Batch embedding size mismatch: expected {}, got {} — processing {} entries", saved.size(), batchEmbeddings.size(), matchCount);
        }
        for (int i = 0; i < matchCount; i++) {
            float[] embedding = batchEmbeddings.get(i);
            if (embedding.length > 0) {
                embeddings.put(saved.get(i).getId(), embedding);
            }
        }
        int skipped = saved.size() - embeddings.size();
        if (skipped > 0) {
            log.warn("{} movies saved without embeddings — they will not appear in recommendations or search", skipped);
        }

        if (!embeddings.isEmpty()) {
            movieService.batchUpdateEmbeddings(embeddings);
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
                .genres(m.genre_ids().stream()
                        .map(genreMap::get)
                        .filter(Objects::nonNull)
                        .toList())
                .popularity(m.popularity())
                .voteAverage(m.vote_average())
                .voteCount(m.vote_count())
                .build();
    }

    private boolean isUnreleased(String releaseDate, LocalDate today) {
        LocalDate parsed = parseReleaseDate(releaseDate);
        return parsed != null && parsed.isAfter(today);
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
