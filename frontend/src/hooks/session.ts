import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchSession, postSessionReset } from '../api/session';

export const useSession = () => {
  return useQuery({
    queryKey: ['session'],
    queryFn: fetchSession,
    staleTime: 0,
  });
};

export const useResetSession = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: postSessionReset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['swipe'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['session'] });
    },
  });
};
