import { useState, useRef } from 'react';
import { useNextSwipeMovie, usePeekMovie, useSwipe, useRecommendations } from '../hooks';
import type { Movie } from '../types';
import RecommendationsModal from '../components/RecommendationsModal';

const RECS_EVERY_N_LIKES = 5;
const DRAG_THRESHOLD = 80;
const MAX_ROTATION = 18;

const Swipe = () => {
  const { data: initialMovie, isLoading, error } = useNextSwipeMovie();
  const { mutate: swipe, isPending: swipePending } = useSwipe();
  const { data: recommendations, isFetching: recsFetching, refetch: fetchRecs } = useRecommendations(5);

  const [likesCount, setLikesCount] = useState(0);
  const [showRecs, setShowRecs] = useState(false);
  const [drag, setDrag] = useState({ x: 0, y: 0 });
  const [flying, setFlying] = useState<'left' | 'right' | 'skip' | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const [current, setCurrent] = useState<Movie | null | undefined>(undefined);
  const effectiveCurrent = current === undefined ? (initialMovie ?? null) : current;

  const { data: fetchedPeek, isFetching: peekFetching } = usePeekMovie(effectiveCurrent?.id, !swipePending);
  const peekIsTrustworthy = !swipePending && !peekFetching;
  const effectivePeek = peekIsTrustworthy && fetchedPeek?.id !== effectiveCurrent?.id
    ? (fetchedPeek ?? null)
    : null;

  const dragStart = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const pendingAction = useRef<{
    action: 'LIKE' | 'DISLIKE' | 'SKIP';
    movieId: number;
    nextMovie: Movie | null;
  } | null>(null);

  const canSwipe = !flying && !swipePending && peekIsTrustworthy;

  if (isLoading) return <div className="state-center">Loading...</div>;
  if (error || (!isLoading && !initialMovie)) return (
    <div className="state-center swipe-done">
      <span className="swipe-done-icon">🎬</span>
      <p>You've seen all movies!</p>
      <p className="swipe-done-sub">Check back later for new additions.</p>
    </div>
  );
  if (current === null) return (
    <div className="state-center swipe-done">
      <span className="swipe-done-icon">🎬</span>
      <p>You've seen all movies!</p>
      <p className="swipe-done-sub">Check back later for new additions.</p>
    </div>
  );
  if (!effectiveCurrent) return null;

  const year = effectiveCurrent.release_date?.slice(0, 4);
  const rating = effectiveCurrent.vote_average ? effectiveCurrent.vote_average.toFixed(1) : null;

  const doSwipe = (movieId: number, action: 'LIKE' | 'DISLIKE' | 'SKIP') => {
    swipe({ movieId, action }, {
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
    if (!canSwipe) return;
    pendingAction.current = {
      action,
      movieId: effectiveCurrent.id,
      nextMovie: effectivePeek,
    };
    setFlying(dir);
    setDrag({ x: 0, y: 0 });
  };

  const handleAnimationEnd = () => {
    const pending = pendingAction.current;
    pendingAction.current = null;
    setFlying(null);
    if (pending) {
      setCurrent(pending.nextMovie); // snapshot taken before swipe fired
      doSwipe(pending.movieId, pending.action);
    }
  };

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!canSwipe) return;
    dragStart.current = { x: e.clientX, y: e.clientY };
    setIsDragging(true);
    (e.currentTarget as HTMLDivElement).setPointerCapture(e.pointerId);
  };

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging) return;
    setDrag({
      x: e.clientX - dragStart.current.x,
      y: e.clientY - dragStart.current.y,
    });
  };

  const onPointerUp = () => {
    if (!isDragging) return;
    setIsDragging(false);
    if (Math.abs(drag.x) > DRAG_THRESHOLD) {
      launchCard(drag.x > 0 ? 'right' : 'left', drag.x > 0 ? 'LIKE' : 'DISLIKE');
    } else {
      setDrag({ x: 0, y: 0 });
    }
  };

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
        <div className="swipe-stack">
          {effectivePeek && (
            <div className="swipe-card swipe-card-peek" aria-hidden>
              {effectivePeek.poster_path ? (
                <img className="swipe-poster" src={effectivePeek.poster_path} alt={effectivePeek.title} draggable={false} />
              ) : (
                <div className="swipe-poster-placeholder" />
              )}
            </div>
          )}

          <div
            className={`swipe-card${flying ? ` swipe-fly-${flying}` : ''}`}
            style={cardStyle}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onAnimationEnd={handleAnimationEnd}
          >
            {effectiveCurrent.poster_path ? (
              <img className="swipe-poster" src={effectiveCurrent.poster_path} alt={effectiveCurrent.title} draggable={false} />
            ) : (
              <div className="swipe-poster-placeholder">No poster</div>
            )}

            <div className="swipe-overlay">
              <h2 className="swipe-title">{effectiveCurrent.title}</h2>
              <div className="swipe-meta">
                {rating && <span className="movie-card-rating">★ {rating}</span>}
                {year && <span className="movie-card-year">{year}</span>}
                {effectiveCurrent.original_language && (
                  <span className="swipe-lang">{effectiveCurrent.original_language.toUpperCase()}</span>
                )}
              </div>
              {effectiveCurrent.genres && effectiveCurrent.genres.length > 0 && (
                <div className="movie-card-genres">
                  {effectiveCurrent.genres.slice(0, 3).map(genre => (
                    <span key={genre} className="genre-tag">{genre}</span>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="swipe-actions">
          <button
            className="swipe-btn swipe-btn-dislike"
            onClick={() => launchCard('left', 'DISLIKE')}
            disabled={!canSwipe}
            title="Dislike"
          >
            ✕
          </button>
          <button
            className="swipe-btn swipe-btn-skip"
            onClick={() => launchCard('skip', 'SKIP')}
            disabled={!canSwipe}
            title="Skip"
          >
            →
          </button>
          <button
            className="swipe-btn swipe-btn-like"
            onClick={() => launchCard('right', 'LIKE')}
            disabled={!canSwipe}
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
