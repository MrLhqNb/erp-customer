"""Local embedding using sentence-transformers (free, no API key needed)."""

from __future__ import annotations

from sentence_transformers import SentenceTransformer

from config import embedding as cfg

_model: SentenceTransformer | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer(cfg.model)
    return _model


async def embed(text: str) -> list[float]:
    """Embed a single text."""
    m = _get_model()
    emb = m.encode(text, normalize_embeddings=True)
    return emb.tolist()


async def embed_batch(texts: list[str]) -> list[list[float]]:
    """Batch embed multiple texts."""
    if not texts:
        return []
    m = _get_model()
    embs = m.encode(texts, normalize_embeddings=True)
    return [e.tolist() for e in embs]
