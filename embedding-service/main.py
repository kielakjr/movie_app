from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()
model = SentenceTransformer("all-MiniLM-L6-v2")


class EmbeddingRequest(BaseModel):
    text: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]


@app.post("/embedding", response_model=EmbeddingResponse)
def get_embedding(request: EmbeddingRequest):
    embedding = model.encode(request.text, normalize_embeddings=True).tolist()
    return EmbeddingResponse(embedding=embedding)


@app.get("/health")
def health():
    return {"status": "ok"}
