import WebSocket from "ws";
import { log } from "../logger.js";
import type { RelayConfig } from "../config/load.js";
import { setRelayAgentOnline } from "./status.js";

interface RelayAgentOptions {
  relay: RelayConfig;
  localPort: number;
}

type RelayMessage =
  | { type: "register_ok"; publicUrl?: string; nodePath?: string }
  | { type: "register_fail"; message?: string }
  | { type: "http_req"; id: string; method: string; path: string; headers?: Record<string, string>; body?: string }
  | { type: "tunnel_open"; tunnelId: string }
  | { type: "tunnel_data"; tunnelId: string; data: string; binary?: boolean }
  | { type: "tunnel_close"; tunnelId: string };

const tunnels = new Map<string, WebSocket>();

function relayWsUrl(relayUrl: string): string {
  const trimmed = relayUrl.replace(/\/$/, "");
  if (trimmed.endsWith("/agent")) return trimmed;
  const wsBase = trimmed.startsWith("https://")
    ? `wss://${trimmed.slice("https://".length)}`
    : trimmed.startsWith("http://")
      ? `ws://${trimmed.slice("http://".length)}`
      : `wss://${trimmed}`;
  return `${wsBase}/agent`;
}

function sendJson(ws: WebSocket, payload: unknown): void {
  if (ws.readyState !== WebSocket.OPEN) return;
  ws.send(JSON.stringify(payload));
}

async function handleHttpRequest(
  message: Extract<RelayMessage, { type: "http_req" }>,
  localPort: number,
  ws: WebSocket,
): Promise<void> {
  const url = `http://127.0.0.1:${localPort}${message.path}`;
  try {
    const response = await fetch(url, {
      method: message.method,
      headers: message.headers,
      body: message.body ? message.body : undefined,
    });
    const body = await response.text();
    const headers: Record<string, string> = {};
    response.headers.forEach((value, key) => {
      headers[key] = value;
    });
    sendJson(ws, {
      type: "http_res",
      id: message.id,
      status: response.status,
      headers,
      body,
    });
  } catch (error) {
    sendJson(ws, {
      type: "http_res",
      id: message.id,
      status: 502,
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        error: "Local upstream failed",
        message: error instanceof Error ? error.message : String(error),
      }),
    });
  }
}

function openLocalTunnel(tunnelId: string, localPort: number, relayWs: WebSocket): void {
  const upstream = new WebSocket(`ws://127.0.0.1:${localPort}/ws`);
  tunnels.set(tunnelId, upstream);

  upstream.on("open", () => log(`Relay tunnel open: ${tunnelId}`));

  upstream.on("message", (raw) => {
    if (relayWs.readyState !== WebSocket.OPEN) return;
    if (Buffer.isBuffer(raw)) {
      sendJson(relayWs, {
        type: "tunnel_data",
        tunnelId,
        data: raw.toString("base64"),
        binary: true,
      });
    } else {
      sendJson(relayWs, { type: "tunnel_data", tunnelId, data: String(raw) });
    }
  });

  upstream.on("close", () => {
    tunnels.delete(tunnelId);
    sendJson(relayWs, { type: "tunnel_closed", tunnelId });
  });

  upstream.on("error", (error) => {
    log(`Relay tunnel error: ${error.message}`);
    upstream.close();
  });
}

export function startRelayAgent(options: RelayAgentOptions): () => void {
  const { relay, localPort } = options;
  if (!relay.enabled) return () => undefined;

  let socket: WebSocket | undefined;
  let stopped = false;
  let retryMs = 2000;

  const connect = (): void => {
    if (stopped) return;
    const url = relayWsUrl(relay.url);
    socket = new WebSocket(url);

    socket.on("open", () => {
      retryMs = 2000;
      sendJson(socket!, {
        type: "register",
        nodeId: relay.nodeId,
        secret: relay.secret,
      });
      log(`Relay agent connecting (${relay.nodeId})`);
    });

    socket.on("message", (raw) => {
      let message: RelayMessage;
      try {
        message = JSON.parse(raw.toString()) as RelayMessage;
      } catch {
        return;
      }

      if (message.type === "register_ok") {
        setRelayAgentOnline(true);
        log(`Relay agent online at ${message.publicUrl ?? relay.url}${message.nodePath ?? ""}`);
        return;
      }
      if (message.type === "register_fail") {
        setRelayAgentOnline(false);
        log(`Relay registration failed: ${message.message ?? "unknown"}`);
        socket?.close(4002, "register failed");
        return;
      }
      if (message.type === "http_req") {
        void handleHttpRequest(message, localPort, socket!);
        return;
      }
      if (message.type === "tunnel_open") {
        openLocalTunnel(message.tunnelId, localPort, socket!);
        return;
      }
      if (message.type === "tunnel_data") {
        const upstream = tunnels.get(message.tunnelId);
        if (!upstream || upstream.readyState !== WebSocket.OPEN) return;
        if (message.binary) {
          upstream.send(Buffer.from(message.data, "base64"));
        } else {
          upstream.send(message.data);
        }
        return;
      }
      if (message.type === "tunnel_close") {
        const upstream = tunnels.get(message.tunnelId);
        tunnels.delete(message.tunnelId);
        upstream?.close(1000, "client closed");
      }
    });

    socket.on("close", () => {
      setRelayAgentOnline(false);
      for (const [id, upstream] of tunnels.entries()) {
        upstream.close();
        tunnels.delete(id);
      }
      if (!stopped) {
        log(`Relay agent disconnected, retry in ${retryMs}ms`);
        setTimeout(connect, retryMs);
        retryMs = Math.min(retryMs * 2, 30_000);
      }
    });

    socket.on("error", (error) => {
      log(`Relay agent socket error: ${error.message}`);
    });
  };

  connect();

  return () => {
    stopped = true;
    socket?.close(1000, "shutdown");
  };
}
