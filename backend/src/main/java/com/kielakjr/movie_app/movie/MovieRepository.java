package com.kielakjr.movie_app.movie;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTmdbId(Long tmdbId);

    Page<Movie> findAll(Pageable pageable);

    @Query("SELECT m.tmdbId FROM Movie m")
    Set<Long> findAllTmdbIds();

    @Query("SELECT m FROM Movie m WHERE m.id NOT IN :seenIds ORDER BY FUNCTION('random')")
    List<Movie> findUnseen(@Param("seenIds") Set<Long> seenIds, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE movies SET embedding = CAST(:embedding AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    @Query(value = "SELECT * FROM movies WHERE embedding IS NOT NULL ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Movie> findSimilar(@Param("embedding") String embedding, @Param("limit") int limit);

    @Query(value = "SELECT embedding FROM movies WHERE id = :id AND embedding IS NOT NULL", nativeQuery = true)
    float[] getEmbeddingById(Long id);
}
