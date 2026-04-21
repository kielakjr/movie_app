import type { MoviesResponse } from '../types';

const fetchMovies = async (page: number, size: number): Promise<MoviesResponse> => {
  const response = await fetch(`/api/movies?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error('Failed to fetch movies');
  }
  return response.json();
};

const searchSimilar = async (query: string, limit: number): Promise<MoviesResponse> => {
  const response = await fetch(`/api/movies/similar?query=${encodeURIComponent(query)}&limit=${limit}`);
  if (!response.ok) {
    throw new Error('Failed to search for similar movies');
  }
  return response.json();
}

export { fetchMovies, searchSimilar };
