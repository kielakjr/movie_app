CREATE TABLE IF NOT EXISTS movies (
  id                BIGSERIAL PRIMARY KEY,
  tmdb_id           BIGINT NOT NULL UNIQUE,

  title             TEXT NOT NULL,
  overview          TEXT,

  release_date      DATE,
  original_language TEXT,

  adult             BOOLEAN NOT NULL DEFAULT FALSE,

  poster_path       TEXT,
  backdrop_path     TEXT,

  genres            TEXT[],

  popularity        DOUBLE PRECISION,
  vote_average      DOUBLE PRECISION,
  vote_count        INT,

  embedding         vector(384),

  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
