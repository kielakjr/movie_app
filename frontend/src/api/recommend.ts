import type { Movie } from '../types';
import { fetchWithCreds } from './index';

export const fetchRecommendations = async (limit: number): Promise<Movie[]> => {
  const response = await fetchWithCreds(`/api/recommend?limit=${limit}`);
  if (!response.ok) {
    throw new Error('Failed to fetch recommendations');
  }
  return response.json();
};
