import type { PaginationData } from './page';

export interface MoviesResponse extends PaginationData<Movie> {}

interface Movie {
  id: number;
  tmdbId: number;
  title: string;
  overview: string;
  releaseDate: string;
  originalLanguage: string;
  adult: boolean;
  posterPath: string;
  backdropPath: string;
  genres: string[];
  popularity: number;
  voteAverage: number;
  voteCount: number;
}

