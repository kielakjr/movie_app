import { useMovies } from '../hooks';
import { useState } from 'react';
import MovieCard from '../components/MovieCard';

const Movies = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useMovies(page, 10);

  if (isLoading) return <div className="state-center">Loading movies...</div>;
  if (error) return <div className="state-center">Failed to load movies.</div>;
  if (!data) return <div className="state-center">No movies found.</div>;

  return (
    <div className="page">
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
    </div>
  );
};

export default Movies;
