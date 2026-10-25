// Cloak SOS Access — Supabase Edge Function
// Routes: POST /session, POST /upload, GET /listen, GET /play
// Deploy with: npx supabase functions deploy cloak-sos-access --no-verify-jwt --project-ref uppnyyztbgntvxddzqbo

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "https://uppnyyztbgntvxddzqbo.supabase.co";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const BUCKET = "sos_audio";
const SIGNED_URL_EXPIRY = 120; // seconds
const SESSION_EXPIRY = 3600; // 1 hour

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Cache-Control": "no-store",
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json", "X-Content-Type-Options": "nosniff" },
  });
}

export function html(body: string, status = 200): Response {
  const headers = new Headers();
  headers.set("Content-Type", "text/html; charset=utf-8");
  headers.set("Access-Control-Allow-Origin", "*");
  headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  headers.set("Access-Control-Allow-Headers", "authorization, apikey, content-type");
  headers.set("Cache-Control", "no-store, must-revalidate");
  headers.set(
    "Content-Security-Policy",
    "default-src 'self' 'unsafe-inline' https://uppnyyztbgntvxddzqbo.supabase.co https://*.supabase.co; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; media-src * data: blob:;"
  );
  return new Response(body, {
    status,
    headers,
  });
}

function adminClient() {
  return createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
    auth: { persistSession: false },
  });
}

// ── Helper: Get Session Metadata (Table with Storage Fallback) ──
async function getSession(session_id: string): Promise<{ token: string; expires_at: string } | null> {
  const supabase = adminClient();
  try {
    const { data, error } = await supabase
      .from("sos_access_sessions")
      .select("token, expires_at")
      .eq("session_id", session_id)
      .single();
    if (!error && data?.token) return data;
  } catch (_) {
    // Ignore database lookup errors and fallback to storage
  }

  try {
    const { data: fileData, error: fileError } = await supabase.storage
      .from(BUCKET)
      .download(`${session_id}/.session.json`);
    if (!fileError && fileData) {
      const text = await fileData.text();
      return JSON.parse(text);
    }
  } catch (_) {
    // Session metadata not found
  }
  return null;
}

// ── POST /session ───────────────────────────────────────────────
async function handleSession(req: Request): Promise<Response> {
  const body = await req.json();
  const { session_id, token, trigger_source } = body;
  if (!session_id || !token) return json({ error: "Missing session_id or token" }, 400);

  const supabase = adminClient();
  const expires_at = new Date(Date.now() + SESSION_EXPIRY * 1000).toISOString();
  const sessionRecord = { session_id, token, trigger_source: trigger_source || "manual", expires_at };

  // 1. Attempt database persistence
  let dbSuccess = false;
  try {
    const { error } = await supabase.from("sos_access_sessions").upsert(
      sessionRecord,
      { onConflict: "session_id" }
    );
    if (!error) dbSuccess = true;
  } catch (_) {
    // Fall back to storage
  }

  // 2. Persist session token to private storage bucket as backup / primary store
  try {
    const sessionJson = JSON.stringify(sessionRecord);
    const { error: storageError } = await supabase.storage
      .from(BUCKET)
      .upload(`${session_id}/.session.json`, sessionJson, {
        contentType: "application/json",
        upsert: true,
      });

    if (storageError && !dbSuccess) {
      console.error("Session storage error:", storageError);
      return json({ error: "Request failed" }, 500);
    }
  } catch (err) {
    if (!dbSuccess) {
      console.error("Session creation exception:", err);
      return json({ error: "Request failed" }, 500);
    }
  }

  return json({ status: "created", expires_in_seconds: SESSION_EXPIRY });
}

// ── POST /upload ────────────────────────────────────────────────
async function handleUpload(req: Request): Promise<Response> {
  const formData = await req.formData();
  const session_id = formData.get("session_id") as string;
  const token = formData.get("token") as string;
  const file = formData.get("file") as File;

  if (!session_id || !token || !file) {
    return json({ error: "Missing session_id, token, or file" }, 400);
  }

  // Validate session token
  const sess = await getSession(session_id);
  if (!sess || sess.token !== token) return json({ error: "Unauthorized" }, 403);
  if (new Date(sess.expires_at) < new Date()) return json({ error: "Session expired" }, 403);

  const supabase = adminClient();
  const fileName = `chunk_${Date.now()}.m4a`;
  const filePath = `${session_id}/${fileName}`;

  const { error: uploadError } = await supabase.storage
    .from(BUCKET)
    .upload(filePath, file, { contentType: "audio/mp4", upsert: false });

  if (uploadError) {
    console.error("Upload error:", uploadError);
    return json({ error: "Upload failed" }, 500);
  }

  return json({ status: "uploaded", path: filePath });
}

// ── GET /listen ─────────────────────────────────────────────────
async function handleListen(url: URL): Promise<Response> {
  const session_id = url.searchParams.get("session_id");
  const token = url.searchParams.get("token");

  if (!session_id || !token) return json({ error: "Missing session_id or token" }, 400);

  // Validate session token
  const sess = await getSession(session_id);
  if (!sess || sess.token !== token) return json({ error: "Unauthorized" }, 403);
  if (new Date(sess.expires_at) < new Date()) return json({ error: "Session expired" }, 403);

  const supabase = adminClient();
  const { data: files, error } = await supabase.storage
    .from(BUCKET)
    .list(session_id, { sortBy: { column: "name", order: "asc" } });

  if (error || !files) return json({ recordings: [], refresh_after_seconds: 5 });

  const recordings = [];
  for (const f of files) {
    if (!f.name.endsWith(".m4a")) continue;
    const { data: signed } = await supabase.storage
      .from(BUCKET)
      .createSignedUrl(`${session_id}/${f.name}`, SIGNED_URL_EXPIRY);
    if (signed?.signedUrl) {
      recordings.push({ name: f.name, url: signed.signedUrl });
    }
  }

  return json({ recordings, refresh_after_seconds: 5 });
}

// ── GET /play ───────────────────────────────────────────────────
// Serves a self-contained HTML audio player page.
// The browser opens this directly from the SMS link — no auth headers needed.
export function handlePlay(): Response {
  const pageHtml = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Emergency Audio — Cloak Guardian</title>
<style>
:root{--bg:#0d1117;--card:#161b22;--border:#30363d;--text:#f0f6fc;--muted:#8b949e;--danger:#f85149;--accent:#58a6ff}
*{box-sizing:border-box;margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif}
body{background:var(--bg);color:var(--text);padding:16px;display:flex;flex-direction:column;align-items:center;min-height:100vh}
.c{width:100%;max-width:500px;display:flex;flex-direction:column;gap:16px}
.card{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:20px;display:flex;flex-direction:column;gap:12px}
.hdr{display:flex;align-items:center;justify-content:space-between}
.badge{display:inline-flex;align-items:center;gap:6px;background:rgba(248,81,73,.15);color:var(--danger);border:1px solid rgba(248,81,73,.4);padding:4px 10px;border-radius:20px;font-weight:700;font-size:.82rem;letter-spacing:.5px;text-transform:uppercase}
.dot{width:8px;height:8px;background:var(--danger);border-radius:50%;animation:p 1.5s infinite}
@keyframes p{0%{transform:scale(.95);box-shadow:0 0 0 0 rgba(248,81,73,.7)}70%{transform:scale(1.1);box-shadow:0 0 0 8px rgba(248,81,73,0)}100%{transform:scale(.95);box-shadow:0 0 0 0 rgba(248,81,73,0)}}
h1{font-size:1.3rem;font-weight:700}
.info{font-size:.8rem;color:var(--muted);word-break:break-all;background:rgba(0,0,0,.2);padding:8px;border-radius:6px}
.status{font-size:.95rem;font-weight:600;color:var(--accent);text-align:center}
audio{width:100%;border-radius:8px}
.row{display:flex;gap:12px;width:100%}
.btn{flex:1;padding:12px;border-radius:8px;border:none;font-weight:600;cursor:pointer;font-size:.9rem}
.bp{background:var(--accent);color:#0d1117}.bs{background:#21262d;color:var(--text);border:1px solid var(--border)}
.lt{font-size:.9rem;font-weight:600;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;display:flex;justify-content:space-between}
.cl{display:flex;flex-direction:column;gap:8px;max-height:280px;overflow-y:auto}
.ci{display:flex;align-items:center;justify-content:space-between;padding:10px 12px;background:#0d1117;border:1px solid var(--border);border-radius:6px;font-size:.85rem;cursor:pointer}
.ci.a{border-color:var(--accent);background:rgba(88,166,255,.1)}
.ci .n{font-weight:600}.ci .s{font-size:.75rem;color:var(--muted)}
.em{text-align:center;color:var(--muted);font-size:.85rem;padding:20px 0}
</style>
</head>
<body>
<div class="c">
<div class="card"><div class="hdr"><h1>Cloak Guardian</h1><div class="badge"><div class="dot"></div>LIVE SOS AUDIO</div></div><div class="info" id="sL">Loading session...</div></div>
<div class="card" style="align-items:center;text-align:center"><div class="status" id="pS">Connecting to secure audio stream...</div><audio id="aP" controls preload="auto"></audio><div class="row"><button class="btn bp" id="bA">Auto-Play: ON</button><button class="btn bs" id="bR">Refresh</button></div></div>
<div class="card"><div class="lt"><span>Audio Chunks</span><span id="cC">0</span></div><div class="cl" id="cL"><div class="em">Waiting for incoming audio...</div></div></div>
</div>
<script>
const urlParams = new URLSearchParams(window.location.search);
const SID = urlParams.get("session_id") || urlParams.get("sessionId") || "";
const TK = urlParams.get("token") || "";
const BASE = "https://uppnyyztbgntvxddzqbo.supabase.co";
const ANON = "sb_publishable_KL_66tjyRQ1P2bhvStUTaA_A6tY-54D";

const sL = document.getElementById("sL");
const pS = document.getElementById("pS");
const aP = document.getElementById("aP");
const cL = document.getElementById("cL");
const cC = document.getElementById("cC");
const bA = document.getElementById("bA");
const bR = document.getElementById("bR");

let recs = [];
let cur = -1;
let auto = true;
let tid = null;

bA.onclick = () => {
  auto = !auto;
  bA.textContent = "Auto-Play: " + (auto ? "ON" : "OFF");
  bA.className = "btn " + (auto ? "bp" : "bs");
};

bR.onclick = poll;

aP.onended = () => {
  if (auto && cur + 1 < recs.length) {
    play(cur + 1);
  } else if (auto) {
    pS.textContent = "Waiting for next chunk...";
  }
};

async function poll() {
  if (!SID || !TK) {
    sL.textContent = "Error: Missing session ID or access token.";
    pS.textContent = "Unauthorized: Please use the complete emergency link received via SMS.";
    pS.style.color = "#f85149";
    cL.innerHTML = '<div class="em">Invalid emergency link. Session ID and token are required.</div>';
    return;
  }

  sL.textContent = "Session: " + SID;

  try {
    const listenUrl = BASE + "/functions/v1/cloak-sos-access/listen?session_id=" + encodeURIComponent(SID) + "&token=" + encodeURIComponent(TK);
    const r = await fetch(listenUrl, {
      headers: {
        "apikey": ANON,
        "Authorization": "Bearer " + ANON
      }
    });

    if (!r.ok) {
      throw new Error("HTTP " + r.status);
    }

    const d = await r.json();
    const inc = d.recordings || [];

    if (inc.length !== recs.length) {
      const wasEmpty = (recs.length === 0);
      recs = inc;
      render();
      if (wasEmpty && auto && recs.length > 0) {
        play(0);
      } else if (cur === -1 && auto && recs.length > 0) {
        play(0);
      }
    }

    if (recs.length > 0 && cur >= 0) {
      pS.textContent = "Playing chunk " + (cur + 1) + " of " + recs.length;
    } else if (recs.length > 0) {
      pS.textContent = recs.length + " audio chunk(s) ready";
    } else {
      pS.textContent = "Waiting for first audio chunk from device...";
    }

    sched((d.refresh_after_seconds || 5) * 1000);
  } catch (e) {
    pS.textContent = "Connection issue: " + e.message + ". Retrying...";
    sched(5000);
  }
}

function sched(ms) {
  if (tid) clearTimeout(tid);
  tid = setTimeout(poll, ms);
}

function play(i) {
  if (i < 0 || i >= recs.length) return;
  cur = i;
  render();
  pS.textContent = "Playing chunk " + (i + 1) + "...";
  aP.src = recs[i].url;
  aP.play().catch(() => {
    pS.textContent = "Tap Play to start listening (autoplay restricted by browser)";
  });
}

function render() {
  cC.textContent = recs.length + " chunk" + (recs.length === 1 ? "" : "s");
  if (!recs.length) {
    cL.innerHTML = '<div class="em">Waiting for incoming audio...</div>';
    return;
  }
  cL.innerHTML = "";
  recs.forEach((c, i) => {
    const d = document.createElement("div");
    d.className = "ci" + (i === cur ? " a" : "");
    d.onclick = () => play(i);

    const n = document.createElement("span");
    n.className = "n";
    n.textContent = "Part #" + (i + 1) + " (" + c.name + ")";

    const s = document.createElement("span");
    s.className = "s";
    s.textContent = (i === cur ? "Playing" : "Ready");

    d.appendChild(n);
    d.appendChild(s);
    cL.appendChild(d);
  });
}

// Initial execution
if (!SID || !TK) {
  sL.textContent = "Error: Missing session ID or access token.";
  pS.textContent = "Unauthorized: Please use the complete emergency link received via SMS.";
  pS.style.color = "#f85149";
  cL.innerHTML = '<div class="em">Invalid emergency link. Session ID and token are required.</div>';
} else {
  poll();
}
</script>
</body>
</html>`;

  return html(pageHtml);
}

// ── Router ──────────────────────────────────────────────────────
serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }

  const url = new URL(req.url);
  const path = url.pathname.replace(/^\/cloak-sos-access/, "").replace(/\/$/, "") || "/";

  try {
    if (req.method === "POST" && path === "/session") return await handleSession(req);
    if (req.method === "POST" && path === "/upload") return await handleUpload(req);
    if (req.method === "GET" && path === "/listen") return await handleListen(url);
    if (req.method === "GET" && path === "/play") return handlePlay();
    return json({ error: "Not found" }, 404);
  } catch (e) {
    console.error("Handler error:", e);
    return json({ error: "Request failed" }, 500);
  }
});
