import { useQuery } from '@tanstack/react-query';
import { fetchRecommendations } from '../api/recommend';

export const useRecommendations = (limit: number = 5) => {
  return useQuery({
    queryKey: ['recommendations'],
    queryFn: () => fetchRecommendations(limit),
    enabled: false,
    staleTime: 0,
    retry: false,
  });
};
