# Movie App

A movie discovery app that learns your taste in real time from a swipe feed and serves personalised recommendations using semantic vector search.

**Live demo:** [soothing-rejoicing-production-810e.up.railway.app](https://soothing-rejoicing-production-810e.up.railway.app)

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-strict-3178c6)
![Postgres](https://img.shields.io/badge/Postgres-16%20%2B%20pgvector-336791)

---

## What it does

- **Swipe feed** — swipe `LIKE`, `DISLIKE`, or `SKIP` on movies one at a time. Each action updates a server-side taste model in real time.
- **Personalised recommendations** — ranked list of movies your taste model predicts you'll enjoy, each annotated with the liked movie that drove it (*"Recommended because you liked X"*).
- **Semantic search** — free-text similarity search over the catalogue (e.g. *"slow burn psychological thriller with an unreliable narrator"*) using the same embedding model that powers recommendations.
- **Catalogue ingestion** — seed the database from [The Movie Database (TMDB)](https://www.themoviedb.org/) via a single API call.

> **Note:** Recommendations require swiping on at least a few movies first. The embedding service downloads a ~90 MB model on first start — subsequent starts use the cached version.

---

## Quick start

Prerequisites: Docker, Java 25, Node 24, a free [TMDB API key](https://www.themoviedb.org/settings/api).

```bash
# 1. Start Postgres (pgvector), Redis, and the embedding service
cd infra && docker compose up -d

# 2. Start the backend (in a new shell)
cd backend
TMDB_API_KEY=your_key_here ./mvnw spring-boot:run

# 3. Seed movies (wait until the backend is up)
curl -X POST 'http://localhost:8080/api/seed/all?pages=5'

# 4. Start the frontend (in a new shell)
cd frontend
npm install && npm run dev
```

App: `http://localhost:5173` · API docs: `http://localhost:8080/swagger-ui.html`

Run tests:

```bash
cd backend  && ./mvnw verify           # unit + Testcontainers integration tests
cd frontend && npm run lint && npm run build
```

---

## How the recommendation engine works

Every swipe updates a taste model stored in the user's Redis-backed session.

### 1. Embedding movies

During seed, each movie's title, overview, genres, and TMDB keywords are concatenated and sent to the embedding service, which runs [`sentence-transformers/all-MiniLM-L6-v2`](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2). The returned 384-dim L2-normalised vector is stored alongside the row in Postgres. An [HNSW index](https://en.wikipedia.org/wiki/Hierarchical_navigable_small_world) makes approximate nearest-neighbour queries fast at catalogue scale.

### 2. Online clustering of taste

Rather than averaging all liked embeddings into a single user vector — which collapses multi-modal taste (e.g. *"loves both arthouse drama and mindless action"*) — the backend keeps a list of **clusters** per session.

On each swipe (`ClusterService.addToClusters`):

- Compute cosine distance from the new embedding to each existing cluster centroid.
- If the closest cluster is within a threshold (`0.6`), absorb the embedding and update the centroid.
- Otherwise, spawn a new cluster.

Likes feed the *positive* clusters; dislikes feed a parallel set of *negative* clusters used to steer the feed away from content the user has rejected.

### 3. Serving the next swipe — exploration vs exploitation

`SwipeService.getNextMovie` implements an [epsilon-greedy](https://en.wikipedia.org/wiki/Multi-armed_bandit) strategy:

- **80% exploit** — pick a random positive cluster and return the nearest unseen movie via [`pgvector`](https://github.com/pgvector/pgvector) cosine search.
- **20% explore** — return a random unseen movie. Of those, 15% specifically return the *least similar* movie to a random negative cluster, so the system keeps learning when the user has a narrow taste profile.

### 4. Ranked recommendations with explanations

`RecommendService.getRecommendedMovies` fans out across all positive clusters, pulls top-N candidates per cluster from `pgvector`, and merges them ranked by cosine similarity to their cluster's centroid. Each recommendation also returns a *reason* — the single liked movie inside the cluster closest to the candidate.

---

## API

Full interactive docs at `/swagger-ui.html` when the backend is running.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/swipe` | Record a `LIKE` / `DISLIKE` / `SKIP` and return the next movie |
| `GET` | `/api/swipe/next` | Get the next movie without recording an action |
| `GET` | `/api/recommend?limit=10` | Cluster-ranked recommendations with reasons |
| `GET` | `/api/search/similar?query=...` | Free-text semantic search |
| `GET` | `/api/movies?page=&size=` | Paginated catalogue browse |
| `POST` | `/api/seed/all?pages=5` | Ingest popular + top-rated movies from TMDB |
| `POST` | `/api/session/reset` | Clear swipe state |
| `GET` | `/actuator/health` | Liveness / readiness probe |

All inputs are validated with Jakarta Bean Validation; errors are mapped through a `GlobalExceptionHandler` to consistent JSON responses.

---

## Architecture

```
                    ┌──────────────────────┐
                    │   React 19 + Vite    │
                    │   TanStack Query     │
                    └──────────┬───────────┘
                               │ JSON / cookies
                               ▼
    ┌──────────────────────────────────────────────────┐
    │           Spring Boot 4 / Java 25 API            │
    │  swipe · recommend · search · seed · session     │
    │  Bucket4j rate limit · springdoc OpenAPI         │
    └──┬────────────────┬───────────────┬──────────────┘
       │ JDBC           │ RESP          │ HTTP
       ▼                ▼               ▼
 ┌───────────┐    ┌──────────┐   ┌──────────────────┐
 │ Postgres  │    │  Redis   │   │  Embedding svc   │
 │ pgvector  │    │ Session  │   │  FastAPI +       │
 │  + HNSW   │    │  store   │   │  sentence-       │
 │           │    │          │   │  transformers    │
 └───────────┘    └──────────┘   └──────────────────┘
```

- **`backend`** — Spring Boot 4 on Java 25, the only public-facing API.
- **`embedding-service`** — Python FastAPI wrapping `all-MiniLM-L6-v2`. Exposes single and batch embed endpoints, called during seed and search.
- **`infra`** — Docker Compose orchestrating `pgvector/pgvector:pg16`, `redis:7.4-alpine`, and the embedding service. Redis backs Spring Session so swipe state survives backend restarts and scales horizontally.

---

## Tech stack

**Backend** — Java 25, Spring Boot 4 (web-mvc, data-jpa, validation, actuator), Flyway, Spring Session + Redis, [Bucket4j](https://github.com/bucket4j/bucket4j), springdoc-openapi, Lombok, Jackson (snake_case wire format), JUnit 5 + Mockito + [Testcontainers](https://testcontainers.com/)

**Embedding service** — Python 3.12, FastAPI, Uvicorn, sentence-transformers

**Frontend** — React 19, TypeScript strict, Vite 8, TanStack Query, React Router 7, ESLint flat config

**Database** — Postgres 16 + [pgvector](https://github.com/pgvector/pgvector), HNSW index on `vector(384)` with cosine distance (`m=16`, `ef_construction=128`)

**Infra & CI** — Docker Compose, GitHub Actions (backend `mvnw verify` + frontend lint & build on every push and PR)

---

## Engineering notes

- **Migrations, not auto-DDL.** Flyway-managed schema, `hibernate.ddl-auto=validate`. The vector column and HNSW index live in versioned SQL.
- **Package-by-feature.** Each domain (`swipe`, `recommend`, `cluster`, `search`, `session`, `seed`, `tmdb`, `embedding`) owns its controller, service, and DTOs.
- **Server-side session, not JWT.** Spring Session writes to Redis with an HttpOnly, SameSite=Lax cookie. Swipe state survives backend restarts and can scale to multiple instances.
- **Rate limiting at the boundary.** Bucket4j throttles outbound TMDB calls so seeding can't trip TMDB's quota.
- **Testing pyramid.** Unit tests (Mockito + MockMvc) for every service and controller. Integration tests via Testcontainers run vector search against a real `pgvector` instance, not a mock.

---

## Project layout

```
movie_app/
├── backend/                  # Spring Boot 4, Java 25
│   └── src/main/java/com/kielakjr/movie_app/
│       ├── cluster/          # Online clustering of swipe embeddings
│       ├── recommend/        # Cluster-ranked recommendations with reasons
│       ├── swipe/            # Swipe feed + epsilon-greedy next-movie picker
│       ├── search/           # Free-text semantic search
│       ├── session/          # Redis-backed swipe state
│       ├── seed/             # TMDB ingestion
│       ├── tmdb/             # TMDB client + rate limiter
│       ├── embedding/        # Embedding-service HTTP client
│       ├── movie/            # Catalogue domain
│       └── config/           # CORS, rate limit, exception handler
├── embedding-service/        # FastAPI + sentence-transformers
├── frontend/                 # React 19 + Vite + TanStack Query
├── infra/                    # Docker Compose (dev + prod)
└── .github/workflows/        # CI: backend verify + frontend lint/build
```
