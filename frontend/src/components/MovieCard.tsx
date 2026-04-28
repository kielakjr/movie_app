import type { Movie } from '../types';
import { StarIcon } from './Icons';

interface MovieCardProps {
  movie: Movie;
}

const MovieCard = ({ movie }: MovieCardProps) => {
  const year = movie.release_date?.slice(0, 4);
  const rating = movie.vote_average ? movie.vote_average.toFixed(1) : null;

  return (
    <div className="movie-card">
      <div className="movie-card-poster-wrap">
        {rating && (
          <span className="movie-card-rating-badge"><StarIcon /> {rating}</span>
        )}
        {movie.poster_path ? (
          <img
            className="movie-card-poster"
            src={movie.poster_path}
            alt={movie.title}
            loading="lazy"
          />
        ) : (
          <div className="movie-card-poster-placeholder">No poster</div>
        )}
      </div>
      <div className="movie-card-body">
        <h3 className="movie-card-title">{movie.title}</h3>
        <div className="movie-card-meta">
          {year && <span className="movie-card-year">{year}</span>}
        </div>
        {movie.genres && movie.genres.length > 0 && (
          <div className="movie-card-genres">
            {movie.genres.slice(0, 2).map(genre => (
              <span key={genre} className="genre-tag">{genre}</span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default MovieCard;
