"""Conversation history — simple SQLite storage."""

from __future__ import annotations

import aiosqlite
import json
from datetime import datetime
from pathlib import Path
from typing import Optional

from models.schemas import ConversationItem, ConversationDetail, MessageItem

DB_PATH = Path(__file__).resolve().parent.parent / "conversations.db"


async def _get_db() -> aiosqlite.Connection:
    db = await aiosqlite.connect(str(DB_PATH))
    db.row_factory = aiosqlite.Row
    await db.execute("PRAGMA journal_mode=WAL")
    await db.execute("""
        CREATE TABLE IF NOT EXISTS conversations (
            id TEXT PRIMARY KEY,
            title TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
    """)
    await db.execute("""
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            conversation_id TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            sources TEXT,
            created_at TEXT NOT NULL,
            FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
        )
    """)
    await db.commit()
    return db


async def create_conversation() -> str:
    import uuid
    conv_id = uuid.uuid4().hex
    now = datetime.now().isoformat()
    db = await _get_db()
    await db.execute(
        "INSERT INTO conversations (id, title, created_at, updated_at) VALUES (?, ?, ?, ?)",
        (conv_id, None, now, now),
    )
    await db.commit()
    await db.close()
    return conv_id


async def get_or_create(conversation_id: Optional[str]) -> str:
    if not conversation_id:
        return await create_conversation()
    db = await _get_db()
    row = await db.execute("SELECT id FROM conversations WHERE id = ?", (conversation_id,))
    if await row.fetchone() is None:
        await db.close()
        return await create_conversation()
    await db.close()
    return conversation_id


async def add_message(conversation_id: str, role: str, content: str, sources: list | None = None) -> int:
    now = datetime.now().isoformat()
    sources_json = json.dumps(sources, ensure_ascii=False) if sources else None
    db = await _get_db()
    cur = await db.execute(
        "INSERT INTO messages (conversation_id, role, content, sources, created_at) VALUES (?, ?, ?, ?, ?)",
        (conversation_id, role, content, sources_json, now),
    )
    # update conversation timestamp
    await db.execute("UPDATE conversations SET updated_at = ? WHERE id = ?", (now, conversation_id))
    await db.commit()
    msg_id = cur.lastrowid
    await db.close()
    return msg_id


async def get_recent_messages(conversation_id: str, limit: int = 10) -> list[MessageItem]:
    db = await _get_db()
    cur = await db.execute(
        "SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at DESC LIMIT ?",
        (conversation_id, limit),
    )
    rows = await cur.fetchall()
    await db.close()
    items = []
    for r in reversed(rows):
        srcs = None
        if r["sources"]:
            try:
                srcs = json.loads(r["sources"])
            except json.JSONDecodeError:
                pass
        items.append(MessageItem(
            id=r["id"], role=r["role"], content=r["content"],
            sources=srcs, created_at=r["created_at"],
        ))
    return items


async def list_conversations(limit: int = 50) -> list[ConversationItem]:
    db = await _get_db()
    cur = await db.execute(
        """SELECT c.*, COUNT(m.id) as msg_count
           FROM conversations c LEFT JOIN messages m ON c.id = m.conversation_id
           GROUP BY c.id ORDER BY c.updated_at DESC LIMIT ?""",
        (limit,),
    )
    rows = await cur.fetchall()
    await db.close()
    return [
        ConversationItem(
            conversation_id=r["id"], title=r["title"],
            created_at=r["created_at"], updated_at=r["updated_at"],
            message_count=r["msg_count"],
        )
        for r in rows
    ]


async def get_conversation(conversation_id: str) -> Optional[ConversationDetail]:
    db = await _get_db()
    cur = await db.execute("SELECT * FROM conversations WHERE id = ?", (conversation_id,))
    conv = await cur.fetchone()
    if conv is None:
        await db.close()
        return None

    cur = await db.execute(
        "SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at ASC",
        (conversation_id,),
    )
    msgs = await cur.fetchall()
    await db.close()

    messages = []
    for r in msgs:
        srcs = None
        if r["sources"]:
            try:
                srcs = json.loads(r["sources"])
            except json.JSONDecodeError:
                pass
        messages.append(MessageItem(
            id=r["id"], role=r["role"], content=r["content"],
            sources=srcs, created_at=r["created_at"],
        ))
    return ConversationDetail(
        conversation_id=conv["id"], title=conv["title"],
        created_at=conv["created_at"], updated_at=conv["updated_at"],
        messages=messages,
    )


async def delete_conversation(conversation_id: str) -> bool:
    db = await _get_db()
    cur = await db.execute("DELETE FROM messages WHERE conversation_id = ?", (conversation_id,))
    await db.execute("DELETE FROM conversations WHERE id = ?", (conversation_id,))
    await db.commit()
    deleted = cur.rowcount >= 0
    await db.close()
    return deleted
