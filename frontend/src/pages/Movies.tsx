import { useDebounce, useMovies, useSearchSimilarMovies } from '../hooks';
import { useState } from 'react';
import MovieCard from '../components/MovieCard';

const Movies = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useMovies(page, 10);
  const [searchQuery, setSearchQuery, debouncedQuery] = useDebounce<string>('', 500);
  const { data: similarMovies, isLoading: isSearching } = useSearchSimilarMovies(debouncedQuery);

  if (isLoading) return <div className="state-center">Loading movies...</div>;
  if (error) return <div className="state-center">Failed to load movies.</div>;
  if (!data) return <div className="state-center">No movies found.</div>;

  const isSearchActive = debouncedQuery.trim().length > 0;

  return (
    <div className="page">
      <div className="search-bar">
        <input
          type="text"
          placeholder="Search similar movies..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="search-input"
        />
      </div>

      {isSearchActive ? (
        <div className="similar-movies">
          {isSearching ? (
            <div className="state-center">Searching...</div>
          ) : similarMovies && similarMovies.length > 0 ? (
            <>
              <h2 className="section-title">Results for "{debouncedQuery}"</h2>
              <div className="movie-grid">
                {similarMovies.map(movie => (
                  <MovieCard key={movie.id} movie={movie} />
                ))}
              </div>
            </>
          ) : (
            <div className="state-center">No results found.</div>
          )}
        </div>
      ) : (
        <>
          <div className="movie-grid">
            {data.content.map(movie => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
          <div className="pagination">
            <button
              className="pagination-btn"
              onClick={() => setPage(prev => Math.max(prev - 1, 0))}
              disabled={page === 0}
            >
              Previous
            </button>
            <span className="pagination-info">Page {page + 1} of {data.page.total_pages}</span>
            <button
              className="pagination-btn"
              onClick={() => setPage(prev => prev + 1)}
              disabled={page >= data.page.total_pages - 1}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default Movies;
