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

    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(m.overview) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "ORDER BY m.popularity DESC")
    List<Movie> searchByText(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE movies SET embedding = CAST(:embedding AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    @Query(value = "SELECT * FROM movies WHERE embedding IS NOT NULL ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Movie> findSimilar(@Param("embedding") String embedding, @Param("limit") int limit);

    @Query(value = "SELECT * FROM movies WHERE embedding IS NOT NULL AND id NOT IN :seenIds ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Movie> findSimilarExcluding(@Param("embedding") String embedding, @Param("limit") int limit, @Param("seenIds") Set<Long> seenIds);

@Query(value = "SELECT embedding::text FROM movies WHERE id = :id AND embedding IS NOT NULL", nativeQuery = true)
    String getEmbeddingByIdRaw(@Param("id") Long id);

    @Query(value = "SELECT id, embedding::text FROM movies WHERE id IN :ids AND embedding IS NOT NULL", nativeQuery = true)
    List<Object[]> getEmbeddingsByIds(@Param("ids") Set<Long> ids);

    @Query(value = "SELECT * FROM movies WHERE embedding IS NOT NULL ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT 1", nativeQuery = true)
    Optional<Movie> findByEmbedding(@Param("embedding") String embedding);
}
