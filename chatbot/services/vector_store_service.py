"""Milvus Lite — embedded, local file, no Docker needed."""

from __future__ import annotations

from pymilvus import MilvusClient, DataType, FieldSchema, CollectionSchema

from config import milvus as cfg, embedding as emb_cfg

DB_PATH = "./milvus_lite.db"
_client: MilvusClient | None = None


def _get_client() -> MilvusClient:
    global _client
    if _client is None:
        _client = MilvusClient(uri=DB_PATH)
    return _client


def ensure_collection():
    client = _get_client()
    name = cfg.collection_name

    if client.has_collection(name):
        return

    fields = [
        FieldSchema(name="id", dtype=DataType.VARCHAR, is_primary=True, max_length=64),
        FieldSchema(name="document", dtype=DataType.VARCHAR, max_length=65535),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=emb_cfg.dimension),
        FieldSchema(name="knowledge_id", dtype=DataType.INT64),
        FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=500),
        FieldSchema(name="chunk_index", dtype=DataType.INT64),
        FieldSchema(name="chunk_count", dtype=DataType.INT64),
    ]
    schema = CollectionSchema(fields, auto_id=False)
    client.create_collection(name, schema=schema)

    idx = client.prepare_index_params()
    idx.add_index(field_name="embedding", index_type="HNSW", metric_type="COSINE",
                  params={"M": 16, "efConstruction": 200})
    client.create_index(name, index_params=idx)
    client.load_collection(name)


async def ensure_collection_async():
    ensure_collection()


def query(query_embedding: list[float], top_k: int = 5) -> list:
    _ensure_init()

    results = _get_client().search(
        collection_name=cfg.collection_name,
        data=[query_embedding],
        limit=top_k,
        output_fields=["document", "knowledge_id", "title", "chunk_index", "chunk_count"],
    )

    out = []
    for hit in results[0]:
        ent = hit.get("entity", {})
        out.append({
            "id": hit.get("id", ""),
            "document": ent.get("document", ""),
            "knowledge_id": ent.get("knowledge_id", 0),
            "title": ent.get("title", ""),
            "chunk_index": ent.get("chunk_index", 0),
            "chunk_count": ent.get("chunk_count", 0),
            "score": hit.get("distance", 0.0),
        })
    return out


def add_documents(docs: list[dict]) -> int:
    _ensure_init()
    data = []
    for d in docs:
        data.append({
            "id": d["id"],
            "document": d.get("document", ""),
            "embedding": d.get("embedding", []),
            "knowledge_id": d.get("knowledge_id", 0),
            "title": d.get("title", ""),
            "chunk_index": d.get("chunk_index", 0),
            "chunk_count": d.get("chunk_count", 0),
        })
    result = _get_client().insert(collection_name=cfg.collection_name, data=data)
    return result.get("insert_count", 0)


def delete_by_ids(ids: list[str]) -> int:
    _ensure_init()
    if not ids:
        return 0
    expr = "id in [\"" + "\", \"".join(ids) + "\"]"
    _get_client().delete(collection_name=cfg.collection_name, filter=expr)
    return len(ids)


def list_all(limit: int = 200) -> list:
    _ensure_init()
    return _get_client().query(
        collection_name=cfg.collection_name,
        filter="id != ''",
        limit=limit,
        output_fields=["document", "knowledge_id", "title", "chunk_index", "chunk_count"],
    )


def _ensure_init():
    c = _get_client()
    if not c.has_collection(cfg.collection_name):
        ensure_collection()
    else:
        c.load_collection(cfg.collection_name)
