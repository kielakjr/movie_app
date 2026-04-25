import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchNextSwipeMovie, fetchPeekMovie, postSwipe } from '../api/swipe';
import type { SwipeAction } from '../api/swipe';

export const useNextSwipeMovie = () => {
  return useQuery({
    queryKey: ['swipe', 'next'],
    queryFn: fetchNextSwipeMovie,
    retry: false,
    staleTime: 0,
  });
};

export const usePeekMovie = (excludeId: number | undefined) => {
  return useQuery({
    queryKey: ['swipe', 'peek', excludeId],
    queryFn: () => fetchPeekMovie(excludeId!),
    enabled: excludeId != null,
    retry: false,
    staleTime: 0,
  });
};

export const useSwipe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ movieId, action }: { movieId: number; action: SwipeAction }) =>
      postSwipe(movieId, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['swipe', 'next'] });
    },
  });
};
