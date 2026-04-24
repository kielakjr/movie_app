import type { Movie } from '../types';

interface RecommendationsModalProps {
  movies: Movie[] | undefined;
  isLoading: boolean;
  onClose: () => void;
}

const RecommendationsModal = ({ movies, isLoading, onClose }: RecommendationsModalProps) => {
  return (
    <div className="recs-overlay" onClick={onClose}>
      <div className="recs-panel" onClick={e => e.stopPropagation()}>
        <div className="recs-header">
          <div>
            <h2 className="recs-title">Based on your taste</h2>
            <p className="recs-subtitle">Movies you might enjoy</p>
          </div>
          <button className="recs-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        <div className="recs-body">
          {isLoading && (
            <div className="recs-state">Finding your picks...</div>
          )}

          {!isLoading && movies && movies.length === 0 && (
            <div className="recs-state">No recommendations yet — keep liking movies!</div>
          )}

          {!isLoading && movies && movies.length > 0 && (
            <div className="recs-grid">
              {movies.map(movie => {
                const year = movie.release_date?.slice(0, 4);
                const rating = movie.vote_average ? movie.vote_average.toFixed(1) : null;
                return (
                  <div key={movie.id} className="recs-card">
                    {movie.poster_path ? (
                      <img className="recs-card-poster" src={movie.poster_path} alt={movie.title} />
                    ) : (
                      <div className="recs-card-poster-placeholder">No poster</div>
                    )}
                    <div className="recs-card-body">
                      <p className="recs-card-title">{movie.title}</p>
                      <div className="movie-card-meta">
                        {rating && <span className="movie-card-rating">★ {rating}</span>}
                        {year && <span className="movie-card-year">{year}</span>}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="recs-footer">
          <button className="recs-continue-btn" onClick={onClose}>Keep Swiping</button>
        </div>
      </div>
    </div>
  );
};

export default RecommendationsModal;
