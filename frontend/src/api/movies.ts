import type { Movie, MoviesResponse } from '../types';
import { fetchWithCreds } from './index';

const fetchMovies = async (page: number, size: number): Promise<MoviesResponse> => {
  const response = await fetchWithCreds(`/api/movies?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error('Failed to fetch movies');
  }
  return response.json();
};

const searchSimilar = async (query: string, limit: number): Promise<Array<Movie>> => {
  const response = await fetchWithCreds(`/api/search/similar?query=${encodeURIComponent(query)}&limit=${limit}`);
  if (!response.ok) {
    throw new Error('Failed to search for similar movies');
  }
  return response.json();
}

export { fetchMovies, searchSimilar };
