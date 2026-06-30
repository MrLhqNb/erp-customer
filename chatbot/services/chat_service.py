"""RAG Chat Pipeline — embed → retrieve → augment → generate."""

from __future__ import annotations

import httpx

from config import llm, vectorstore
from models.schemas import ChatResponse, Source
from services.embedding_service import embed
from services.vector_store_service import query
from services.conversation_service import (
    get_or_create, add_message, get_recent_messages,
)

SYSTEM_PROMPT = """\
You are a customer service assistant for the ERP system. \
Answer questions based ONLY on the provided context below. \
If the context does not contain enough information to answer the question, \
say "抱歉，我没有足够的信息来回答这个问题。请联系我们的支持团队获取进一步帮助。" \
Do not make up information or reference knowledge outside the provided context.

Context:
{context}"""


async def chat(message: str, conversation_id: str | None = None, max_context: int | None = None) -> ChatResponse:
    conv_id = await get_or_create(conversation_id)
    top_k = max_context or vectorstore.top_k

    # 1. embed query
    query_embedding = await embed(message)

    # 2. retrieve from vector store
    retrieved = query(query_embedding, top_k)

    # 3. filter by threshold
    threshold = vectorstore.similarity_threshold
    relevant = [r for r in retrieved if r.get("score", 0) >= threshold]
    if not relevant and retrieved:
        relevant = [retrieved[0]]

    # 4. build context string + sources
    context_parts = []
    sources = []
    for i, r in enumerate(relevant):
        title = r.get("title") or "Unknown"
        kid = r.get("knowledge_id")

        doc = r.get("document", "")
        context_parts.append(f"[{i+1}] {title}\n{doc}")
        snippet = doc[:200] + "..." if doc and len(doc) > 200 else doc
        sources.append(Source(knowledge_id=kid, title=title, snippet=snippet, score=r.get("score", 0)))

    context = "\n\n".join(context_parts)

    # 5. build messages: system + history + user
    messages = []
    messages.append({"role": "system", "content": SYSTEM_PROMPT.format(context=context)})

    history = await get_recent_messages(conv_id, limit=10)
    for h in history:
        messages.append({"role": h.role.lower(), "content": h.content})

    messages.append({"role": "user", "content": message})

    # 6. save user message
    await add_message(conv_id, "USER", message)

    # 7. call LLM
    answer = await _call_llm(messages)
    if not answer:
        answer = "抱歉，AI 服务暂时不可用，请稍后再试。"

    # 8. save assistant message
    src_dicts = [s.model_dump() for s in sources] if sources else None
    msg_id = await add_message(conv_id, "ASSISTANT", answer, src_dicts)

    return ChatResponse(
        conversation_id=conv_id,
        answer=answer,
        sources=sources,
        message_id=msg_id,
    )


async def _build_llm_payload(messages: list[dict]) -> dict:
    return {
        "model": llm.chat_model,
        "messages": messages,
        "temperature": llm.temperature,
        "max_tokens": llm.max_tokens,
    }


async def _call_llm(messages: list[dict]) -> str | None:
    url = f"{llm.base_url}/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {llm.api_key}",
    }
    body = await _build_llm_payload(messages)

    async with httpx.AsyncClient(timeout=60) as client:
        try:
            resp = await client.post(url, headers=headers, json=body)
            resp.raise_for_status()
            data = resp.json()
            return data["choices"][0]["message"]["content"]
        except httpx.HTTPError:
            return None
