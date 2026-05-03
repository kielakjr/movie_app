import { useState, useRef } from 'react';
import { useNextSwipeMovie, useSwipe, useRecommendations, useResetSession } from '../hooks';
import type { Movie } from '../types';
import RecommendationsModal from '../components/RecommendationsModal';
import { FilmIcon, HeartIcon, SkipIcon, StarIcon, XIcon } from '../components/Icons';

const RECS_EVERY_N_LIKES = 5;
const DRAG_THRESHOLD = 80;
const MAX_ROTATION = 14;

const Swipe = () => {
  const { data: initialMovie, isLoading, error } = useNextSwipeMovie();
  const [swipeError, setSwipeError] = useState<string | null>(null);
  const { mutate: swipe, isPending: swipePending } = useSwipe(() => {
    setFlying(null);
    setDrag({ x: 0, y: 0 });
    setSwipeError('Failed to record swipe. Please try again.');
  });
  const { data: recommendations, isFetching: recsFetching, refetch: fetchRecs } = useRecommendations(5);
  const { mutate: resetSession, isPending: resetPending } = useResetSession();

  const [likesCount, setLikesCount] = useState(0);
  const [showRecs, setShowRecs] = useState(false);
  const [drag, setDrag] = useState({ x: 0, y: 0 });
  const [flying, setFlying] = useState<'left' | 'right' | 'skip' | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const [current, setCurrent] = useState<Movie | null | undefined>(undefined);
  const effectiveCurrent = current === undefined ? (initialMovie ?? null) : current;

  const dragStart = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const animationDoneRef = useRef(false);
  const pendingNextRef = useRef<{ next: Movie | null } | null>(null);

  const canSwipe = !flying && !swipePending && !resetPending;

  const handleReset = () => {
    if (resetPending) return;
    resetSession(undefined, {
      onSuccess: () => {
        setCurrent(undefined);
        setLikesCount(0);
        setShowRecs(false);
        setDrag({ x: 0, y: 0 });
        setFlying(null);
      },
    });
  };

  const resetButton = (
    <button
      className="swipe-reset-btn"
      onClick={handleReset}
      disabled={resetPending}
      title="Reset session"
    >
      {resetPending ? 'Resetting…' : 'Reset session'}
    </button>
  );

  const doneState = (
    <div className="state-center swipe-done">
      <span className="swipe-done-icon"><FilmIcon /></span>
      <p>You've seen all movies</p>
      <p className="swipe-done-sub">Check back later for new additions.</p>
      {resetButton}
    </div>
  );

  if (isLoading) return (
    <div className="state-center">
      <div className="spinner" />
      <span>Loading…</span>
    </div>
  );
  if (error || (!isLoading && !initialMovie)) return doneState;
  if (current === null) return doneState;
  if (!effectiveCurrent) return null;

  const year = effectiveCurrent.release_date?.slice(0, 4);
  const rating = effectiveCurrent.vote_average ? effectiveCurrent.vote_average.toFixed(1) : null;

  const commitNext = (nextMovie: Movie | null) => {
    setCurrent(nextMovie);
    setFlying(null);
  };

  const launchCard = (dir: 'left' | 'right' | 'skip', action: 'LIKE' | 'DISLIKE' | 'SKIP') => {
    if (!canSwipe) return;
    setSwipeError(null);
    animationDoneRef.current = false;
    pendingNextRef.current = null;
    setFlying(dir);
    swipe({ movieId: effectiveCurrent.id, action }, {
      onSuccess: (nextMovie) => {
        if (action === 'LIKE') {
          const count = likesCount + 1;
          setLikesCount(count);
          if (count % RECS_EVERY_N_LIKES === 0) {
            fetchRecs();
            setShowRecs(true);
          }
        }
        if (animationDoneRef.current) {
          commitNext(nextMovie);
        } else {
          pendingNextRef.current = { next: nextMovie };
        }
      },
    });
  };

  const handleTransitionEnd = (e: React.TransitionEvent<HTMLDivElement>) => {
    if (!flying || e.propertyName !== 'transform') return;
    animationDoneRef.current = true;
    const pending = pendingNextRef.current;
    if (pending) {
      pendingNextRef.current = null;
      commitNext(pending.next);
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
  const dragRatio = flying ? 0 : Math.min(Math.abs(drag.x) / DRAG_THRESHOLD, 1);
  const glowColor = drag.x > 0 ? '62, 207, 142' : '224, 69, 77';
  const glowShadow = dragRatio > 0
    ? `0 0 0 2px rgba(${glowColor}, ${0.5 * dragRatio}), 0 0 40px rgba(${glowColor}, ${0.35 * dragRatio}), 0 24px 60px rgba(0,0,0,0.55)`
    : undefined;

  const flyTransform =
    flying === 'right' ? 'translateX(160%) rotate(22deg)' :
    flying === 'left' ? 'translateX(-160%) rotate(-22deg)' :
    flying === 'skip' ? 'translateY(-130%) scale(0.85)' :
    null;

  const cardStyle: React.CSSProperties = flying ? {
    transform: flyTransform!,
    opacity: 0,
    transition: `transform ${flying === 'skip' ? '0.18s ease-in' : '0.22s cubic-bezier(.4,0,.6,1)'}, opacity 0.22s linear`,
  } : {
    transform: drag.x !== 0 || drag.y !== 0
      ? `translate(${drag.x}px, ${drag.y * 0.3}px) rotate(${rotation}deg)`
      : undefined,
    transition: !isDragging ? 'transform 0.3s ease, box-shadow 0.2s ease' : 'box-shadow 0.1s ease',
    cursor: isDragging ? 'grabbing' : 'grab',
    boxShadow: glowShadow,
  };

  return (
    <>
      <div className="swipe-page">
        <div className="swipe-header">
          <span className="swipe-counter">
            <span className="swipe-counter-dot" />
            {likesCount} {likesCount === 1 ? 'like' : 'likes'}
          </span>
          {resetButton}
        </div>
        <div className="swipe-stack">
          <div
            key={effectiveCurrent.id}
            className="swipe-card"
            style={cardStyle}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onTransitionEnd={handleTransitionEnd}
          >
            {effectiveCurrent.poster_path ? (
              <img className="swipe-poster" src={effectiveCurrent.poster_path} alt={effectiveCurrent.title} draggable={false} />
            ) : (
              <div className="swipe-poster-placeholder">No poster</div>
            )}

            <div className="swipe-overlay">
              <h2 className="swipe-title">{effectiveCurrent.title}</h2>
              <div className="swipe-meta">
                {rating && <span className="movie-card-rating"><StarIcon /> {rating}</span>}
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

        {swipeError && <p className="swipe-error">{swipeError}</p>}

        <div className="swipe-actions">
          <button
            className="swipe-btn swipe-btn-dislike"
            onClick={() => launchCard('left', 'DISLIKE')}
            disabled={!canSwipe}
            title="Dislike"
            aria-label="Dislike"
          >
            <XIcon />
          </button>
          <button
            className="swipe-btn swipe-btn-skip"
            onClick={() => launchCard('skip', 'SKIP')}
            disabled={!canSwipe}
            title="Skip"
            aria-label="Skip"
          >
            <SkipIcon />
          </button>
          <button
            className="swipe-btn swipe-btn-like"
            onClick={() => launchCard('right', 'LIKE')}
            disabled={!canSwipe}
            title="Like"
            aria-label="Like"
          >
            <HeartIcon />
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
