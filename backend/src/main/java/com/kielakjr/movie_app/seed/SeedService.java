package com.kielakjr.movie_app.seed;

import com.kielakjr.movie_app.tmdb.TmdbClient;
import com.kielakjr.movie_app.tmdb.TmdbImageUrlBuilder;
import com.kielakjr.movie_app.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.kielakjr.movie_app.tmdb.dto.TmdbPopularResponse.TmdbMovie;
import com.kielakjr.movie_app.movie.Movie;
import com.kielakjr.movie_app.movie.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeedService {

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final TmdbImageUrlBuilder imageUrlBuilder;

    public int seedPopularMovies(int pages) {
        if (pages < 1 || pages > 500) {
            throw new IllegalArgumentException("pages must be between 1 and 500");
        }

        Map<Integer, String> genreMap = tmdbClient.getGenres().genres().stream()
                .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));

        List<TmdbMovie> fetched = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            var results = tmdbClient.getPopularMovies(page).results();
            if (results != null) fetched.addAll(results);
        }

        int count = persist(fetched, genreMap);
        log.info("Seeded {} new movies ({} pages fetched)", count, pages);
        return count;
    }

    @Transactional
    protected int persist(List<TmdbMovie> tmdbMovies, Map<Integer, String> genreMap) {
        Set<Long> existingIds = movieRepository.findAllTmdbIds();
        int count = 0;
        for (var tmdbMovie : tmdbMovies) {
            if (existingIds.contains(tmdbMovie.id())) continue;
            movieRepository.save(Movie.builder()
                    .tmdbId(tmdbMovie.id())
                    .title(tmdbMovie.title())
                    .overview(tmdbMovie.overview())
                    .releaseDate(parseReleaseDate(tmdbMovie.release_date()))
                    .originalLanguage(tmdbMovie.original_language())
                    .adult(tmdbMovie.adult())
                    .posterPath(imageUrlBuilder.posterUrl(tmdbMovie.poster_path()))
                    .backdropPath(imageUrlBuilder.backdropUrl(tmdbMovie.backdrop_path()))
                    .genres(tmdbMovie.genre_ids().stream()
                            .map(genreMap::get)
                            .filter(Objects::nonNull)
                            .toList())
                    .popularity(tmdbMovie.popularity())
                    .voteAverage(tmdbMovie.vote_average())
                    .voteCount(tmdbMovie.vote_count())
                    .build());
            count++;
        }
        return count;
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
