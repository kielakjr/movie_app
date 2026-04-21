import { useQuery } from '@tanstack/react-query';
import { fetchMovies, searchSimilar } from '../api';

const useMovies = (page: number, size: number) => {
    return useQuery({
        queryKey: ['movies', page, size],
        queryFn: () => fetchMovies(page, size),
        placeholderData: (previousData) => previousData || { content: [], page: { size, total_elements: 0, total_pages: 0, number: 0 } },
        staleTime: 5 * 60 * 1000,
    });
};

const useSearchSimilarMovies = (query: string) => {
    return useQuery({
        queryKey: ['similarMovies', query],
        queryFn: () => searchSimilar(query, 10),
        enabled: !!query,
        placeholderData: [],
        staleTime: 5 * 60 * 1000,
    });
}

export { useMovies, useSearchSimilarMovies };
