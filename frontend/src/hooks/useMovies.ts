import { useQuery } from '@tanstack/react-query';
import { fetchMovies } from '../api';

const useMovies = (page: number, size: number) => {
    return useQuery({
        queryKey: ['movies', page, size],
        queryFn: () => fetchMovies(page, size),
        placeholderData: (previousData) => previousData || { content: [], page: { size, total_elements: 0, total_pages: 0, number: 0 } },
        staleTime: 5 * 60 * 1000,
    });
};

export { useMovies };
