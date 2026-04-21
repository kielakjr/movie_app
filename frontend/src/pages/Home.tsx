import { Link } from 'react-router';

const Home = () => {
  return (
    <div className="hero">
      <h1 className="hero-title">Discover your next<br />favorite film</h1>
      <p className="hero-subtitle">
        Browse thousands of movies, explore ratings, genres, and find hidden gems.
      </p>
      <Link to="/movies" className="hero-cta">Browse Movies</Link>
    </div>
  );
};

export default Home;
