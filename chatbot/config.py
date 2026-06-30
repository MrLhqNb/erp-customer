"""Configuration — reads from environment variables with sensible defaults."""

import os


class EmbeddingConfig:
    model: str = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
    dimension: int = 512


class LlmConfig:
    base_url: str = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")
    api_key: str = os.getenv("DEEPSEEK_API_KEY", "sk-your-deepseek-key")  # 替换或设环境变量
    chat_model: str = os.getenv("LLM_CHAT_MODEL", "deepseek-chat")
    temperature: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))
    max_tokens: int = int(os.getenv("LLM_MAX_TOKENS", "2048"))
    chat_timeout: int = 60


class MilvusConfig:
    host: str = os.getenv("MILVUS_HOST", "localhost")
    port: int = int(os.getenv("MILVUS_PORT", "19530"))
    collection_name: str = os.getenv("MILVUS_COLLECTION", "erp_knowledge_base")


class VectorStoreConfig:
    chunk_size: int = int(os.getenv("VECTOR_CHUNK_SIZE", "512"))
    chunk_overlap: int = int(os.getenv("VECTOR_CHUNK_OVERLAP", "64"))
    top_k: int = int(os.getenv("VECTOR_TOP_K", "5"))
    similarity_threshold: float = float(os.getenv("VECTOR_SIMILARITY_THRESHOLD", "0.7"))


embedding = EmbeddingConfig()
llm = LlmConfig()
milvus = MilvusConfig()
vectorstore = VectorStoreConfig()
