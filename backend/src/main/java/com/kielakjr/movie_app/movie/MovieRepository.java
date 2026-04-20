package com.kielakjr.movie_app.movie;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTmdbId(Long tmdbId);

    @Query("SELECT m.tmdbId FROM Movie m")
    Set<Long> findAllTmdbIds();
}
