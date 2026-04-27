import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postSessionReset } from '../api/session';

export const useResetSession = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: postSessionReset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['swipe'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
  });
};
