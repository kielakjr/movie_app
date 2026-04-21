CREATE INDEX IF NOT EXISTS idx_movies_embedding_hnsw
ON movies
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 128);

ANALYZE movies;
