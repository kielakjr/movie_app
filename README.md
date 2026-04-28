# Movie App — Vector-Based Movie Recommender with a Swipe UX

A full-stack movie discovery app that learns a user's taste in real time from a Tinder-style swipe feed and serves recommendations from a 384-dimensional semantic vector space backed by Postgres `pgvector`.

The project is built end-to-end as a multi-service system: a Spring Boot 4 / Java 25 API, a Python FastAPI embedding microservice, a React 19 + TypeScript frontend, and a containerised Postgres (pgvector) + Redis stack. It exercises the patterns I'd reach for in a production system: vector search, online clustering, session state, rate limiting, integration tests with Testcontainers, and CI on every push.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-strict-3178c6)
![Postgres](https://img.shields.io/badge/Postgres-16%20%2B%20pgvector-336791)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Table of Contents

- [Movie App — Vector-Based Movie Recommender with a Swipe UX](#movie-app--vector-based-movie-recommender-with-a-swipe-ux)
  - [Table of Contents](#table-of-contents)
  - [What it does](#what-it-does)
  - [Architecture](#architecture)
  - [Tech stack](#tech-stack)
  - [How the recommendation engine works](#how-the-recommendation-engine-works)
    - [1. Embedding movies](#1-embedding-movies)
    - [2. Online clustering of liked / disliked movies](#2-online-clustering-of-liked--disliked-movies)
    - [3. Serving the next swipe — exploration vs exploitation](#3-serving-the-next-swipe--exploration-vs-exploitation)
    - [4. Ranked recommendations with explanations](#4-ranked-recommendations-with-explanations)
  - [API surface](#api-surface)
  - [Engineering practices](#engineering-practices)
  - [Running locally](#running-locally)
  - [Project layout](#project-layout)

---

## What it does

- **Catalogue** — the backend ingests movies from The Movie Database (TMDB) via a seeding endpoint that fetches *popular* and *top-rated* titles, enriches them with genres and keywords, and stores them in Postgres with a 384-dim semantic embedding per movie.
- **Swipe feed** — the user is shown one movie at a time and swipes `LIKE` / `DISLIKE` / `SKIP`. Each interaction updates a server-side session model of their taste.
- **Live recommendations** — at any point the user can request a ranked list of personalised recommendations, each annotated with the *reason* movie that drove it (the closest liked movie in its cluster).
- **Semantic search** — free-text similarity search over the catalogue (e.g. "slow burn psychological thriller with an unreliable narrator") using the same embedding model.
- **Reset** — the swipe session can be wiped and started fresh.

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
     └───────────┘    └──────────┘   └────────┬─────────┘
                                              │
                                              ▼
                                       ┌──────────────┐
                                       │ TMDB public  │
                                       │ API (seed)   │
                                       └──────────────┘
```

Three runtime services, all containerised:

- **`backend`** — Spring Boot 4 on Java 25, the only public-facing API.
- **`embedding-service`** — Python FastAPI wrapping `sentence-transformers/all-MiniLM-L6-v2`. Exposes single and batch endpoints, called by the backend during seed and search.
- **`infra`** — `docker-compose.yml` orchestrates `pgvector/pgvector:pg16`, `redis:7.4-alpine`, and the embedding service. Redis backs Spring Session so swipe state survives backend restarts and could scale horizontally.

## Tech stack

**Backend**
- Java 25, Spring Boot 4 (web-mvc, data-jpa, validation, actuator)
- Flyway migrations (extensions, schema, HNSW index)
- Spring Session + Redis (server-side swipe state, HttpOnly cookie)
- Bucket4j for per-IP rate limiting on TMDB calls
- springdoc-openapi for live Swagger UI
- Lombok, Jackson (snake_case wire format)
- JUnit 5, Mockito, Testcontainers (Postgres + pgvector)

**Embedding service**
- Python 3.12, FastAPI, Uvicorn
- `sentence-transformers` (`all-MiniLM-L6-v2`, 384-dim, L2-normalised)

**Frontend**
- React 19, TypeScript, Vite 8
- TanStack Query for server state, caching, and optimistic updates
- React Router 7
- ESLint flat config

**Database**
- Postgres 16 with `pgvector`
- HNSW index on `vector(384)` with cosine distance (`m=16`, `ef_construction=128`)

**Infra & CI**
- Docker Compose for local dev
- GitHub Actions: backend `mvnw verify` + frontend lint & build on every push and PR

## How the recommendation engine works

The interesting part. Every swipe updates an in-memory taste model stored in the user's Redis-backed session.

### 1. Embedding movies

During seed, each movie's title, overview, genres, and TMDB keywords are concatenated and sent to the embedding service. The returned 384-dim vector is stored alongside the row in Postgres. An HNSW index makes approximate nearest-neighbour queries fast at catalogue scale (`backend/src/main/resources/db/migration/V3__movies_embedding_hnsw_index.sql`).

### 2. Online clustering of liked / disliked movies

Rather than averaging all liked embeddings into a single user vector (which collapses multi-modal taste — e.g. *"loves both arthouse drama and mindless action"*), the backend keeps a list of **clusters** per session.

On each swipe (`ClusterService.addToClusters`):

- Compute cosine distance from the new embedding to each existing cluster centroid.
- If the closest cluster is within a threshold (`0.6`), absorb the embedding and update the centroid.
- Otherwise, spawn a new cluster.

The result is an adaptive, non-parametric grouping of taste. Likes feed the *positive* clusters; dislikes feed a parallel set of *negative* clusters used to actively avoid bad recommendations.

### 3. Serving the next swipe — exploration vs exploitation

`SwipeService.getNextMovie` implements an epsilon-greedy strategy:

- 80% of the time → **exploit**: pick a random positive cluster and return the nearest unseen movie via `pgvector` cosine search.
- 20% of the time → **explore**: return a random unseen movie. Of those exploration picks, 15% specifically return the *least similar* movie to a random negative cluster — so the system still learns when the user has a narrow taste profile.

Tunables (`EXPLORATION_RATE`, `DISLIKE_EXPLOITATION_RATE`) live at the top of `SwipeService`.

### 4. Ranked recommendations with explanations

`RecommendService.getRecommendedMovies` fans out across all positive clusters, pulls top-N candidates per cluster from `pgvector`, and merges them ranked by cosine similarity to their cluster's centroid. For each recommendation it also returns a *reason* — the single liked movie inside the cluster that's closest to the candidate — so the UI can show *"Recommended because you liked X"*.

## API surface

OpenAPI is exposed at `/swagger-ui.html` when the backend is running. Highlights:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/swipe` | Record a `LIKE` / `DISLIKE` / `SKIP` and return the next movie |
| `GET` | `/api/swipe/next` | Get the next movie without recording an action |
| `GET` | `/api/recommend?limit=10` | Personalised, cluster-ranked recommendations with reasons |
| `GET` | `/api/search/similar?query=...` | Free-text semantic search |
| `GET` | `/api/movies?page=&size=` | Paginated catalogue browse |
| `POST` | `/api/seed/all?pages=5` | Ingest popular + top-rated movies from TMDB |
| `POST` | `/api/session/reset` | Clear swipe state |
| `GET` | `/actuator/health` | Liveness/readiness probes |

All inputs are validated with Jakarta Bean Validation; errors are mapped through a `GlobalExceptionHandler` to consistent JSON responses.

## Engineering practices

- **Migrations, not auto-DDL.** Flyway-managed schema, `hibernate.ddl-auto=validate`. The vector column and HNSW index are defined in versioned SQL.
- **Layered, package-by-feature.** Each domain (`swipe`, `recommend`, `cluster`, `search`, `session`, `seed`, `tmdb`, `embedding`) owns its controller, service, and DTOs. No god-package.
- **Server-side session, not JWT-in-localStorage.** Spring Session writes to Redis with an HttpOnly, SameSite=Lax cookie, so the swipe state survives restarts and can scale to multiple backend instances.
- **Rate limiting at the boundary.** Bucket4j throttles outbound TMDB calls (`TmdbRateLimitInterceptor`) so seeding can't trip TMDB's quota.
- **Testing pyramid:**
  - Unit tests for every service and controller (Mockito + MockMvc).
  - Integration tests using **Testcontainers** with the real `pgvector` image — vector search is exercised against a real Postgres, not a mock.
- **CI on every push.** GitHub Actions runs the full backend test suite (`mvnw verify`) and the frontend lint + production build.
- **Wire format consistency.** Backend serialises with `SNAKE_CASE`; frontend types match exactly.
- **Strict typing on the frontend.** TypeScript strict mode, ESLint flat config including `react-hooks` rules. TanStack Query handles caching, retry, and invalidation declaratively.

## Running locally

Prerequisites: Docker, Java 25, Node 24, a free [TMDB API key](https://www.themoviedb.org/settings/api).

```bash
# 1. Start Postgres (pgvector), Redis, and the embedding service
cd infra && docker compose up -d

# 2. Start the backend
cd ../backend
TMDB_API_KEY=your_key_here ./mvnw spring-boot:run

# 3. Seed some movies (in another shell)
curl -X POST 'http://localhost:8080/api/seed/all?pages=5'

# 4. Start the frontend
cd ../frontend
npm install
npm run dev
```

Open the app at `http://localhost:5173`, the API docs at `http://localhost:8080/swagger-ui.html`.

Run the test suites:

```bash
cd backend  && ./mvnw verify        # unit + Testcontainers integration tests
cd frontend && npm run lint && npm run build
```

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
├── infra/                    # docker-compose: pgvector, redis, embedding
└── .github/workflows/        # CI: backend verify + frontend lint/build
```

---

Built by [@mkielak](https://github.com/mkielak) as a portfolio project. Feedback and questions welcome.
