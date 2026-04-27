from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()
model = SentenceTransformer("all-MiniLM-L6-v2")


class EmbeddingRequest(BaseModel):
    text: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]


class BatchEmbeddingRequest(BaseModel):
    texts: list[str]


class BatchEmbeddingResponse(BaseModel):
    embeddings: list[list[float]]


@app.post("/embedding", response_model=EmbeddingResponse)
def get_embedding(request: EmbeddingRequest):
    embedding = model.encode(request.text, normalize_embeddings=True).tolist()
    return EmbeddingResponse(embedding=embedding)


@app.post("/embedding/batch", response_model=BatchEmbeddingResponse)
def get_embeddings_batch(request: BatchEmbeddingRequest):
    embeddings = model.encode(request.texts, normalize_embeddings=True).tolist()
    return BatchEmbeddingResponse(embeddings=embeddings)


@app.get("/health")
def health():
    return {"status": "ok"}
