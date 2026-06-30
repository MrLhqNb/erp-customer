"""Pydantic request / response models."""

from __future__ import annotations

from pydantic import BaseModel, Field
from typing import Optional


# ── Chat ──────────────────────────────────────────────────

class ChatRequest(BaseModel):
    message: str = Field(..., description="用户消息")
    conversation_id: Optional[str] = Field(default=None, description="会话ID，不传则新建")
    max_context: Optional[int] = Field(default=None, description="检索片段数，默认5")


class Source(BaseModel):
    knowledge_id: Optional[int] = None
    title: Optional[str] = None
    snippet: Optional[str] = None
    score: float = 0.0


class ChatResponse(BaseModel):
    conversation_id: str
    answer: str
    sources: list[Source] = []
    message_id: Optional[int] = None


# ── Conversation ──────────────────────────────────────────

class MessageItem(BaseModel):
    id: Optional[int] = None
    role: str
    content: str
    sources: Optional[list[Source]] = None
    created_at: Optional[str] = None


class ConversationItem(BaseModel):
    conversation_id: str
    title: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None
    message_count: int = 0


class ConversationDetail(BaseModel):
    conversation_id: str
    title: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None
    messages: list[MessageItem] = []
