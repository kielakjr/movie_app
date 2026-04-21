import type { PaginationData } from './page';

export interface MoviesResponse extends PaginationData<Movie> {}

export interface Movie {
  id: number;
  tmdbId: number;
  title: string;
  overview: string;
  release_date: string;
  original_language: string;
  adult: boolean;
  poster_path: string;
  backdrop_path: string;
  genres: string[];
  popularity: number;
  vote_average: number;
  vote_count: number;
}

