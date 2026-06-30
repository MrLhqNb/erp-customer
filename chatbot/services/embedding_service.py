"""Local embedding using ModelScope (国内秒下，不走 HuggingFace)."""

from __future__ import annotations

import os

# IMPORTANT: set before importing sentence_transformers
os.environ["SENTENCE_TRANSFORMERS_HOME"] = "./models_cache"

from modelscope import snapshot_download
from sentence_transformers import SentenceTransformer

from config import embedding as cfg

_model: SentenceTransformer | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        # Download from ModelScope to local cache
        local_path = snapshot_download(
            cfg.model,
            cache_dir="./models_cache",
        )
        # Load from local path
        _model = SentenceTransformer(local_path)
    return _model


async def embed(text: str) -> list[float]:
    m = _get_model()
    emb = m.encode(text, normalize_embeddings=True)
    return emb.tolist()


async def embed_batch(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    m = _get_model()
    embs = m.encode(texts, normalize_embeddings=True)
    return [e.tolist() for e in embs]
