import { useQuery, useMutation } from '@tanstack/react-query';
import { fetchNextSwipeMovie, postSwipe } from '../api/swipe';
import type { SwipeAction } from '../api/swipe';

export const useNextSwipeMovie = () => {
  return useQuery({
    queryKey: ['swipe', 'next'],
    queryFn: fetchNextSwipeMovie,
    retry: false,
    staleTime: 0,
  });
};

export const useSwipe = () => {
  return useMutation({
    mutationFn: ({ movieId, action }: { movieId: number; action: SwipeAction }) =>
      postSwipe(movieId, action),
  });
};
