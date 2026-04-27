import type { Movie } from '../types';
import { fetchWithCreds } from './index';

export type SwipeAction = 'LIKE' | 'DISLIKE' | 'SKIP';

export const fetchNextSwipeMovie = async (): Promise<Movie> => {
  const response = await fetchWithCreds('/api/swipe/next');
  if (!response.ok) {
    throw new Error('No more movies available');
  }
  return response.json();
};

export const postSwipe = async (movieId: number, action: SwipeAction): Promise<Movie | null> => {
  const response = await fetchWithCreds('/api/swipe', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ movie_id: movieId, action }),
  });
  if (response.status === 204) return null;
  if (!response.ok) {
    throw new Error('Failed to swipe');
  }
  return response.json();
};
