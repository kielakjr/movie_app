import { useState, useRef, useEffect } from 'react';
import { useNextSwipeMovie, usePeekMovie, useSwipe, useRecommendations } from '../hooks';
import type { Movie } from '../types';
import RecommendationsModal from '../components/RecommendationsModal';

const RECS_EVERY_N_LIKES = 5;
const DRAG_THRESHOLD = 80;
const MAX_ROTATION = 18;

const Swipe = () => {
  const { data: initialMovie, isLoading, error } = useNextSwipeMovie();
  const { mutate: swipe } = useSwipe();
  const { data: recommendations, isFetching: recsFetching, refetch: fetchRecs } = useRecommendations(5);

  const [likesCount, setLikesCount] = useState(0);
  const [showRecs, setShowRecs] = useState(false);
  const [drag, setDrag] = useState({ x: 0, y: 0 });
  const [flying, setFlying] = useState<'left' | 'right' | 'skip' | null>(null);

  const [current, setCurrent] = useState<Movie | null>(null);
  const [peek, setPeek] = useState<Movie | null>(null);

  const dragStart = useRef<{ x: number; y: number; active: boolean }>({ x: 0, y: 0, active: false });
  const pendingAction = useRef<{ action: 'LIKE' | 'DISLIKE' | 'SKIP'; movieId: number } | null>(null);

  useEffect(() => {
    if (initialMovie && !current) {
      setCurrent(initialMovie);
    }
  }, [initialMovie]);

  const { data: fetchedPeek } = usePeekMovie(current && !peek ? current.id : undefined);

  useEffect(() => {
    if (fetchedPeek && fetchedPeek.id !== current?.id) {
      setPeek(fetchedPeek);
    }
  }, [fetchedPeek, current?.id]);

  if (isLoading) return <div className="state-center">Loading...</div>;
  if (error || (!isLoading && !initialMovie)) return (
    <div className="state-center swipe-done">
      <span className="swipe-done-icon">🎬</span>
      <p>You've seen all movies!</p>
      <p className="swipe-done-sub">Check back later for new additions.</p>
    </div>
  );
  if (!current) return null;

  const year = current.release_date?.slice(0, 4);
  const rating = current.vote_average ? current.vote_average.toFixed(1) : null;

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
    if (flying) return;
    pendingAction.current = { action, movieId: current.id };
    setFlying(dir);
    setDrag({ x: 0, y: 0 });
  };

  const handleAnimationEnd = () => {
    const pending = pendingAction.current;
    pendingAction.current = null;
    setFlying(null);

    setCurrent(peek);
    setPeek(null);

    if (pending) {
      doSwipe(pending.movieId, pending.action);
    }
  };

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (flying) return;
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
        <div className="swipe-stack">
          {peek && (
            <div className="swipe-card swipe-card-peek" aria-hidden>
              {peek.poster_path ? (
                <img className="swipe-poster" src={peek.poster_path} alt={peek.title} draggable={false} />
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
            {current.poster_path ? (
              <img className="swipe-poster" src={current.poster_path} alt={current.title} draggable={false} />
            ) : (
              <div className="swipe-poster-placeholder">No poster</div>
            )}

            <div className="swipe-overlay">
              <h2 className="swipe-title">{current.title}</h2>
              <div className="swipe-meta">
                {rating && <span className="movie-card-rating">★ {rating}</span>}
                {year && <span className="movie-card-year">{year}</span>}
                {current.original_language && (
                  <span className="swipe-lang">{current.original_language.toUpperCase()}</span>
                )}
              </div>
              {current.genres && current.genres.length > 0 && (
                <div className="movie-card-genres">
                  {current.genres.slice(0, 3).map(genre => (
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
            disabled={!!flying}
            title="Dislike"
          >
            ✕
          </button>
          <button
            className="swipe-btn swipe-btn-skip"
            onClick={() => launchCard('skip', 'SKIP')}
            disabled={!!flying}
            title="Skip"
          >
            →
          </button>
          <button
            className="swipe-btn swipe-btn-like"
            onClick={() => launchCard('right', 'LIKE')}
            disabled={!!flying}
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
