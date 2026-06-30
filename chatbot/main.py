"""FastAPI entry point — AI Customer Service Chat + Local Embedding."""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

from models.schemas import ChatRequest, ChatResponse
from services.chat_service import chat
from services.conversation_service import list_conversations, get_conversation, delete_conversation
from services.embedding_service import embed_batch, _get_model
from services.vector_store_service import ensure_collection_async


@asynccontextmanager
async def lifespan(app: FastAPI):
    await ensure_collection_async()
    # warm up embedding model
    _get_model()
    yield


app = FastAPI(title="ERP AI Customer Service", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Embedding (for Java to call) ──────────────────────────

class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    embeddings: list[list[float]]


@app.post("/api/embed", response_model=EmbedResponse)
async def handle_embed(req: EmbedRequest):
    embs = await embed_batch(req.texts)
    return EmbedResponse(embeddings=embs)


# ── Chat ──────────────────────────────────────────────────

@app.post("/api/chat")
async def handle_chat(req: ChatRequest):
    import traceback
    try:
        return await chat(
            message=req.message,
            conversation_id=req.conversation_id,
            max_context=req.max_context,
        )
    except Exception:
        traceback.print_exc()
        return {"error": traceback.format_exc()}


# ── Conversations ─────────────────────────────────────────

@app.get("/api/conversations")
async def list_convs():
    return await list_conversations()


@app.get("/api/conversations/{conversation_id}")
async def get_conv(conversation_id: str):
    r = await get_conversation(conversation_id)
    if r is None:
        raise HTTPException(404, "Conversation not found")
    return r


@app.delete("/api/conversations/{conversation_id}")
async def delete_conv(conversation_id: str):
    await delete_conversation(conversation_id)
    return {"ok": True}


# ── Vector CRUD (for Java to call) ─────────────────────────

from services.vector_store_service import add_documents, delete_by_ids, list_all as vs_list_all

class VectorDoc(BaseModel):
    id: str = ""
    document: str = ""
    embedding: list[float] = []
    knowledge_id: int = 0
    title: str = ""
    chunk_index: int = 0
    chunk_count: int = 0

class AddRequest(BaseModel):
    documents: list[VectorDoc]

class DeleteRequest(BaseModel):
    ids: list[str]

@app.post("/api/vectors/add")
async def vectors_add(req: AddRequest):
    cnt = add_documents([d.model_dump() for d in req.documents])
    return {"inserted": cnt}

@app.post("/api/vectors/delete")
async def vectors_delete(req: DeleteRequest):
    cnt = delete_by_ids(req.ids)
    return {"deleted": cnt}

@app.get("/api/vectors/list")
async def vectors_list(limit: int = 200):
    return vs_list_all(limit)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8092, reload=False)


# ── Chat UI Page ───────────────────────────────────────────

CHAT_HTML = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>AI 智能客服</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Microsoft YaHei','PingFang SC',sans-serif;background:#f0f2f5;height:100vh;display:flex}
.sidebar{width:280px;background:#1a1a2e;color:#fff;display:flex;flex-direction:column}
.sidebar h2{padding:20px;font-size:18px;border-bottom:1px solid rgba(255,255,255,0.15)}
.sidebar .conv-list{flex:1;overflow-y:auto;padding:8px}
.conv-item{padding:12px;margin:4px 0;border-radius:8px;cursor:pointer;font-size:14px;color:#ccc;border:1px solid transparent}
.conv-item:hover{background:rgba(255,255,255,0.08)}
.conv-item.active{background:rgba(255,255,255,0.12);color:#fff;border-color:rgba(255,255,255,0.2)}
.conv-item .count{font-size:11px;color:#888;margin-left:6px}
.sidebar .new-btn{margin:12px;padding:10px;background:#1890ff;color:#fff;border:none;border-radius:6px;cursor:pointer;font-size:14px}
.sidebar .new-btn:hover{background:#1677cc}
.main{flex:1;display:flex;flex-direction:column}
.chat-header{padding:16px 24px;background:#fff;border-bottom:1px solid #e8e8e8;font-size:16px;font-weight:bold}
.messages{flex:1;overflow-y:auto;padding:20px 24px}
.msg{display:flex;margin-bottom:16px;gap:10px}
.msg.user{justify-content:flex-end}
.msg .avatar{width:36px;height:36px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0}
.msg.assistant .avatar{background:#e6f7ff;color:#1890ff}
.msg.user .avatar{background:#1890ff;color:#fff}
.msg .bubble{max-width:70%;padding:12px 16px;border-radius:12px;font-size:14px;line-height:1.6;word-break:break-word}
.msg.assistant .bubble{background:#fff;border:1px solid #e8e8e8}
.msg.user .bubble{background:#1890ff;color:#fff}
.msg .sources{margin-top:8px;padding-top:8px;border-top:1px solid #f0f0f0;font-size:12px;color:#888}
.msg .sources .src{padding:4px 0}
.msg .sources .src .title{color:#1890ff;cursor:pointer}
.input-area{padding:16px 24px;background:#fff;border-top:1px solid #e8e8e8;display:flex;gap:10px}
.input-area textarea{flex:1;padding:10px 14px;border:1px solid #d9d9d9;border-radius:8px;font-size:14px;resize:none;height:48px;font-family:inherit}
.input-area textarea:focus{outline:none;border-color:#1890ff}
.input-area button{padding:0 24px;background:#1890ff;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:15px;font-weight:500;white-space:nowrap}
.input-area button:hover{background:#1677cc}
.input-area button:disabled{opacity:0.6;cursor:not-allowed}
.typing .dot{animation:blink 1.4s infinite both}
.typing .dot:nth-child(2){animation-delay:0.2s}
.typing .dot:nth-child(3){animation-delay:0.4s}
@keyframes blink{0%{opacity:0.2}20%{opacity:1}100%{opacity:0.2}}
.empty{display:flex;align-items:center;justify-content:center;height:100%;color:#bbb;font-size:20px;flex-direction:column;gap:12px}
.empty .icon{font-size:60px}
</style>
</head>
<body>
<div class="sidebar">
  <h2>AI 智能客服</h2>
  <button class="new-btn" onclick="newChat()">+ 新对话</button>
  <div class="conv-list" id="convList"></div>
</div>
<div class="main">
  <div class="chat-header" id="chatTitle">AI 智能客服</div>
  <div class="messages" id="messages">
    <div class="empty"><div class="icon">🤖</div>你好！我是 ERP AI 客服助手<br>请问有什么可以帮你的？</div>
  </div>
  <div class="input-area">
    <textarea id="msgInput" placeholder="输入你的问题..." onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();send()}"></textarea>
    <button onclick="send()">发送</button>
  </div>
</div>
<script>
const API='/api';
let convId=null;

async function loadConvs(){
  let resp=await fetch(API+'/conversations');
  let data=await resp.json();
  let list=document.getElementById('convList');
  list.innerHTML=data.map(c=>`<div class="conv-item${c.conversation_id===convId?' active':''}" onclick="selectConv('${c.conversation_id}')">
    ${c.title||'新对话'}<span class="count">${c.message_count}条</span>
  </div>`).join('');
}

async function selectConv(id){
  convId=id;
  let resp=await fetch(API+'/conversations/'+id);
  let data=await resp.json();
  document.getElementById('chatTitle').textContent=data.title||'AI 智能客服';
  renderMessages(data.messages||[]);
  loadConvs();
}

function newChat(){
  convId=null;
  document.getElementById('chatTitle').textContent='AI 智能客服';
  document.getElementById('messages').innerHTML='<div class="empty"><div class="icon">🤖</div>你好！我是 ERP AI 客服助手<br>请问有什么可以帮你的？</div>';
  loadConvs();
}

function renderMessages(msgs){
  let el=document.getElementById('messages');
  if(!msgs.length){el.innerHTML='<div class="empty"><div class="icon">🤖</div>你好！我是 ERP AI 客服助手<br>请问有什么可以帮你的？</div>';return}
  el.innerHTML=msgs.map(m=>{
    let role=m.role==='USER'?'user':'assistant';
    let avatar=role==='user'?'👤':'🤖';
    let srcs='';
    if(m.sources&&m.sources.length){
      srcs='<div class="sources">📎 引用来源:<br>'+m.sources.map(s=>`<div class="src"><span class="title">${esc(s.title||'?')}</span> (${(s.score*100).toFixed(0)}%)</div>`).join('')+'</div>';
    }
    return `<div class="msg ${role}"><div class="avatar">${avatar}</div><div class="bubble">${esc(m.content)}${srcs}</div></div>`;
  }).join('');
  el.scrollTop=el.scrollHeight;
}

async function send(){
  let input=document.getElementById('msgInput');
  let msg=input.value.trim();
  if(!msg)return;
  input.value='';
  // show user msg
  let msgs=document.getElementById('messages');
  if(msgs.querySelector('.empty'))msgs.innerHTML='';
  msgs.insertAdjacentHTML('beforeend',`<div class="msg user"><div class="avatar">👤</div><div class="bubble">${esc(msg)}</div></div>`);
  msgs.scrollTop=msgs.scrollHeight;
  // show typing
  let typing=document.createElement('div');
  typing.className='msg assistant';typing.innerHTML='<div class="avatar">🤖</div><div class="bubble typing"><span class="dot">●</span><span class="dot">●</span><span class="dot">●</span></div>';
  msgs.appendChild(typing);msgs.scrollTop=msgs.scrollHeight;
  let btn=document.querySelector('.input-area button');
  btn.disabled=true;
  try{
    let resp=await fetch(API+'/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:msg,conversation_id:convId})});
    let data=await resp.json();
    typing.remove();
    if(data.error){msgs.insertAdjacentHTML('beforeend',`<div class="msg assistant"><div class="avatar">🤖</div><div class="bubble">❌ ${esc(data.error)}</div></div>`)}
    else{
      convId=data.conversation_id;
      let srcs='';
      if(data.sources&&data.sources.length){
        srcs='<div class="sources">📎 引用来源:<br>'+data.sources.map(s=>`<div class="src"><span class="title">${esc(s.title||'?')}</span> (${(s.score*100).toFixed(0)}%)</div>`).join('')+'</div>';
      }
      msgs.insertAdjacentHTML('beforeend',`<div class="msg assistant"><div class="avatar">🤖</div><div class="bubble">${esc(data.answer)}${srcs}</div></div>`);
    }
    msgs.scrollTop=msgs.scrollHeight;
    loadConvs();
  }catch(e){
    typing.remove();
    msgs.insertAdjacentHTML('beforeend',`<div class="msg assistant"><div class="avatar">🤖</div><div class="bubble">❌ 网络错误: ${esc(e.message)}</div></div>`);
  }
  btn.disabled=false;
}

function esc(s){return s?String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'):''}
loadConvs();
</script>
</body>
</html>"""


@app.get("/", response_class=HTMLResponse)
async def home():
    return CHAT_HTML
