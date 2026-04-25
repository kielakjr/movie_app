import { useState } from 'react';
import { useNextSwipeMovie, useSwipe, useRecommendations } from '../hooks';
import RecommendationsModal from '../components/RecommendationsModal';

const RECS_EVERY_N_LIKES = 5;

const Swipe = () => {
  const { data: movie, isLoading, error } = useNextSwipeMovie();
  const { mutate: swipe, isPending } = useSwipe();
  const { data: recommendations, isFetching: recsFetching, refetch: fetchRecs } = useRecommendations(5);

  const [likesCount, setLikesCount] = useState(0);
  const [showRecs, setShowRecs] = useState(false);

  if (isLoading) return <div className="state-center">Loading...</div>;
  if (error) return (
    <div className="state-center swipe-done">
      <span className="swipe-done-icon">🎬</span>
      <p>You've seen all movies!</p>
      <p className="swipe-done-sub">Check back later for new additions.</p>
    </div>
  );
  if (!movie) return null;

  const year = movie.release_date?.slice(0, 4);
  const rating = movie.vote_average ? movie.vote_average.toFixed(1) : null;

  const handleSwipe = (action: 'LIKE' | 'DISLIKE' | 'SKIP') => {
    swipe({ movieId: movie.id, action }, {
      onSuccess: () => {
        if (action === 'LIKE') {
          const next = likesCount + 1;
          setLikesCount(next);
          if (next % RECS_EVERY_N_LIKES === 0) {
            fetchRecs();
            setShowRecs(true);
          }
        }
      },
    });
  };

  return (
    <>
      <div className="swipe-page">
        <div className="swipe-card">
          <div className="swipe-poster-wrap">
            {movie.poster_path ? (
              <img className="swipe-poster" src={movie.poster_path} alt={movie.title} />
            ) : (
              <div className="swipe-poster-placeholder">No poster</div>
            )}
          </div>

          <div className="swipe-info">
            <h2 className="swipe-title">{movie.title}</h2>

            <div className="swipe-meta">
              {rating && <span className="movie-card-rating">★ {rating}</span>}
              {year && <span className="movie-card-year">{year}</span>}
              {movie.original_language && (
                <span className="swipe-lang">{movie.original_language.toUpperCase()}</span>
              )}
            </div>

            {movie.genres && movie.genres.length > 0 && (
              <div className="movie-card-genres">
                {movie.genres.map(genre => (
                  <span key={genre} className="genre-tag">{genre}</span>
                ))}
              </div>
            )}

            {movie.overview && (
              <p className="swipe-overview">{movie.overview}</p>
            )}
          </div>
        </div>

        <div className="swipe-actions">
          <button
            className="swipe-btn swipe-btn-dislike"
            onClick={() => handleSwipe('DISLIKE')}
            disabled={isPending}
            title="Dislike"
          >
            ✕
          </button>
          <button
            className="swipe-btn swipe-btn-skip"
            onClick={() => handleSwipe('SKIP')}
            disabled={isPending}
            title="Skip"
          >
            →
          </button>
          <button
            className="swipe-btn swipe-btn-like"
            onClick={() => handleSwipe('LIKE')}
            disabled={isPending}
            title="Like"
          >
            ♥
          </button>
        </div>
      </div>

      {showRecs && (
        <RecommendationsModal
          recommendations={recommendations}
          isLoading={recsFetching}
          onClose={() => setShowRecs(false)}
        />
      )}
    </>
  );
};

export default Swipe;
