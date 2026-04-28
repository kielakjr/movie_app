import type { RecommendMovieResponse } from '../types';
import { StarIcon, XIcon } from './Icons';

interface RecommendationsModalProps {
  recommendations: RecommendMovieResponse[] | undefined;
  isLoading: boolean;
  onClose: () => void;
}

const RecommendationsModal = ({ recommendations, isLoading, onClose }: RecommendationsModalProps) => {
  return (
    <div className="recs-overlay" onClick={onClose}>
      <div className="recs-panel" onClick={e => e.stopPropagation()}>
        <div className="recs-header">
          <div>
            <h2 className="recs-title">Based on your taste</h2>
            <p className="recs-subtitle">Movies you might enjoy</p>
          </div>
          <button className="recs-close" onClick={onClose} aria-label="Close"><XIcon /></button>
        </div>

        <div className="recs-body">
          {isLoading && (
            <div className="recs-state">
              <div className="spinner" />
              <span>Finding your picks…</span>
            </div>
          )}

          {!isLoading && recommendations && recommendations.length === 0 && (
            <div className="recs-state">No recommendations yet — keep liking movies.</div>
          )}

          {!isLoading && recommendations && recommendations.length > 0 && (
            <div className="recs-grid">
              {recommendations.map(({ movie, reason }) => {
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
                        {rating && <span className="movie-card-rating"><StarIcon /> {rating}</span>}
                        {year && <span className="movie-card-year">{year}</span>}
                      </div>
                      <p className="recs-card-reason">Because you liked <strong>{reason.title}</strong></p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="recs-footer">
          <button className="recs-continue-btn" onClick={onClose}>Keep swiping</button>
        </div>
      </div>
    </div>
  );
};

export default RecommendationsModal;
