import { createServer, type IncomingMessage } from "node:http";
import express from "express";
import { WebSocket, WebSocketServer } from "ws";

const PORT = Number(process.env.PORT ?? 8780);
const PUBLIC_URL = (process.env.PUBLIC_URL ?? `http://127.0.0.1:${PORT}`).replace(/\/$/, "");

interface AgentRecord {
  ws: WebSocket;
  nodeId: string;
  connectedAt: number;
}

interface PendingHttp {
  resolve: (value: HttpRelayResponse) => void;
  reject: (error: Error) => void;
  timer: NodeJS.Timeout;
}

interface HttpRelayResponse {
  status: number;
  headers: Record<string, string>;
  body: string;
}

type AgentMessage =
  | { type: "register"; nodeId: string; secret: string }
  | { type: "register_ok" }
  | { type: "register_fail"; message: string }
  | { type: "http_res"; id: string; status: number; headers?: Record<string, string>; body?: string }
  | { type: "tunnel_data"; tunnelId: string; data: string; binary?: boolean }
  | { type: "tunnel_closed"; tunnelId: string };

const agents = new Map<string, AgentRecord>();
const pendingHttp = new Map<string, PendingHttp>();
const tunnels = new Map<string, { client: WebSocket; agent: AgentRecord }>();

function log(message: string): void {
  console.log(`[relay] ${message}`);
}

function sendAgent(ws: WebSocket, payload: unknown): void {
  if (ws.readyState !== WebSocket.OPEN) return;
  ws.send(JSON.stringify(payload));
}

function agentFor(nodeId: string): AgentRecord | undefined {
  const record = agents.get(nodeId);
  if (!record || record.ws.readyState !== WebSocket.OPEN) return undefined;
  return record;
}

function parseAgentMessage(raw: Buffer): AgentMessage | null {
  try {
    return JSON.parse(raw.toString()) as AgentMessage;
  } catch {
    return null;
  }
}

function relayHttp(
  agent: AgentRecord,
  method: string,
  path: string,
  headers: Record<string, string>,
  body: string,
): Promise<HttpRelayResponse> {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pendingHttp.delete(id);
      reject(new Error("Upstream timeout"));
    }, 30_000);
    pendingHttp.set(id, { resolve, reject, timer });
    sendAgent(agent.ws, { type: "http_req", id, method, path, headers, body });
  });
}

const app = express();
app.disable("x-powered-by");

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "remote-pc-relay", agents: agents.size });
});

app.use("/n/:nodeId", express.raw({ type: () => true, limit: "4mb" }), async (req, res) => {
  const nodeId = String(req.params.nodeId ?? "");
  const agent = agentFor(nodeId);
  if (!agent) {
    res.status(503).json({ error: "PC offline", hint: "Start the Windows server with relay enabled." });
    return;
  }
  const prefix = `/n/${nodeId}`;
  const path = req.originalUrl.startsWith(prefix)
    ? req.originalUrl.slice(prefix.length) || "/"
    : req.url;
  const hopHeaders = new Set(["connection", "keep-alive", "transfer-encoding", "host", "content-length"]);
  const headers: Record<string, string> = {};
  for (const [key, value] of Object.entries(req.headers)) {
    if (hopHeaders.has(key.toLowerCase())) continue;
    if (typeof value === "string") headers[key] = value;
  }
  const body = Buffer.isBuffer(req.body) ? req.body.toString("utf8") : "";
  try {
    const upstream = await relayHttp(agent, req.method, path, headers, body);
    res.status(upstream.status);
    for (const [key, value] of Object.entries(upstream.headers)) {
      if (hopHeaders.has(key.toLowerCase())) continue;
      res.setHeader(key, value);
    }
    res.send(upstream.body);
  } catch (error) {
    res.status(502).json({
      error: "Relay failed",
      message: error instanceof Error ? error.message : String(error),
    });
  }
});

const httpServer = createServer(app);
const clientWss = new WebSocketServer({ noServer: true });
const agentWss = new WebSocketServer({ noServer: true });

httpServer.on("upgrade", (request, socket, head) => {
  const url = new URL(request.url ?? "/", `http://${request.headers.host ?? "localhost"}`);
  if (url.pathname === "/agent") {
    agentWss.handleUpgrade(request, socket, head, (ws) => handleAgentSocket(ws));
    return;
  }
  const match = url.pathname.match(/^\/n\/([^/]+)\/ws$/);
  if (match) {
    clientWss.handleUpgrade(request, socket, head, (ws) => openClientTunnel(match[1], ws));
    return;
  }
  socket.destroy();
});

function handleAgentSocket(ws: WebSocket): void {
  let nodeId: string | undefined;
  ws.on("message", (raw) => {
    const message = parseAgentMessage(Buffer.isBuffer(raw) ? raw : Buffer.from(String(raw)));
    if (!message) return;

    if (message.type === "register") {
      const id = String(message.nodeId ?? "").trim();
      const secret = String(message.secret ?? "");
      if (!id || id.length > 64 || !/^[a-zA-Z0-9_-]+$/.test(id)) {
        sendAgent(ws, { type: "register_fail", message: "Invalid nodeId" });
        ws.close(4002, "invalid node");
        return;
      }
      if (!secret || secret.length < 16) {
        sendAgent(ws, { type: "register_fail", message: "Invalid secret" });
        ws.close(4002, "invalid secret");
        return;
      }
      nodeId = id;
      agents.set(id, { ws, nodeId: id, connectedAt: Date.now() });
      sendAgent(ws, { type: "register_ok", publicUrl: PUBLIC_URL, nodePath: `/n/${id}` });
      log(`Agent registered: ${id}`);
      return;
    }

    if (message.type === "http_res" && message.id) {
      const pending = pendingHttp.get(message.id);
      if (!pending) return;
      clearTimeout(pending.timer);
      pendingHttp.delete(message.id);
      pending.resolve({
        status: message.status ?? 502,
        headers: message.headers ?? {},
        body: message.body ?? "",
      });
      return;
    }

    if (message.type === "tunnel_data" && message.tunnelId) {
      const tunnel = tunnels.get(message.tunnelId);
      if (!tunnel || tunnel.client.readyState !== WebSocket.OPEN) return;
      if (message.binary) {
        tunnel.client.send(Buffer.from(message.data, "base64"));
      } else {
        tunnel.client.send(message.data);
      }
      return;
    }

    if (message.type === "tunnel_closed" && message.tunnelId) {
      const tunnel = tunnels.get(message.tunnelId);
      if (!tunnel) return;
      tunnels.delete(message.tunnelId);
      if (tunnel.client.readyState === WebSocket.OPEN) tunnel.client.close(1000, "upstream closed");
    }
  });

  ws.on("close", () => {
    if (nodeId) {
      agents.delete(nodeId);
      log(`Agent disconnected: ${nodeId}`);
    }
    for (const [tunnelId, tunnel] of tunnels.entries()) {
      if (tunnel.agent.ws === ws) {
        tunnels.delete(tunnelId);
        if (tunnel.client.readyState === WebSocket.OPEN) tunnel.client.close(1011, "agent offline");
      }
    }
  });
}

function openClientTunnel(nodeId: string, client: WebSocket): void {
  const agent = agentFor(nodeId);
  if (!agent) {
    client.close(1013, "PC offline");
    return;
  }
  const tunnelId = `${nodeId}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  tunnels.set(tunnelId, { client, agent });

  sendAgent(agent.ws, { type: "tunnel_open", tunnelId });

  client.on("message", (raw) => {
    const agentLive = agentFor(nodeId);
    if (!agentLive) {
      client.close(1011, "agent offline");
      return;
    }
    if (Buffer.isBuffer(raw)) {
      sendAgent(agentLive.ws, {
        type: "tunnel_data",
        tunnelId,
        data: raw.toString("base64"),
        binary: true,
      });
    } else {
      sendAgent(agentLive.ws, { type: "tunnel_data", tunnelId, data: String(raw) });
    }
  });

  client.on("close", () => {
    tunnels.delete(tunnelId);
    const agentLive = agentFor(nodeId);
    if (agentLive) sendAgent(agentLive.ws, { type: "tunnel_close", tunnelId });
  });
}

httpServer.listen(PORT, () => {
  log(`Listening on ${PORT}`);
  log(`Public URL (set PUBLIC_URL in production): ${PUBLIC_URL}`);
  log(`Phone path pattern: ${PUBLIC_URL}/n/<nodeId>/…`);
});
