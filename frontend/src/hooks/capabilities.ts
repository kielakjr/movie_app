import { useQuery } from '@tanstack/react-query';
import { fetchCapabilities } from '../api';

const useCapabilities = () => {
  return useQuery({
    queryKey: ['capabilities'],
    queryFn: fetchCapabilities,
    staleTime: Infinity,
  });
};

export { useCapabilities };
