import { useState, useRef } from 'react';
import { useNextSwipeMovie, useSwipe, useRecommendations } from '../hooks';
import RecommendationsModal from '../components/RecommendationsModal';

const RECS_EVERY_N_LIKES = 5;
const DRAG_THRESHOLD = 80;
const MAX_ROTATION = 18;

const Swipe = () => {
  const { data: movie, isLoading, error } = useNextSwipeMovie();
  const { mutate: swipe, isPending } = useSwipe();
  const { data: recommendations, isFetching: recsFetching, refetch: fetchRecs } = useRecommendations(5);

  const [likesCount, setLikesCount] = useState(0);
  const [showRecs, setShowRecs] = useState(false);
  const [drag, setDrag] = useState({ x: 0, y: 0 });
  const [flying, setFlying] = useState<'left' | 'right' | 'skip' | null>(null);
  const dragStart = useRef<{ x: number; y: number; active: boolean }>({ x: 0, y: 0, active: false });
  const pendingAction = useRef<'LIKE' | 'DISLIKE' | 'SKIP' | null>(null);

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

  const doSwipe = (action: 'LIKE' | 'DISLIKE' | 'SKIP') => {
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

  const launchCard = (dir: 'left' | 'right' | 'skip', action: 'LIKE' | 'DISLIKE' | 'SKIP') => {
    if (flying || isPending) return;
    pendingAction.current = action;
    setFlying(dir);
    setDrag({ x: 0, y: 0 });
  };

  const handleAnimationEnd = () => {
    const action = pendingAction.current;
    setFlying(null);
    if (action) {
      pendingAction.current = null;
      doSwipe(action);
    }
  };

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (flying || isPending) return;
    dragStart.current = { x: e.clientX, y: e.clientY, active: true };
    (e.currentTarget as HTMLDivElement).setPointerCapture(e.pointerId);
  };

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragStart.current.active) return;
    setDrag({
      x: e.clientX - dragStart.current.x,
      y: e.clientY - dragStart.current.y,
    });
  };

  const onPointerUp = () => {
    if (!dragStart.current.active) return;
    dragStart.current.active = false;
    if (Math.abs(drag.x) > DRAG_THRESHOLD) {
      launchCard(drag.x > 0 ? 'right' : 'left', drag.x > 0 ? 'LIKE' : 'DISLIKE');
    } else {
      setDrag({ x: 0, y: 0 });
    }
  };

  const isDragging = dragStart.current.active;
  const rotation = flying ? 0 : (drag.x / 200) * MAX_ROTATION;

  const cardStyle = flying ? {} : {
    transform: drag.x !== 0 || drag.y !== 0
      ? `translate(${drag.x}px, ${drag.y * 0.3}px) rotate(${rotation}deg)`
      : undefined,
    transition: !isDragging ? 'transform 0.3s ease' : 'none',
    cursor: isDragging ? 'grabbing' : 'grab',
  };

  return (
    <>
      <div className="swipe-page">
        <div
          className={`swipe-card${flying ? ` swipe-fly-${flying}` : ''}`}
          style={cardStyle}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
          onAnimationEnd={handleAnimationEnd}
        >
          {movie.poster_path ? (
            <img className="swipe-poster" src={movie.poster_path} alt={movie.title} draggable={false} />
          ) : (
            <div className="swipe-poster-placeholder">No poster</div>
          )}

          <div className="swipe-overlay">
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
                {movie.genres.slice(0, 3).map(genre => (
                  <span key={genre} className="genre-tag">{genre}</span>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="swipe-actions">
          <button
            className="swipe-btn swipe-btn-dislike"
            onClick={() => launchCard('left', 'DISLIKE')}
            disabled={isPending || !!flying}
            title="Dislike"
          >
            ✕
          </button>
          <button
            className="swipe-btn swipe-btn-skip"
            onClick={() => launchCard('skip', 'SKIP')}
            disabled={isPending || !!flying}
            title="Skip"
          >
            →
          </button>
          <button
            className="swipe-btn swipe-btn-like"
            onClick={() => launchCard('right', 'LIKE')}
            disabled={isPending || !!flying}
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
