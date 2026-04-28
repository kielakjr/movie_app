import { Link } from 'react-router';
import { ArrowRightIcon, FilmIcon, LayersIcon, SparklesIcon } from '../components/Icons';

const Home = () => {
  return (
    <>
      <section className="hero">
        <span className="hero-eyebrow">
          <span className="hero-eyebrow-dot" />
          Personalized recommendations
        </span>
        <h1 className="hero-title">
          Discover your next<br />
          <em>favorite film</em>
        </h1>
        <p className="hero-subtitle">
          Browse thousands of movies, swipe through curated picks, and let our
          recommender surface hidden gems tailored to your taste.
        </p>
        <div className="hero-actions">
          <Link to="/movies" className="hero-cta">
            Browse movies <ArrowRightIcon />
          </Link>
          <Link to="/swipe" className="hero-cta-secondary">
            Start swiping
          </Link>
        </div>
      </section>

      <section className="features">
        <div className="feature-card">
          <div className="feature-icon"><FilmIcon /></div>
          <h3 className="feature-title">Vast library</h3>
          <p className="feature-desc">
            Explore thousands of films with rich metadata, ratings, and genre tags.
          </p>
        </div>
        <div className="feature-card">
          <div className="feature-icon"><SparklesIcon /></div>
          <h3 className="feature-title">Smart recommendations</h3>
          <p className="feature-desc">
            Our recommender learns from your taste to surface films you'll love.
          </p>
        </div>
        <div className="feature-card">
          <div className="feature-icon"><LayersIcon /></div>
          <h3 className="feature-title">Swipe to rate</h3>
          <p className="feature-desc">
            Quickly rate movies with a fluid swipe interface — like, skip, or pass.
          </p>
        </div>
      </section>
    </>
  );
};

export default Home;
