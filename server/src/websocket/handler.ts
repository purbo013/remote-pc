import type { IncomingMessage } from "node:http";
import { WebSocket, WebSocketServer } from "ws";
import { click, mouseDown, mouseUp, moveNormalized, scroll, scrollHorizontal } from "../mouse/control.js";
import { keyCombo, pressKey, typeText } from "../keyboard/control.js";
import { log } from "../logger.js";
import { clientIp } from "../network.js";
import { captureFrame } from "../screen/capture.js";
import { verifyToken } from "../security/auth.js";
import type { RuntimeState } from "../state.js";

interface ClientMessage {
  type?: string;
  token?: string;
  x?: number;
  y?: number;
  button?: "left" | "right" | "middle";
  double?: boolean;
  delta?: number;
  deltaX?: number;
  key?: string;
  keys?: string[];
  text?: string;
}

export function attachWebsocket(wss: WebSocketServer, state: RuntimeState): void {
  wss.on("connection", (socket, request) => {
    handleConnection(socket, request, state);
  });
}

function handleConnection(socket: WebSocket, request: IncomingMessage, state: RuntimeState): void {
  const address = clientIp(request.socket.remoteAddress);
  log(`Client connected from ${address || "unknown"}`);
  let authed = false;
  let lastHash = "";
  let streamTimer: NodeJS.Timeout | undefined;

  const timeout = setTimeout(() => {
    if (!authed) {
      safeSend(socket, { type: "error", message: "Authentication timeout" });
      socket.close(4001, "auth timeout");
    }
  }, 10_000);

  socket.on("message", (raw) => {
    let message: ClientMessage;
    try {
      message = JSON.parse(raw.toString()) as ClientMessage;
    } catch {
      safeSend(socket, { type: "error", message: "Invalid JSON" });
      return;
    }

    if (!authed) {
      if (message.type !== "auth" || !verifyToken(message.token)) {
        log("Rejected unauthorized WebSocket client");
        safeSend(socket, { type: "error", message: "Unauthorized" });
        socket.close(4003, "unauthorized");
        return;
      }
      const device = verifyToken(message.token)!;
      authed = true;
      clearTimeout(timeout);
      state.clients.set(socket, {
        socket,
        device,
        address,
        connectedAt: new Date().toISOString(),
      });
      log(`Authentication successful (${device.deviceName})`);
      safeSend(socket, {
        type: "hello",
        remoteControl: state.config.remoteControl,
        maxFps: state.config.maxFps,
      });
      streamTimer = startScreenStream(socket, state, () => lastHash, (hash) => {
        lastHash = hash;
      });
      return;
    }

    handleControl(socket, state, message);
  });

  socket.on("close", () => {
    clearTimeout(timeout);
    if (streamTimer) clearInterval(streamTimer);
    state.clients.delete(socket);
    log("Client disconnected");
  });
}

function handleControl(socket: WebSocket, state: RuntimeState, message: ClientMessage): void {
  const controlTypes = new Set([
    "mouse_move",
    "mouse_click",
    "mouse_down",
    "mouse_up",
    "scroll",
    "key",
    "key_combo",
    "text_input",
  ]);
  if (message.type && controlTypes.has(message.type) && !state.config.remoteControl) {
    safeSend(socket, { type: "error", message: "Remote control is disabled" });
    return;
  }

  switch (message.type) {
    case "mouse_move":
      if (typeof message.x === "number" && typeof message.y === "number") {
        moveNormalized(message.x, message.y);
      }
      break;
    case "mouse_click":
      click(message.button ?? "left", Boolean(message.double));
      break;
    case "mouse_down":
      mouseDown(message.button ?? "left");
      break;
    case "mouse_up":
      mouseUp(message.button ?? "left");
      break;
    case "scroll":
      if (typeof message.delta === "number" && message.delta !== 0) {
        scroll(message.delta);
      }
      if (typeof message.deltaX === "number" && message.deltaX !== 0) {
        scrollHorizontal(message.deltaX);
      }
      break;
    case "key":
      if (message.key) pressKey(message.key);
      break;
    case "key_combo":
      if (Array.isArray(message.keys)) keyCombo(message.keys);
      break;
    case "text_input":
      if (typeof message.text === "string") typeText(message.text);
      break;
    case "ping":
      safeSend(socket, { type: "pong" });
      break;
    default:
      break;
  }
}

function startScreenStream(
  socket: WebSocket,
  state: RuntimeState,
  getHash: () => string,
  setHash: (hash: string) => void,
): NodeJS.Timeout {
  const interval = Math.max(33, Math.round(1000 / Math.max(1, state.config.maxFps)));
  let busy = false;
  return setInterval(() => {
    if (busy || socket.readyState !== WebSocket.OPEN) return;
    busy = true;
    captureFrame({
      quality: state.config.screenQuality,
      maxWidth: 1280,
    })
      .then((frame) => {
        if (frame.hash === getHash()) return;
        setHash(frame.hash);
        safeSend(socket, {
          type: "screen_frame",
          mime: "image/jpeg",
          width: frame.width,
          height: frame.height,
          data: frame.jpeg.toString("base64"),
        });
      })
      .catch((error: unknown) => {
        log(`Screen capture failed: ${error instanceof Error ? error.message : String(error)}`);
      })
      .finally(() => {
        busy = false;
      });
  }, interval);
}

export function safeSend(socket: WebSocket, payload: unknown): void {
  if (socket.readyState !== WebSocket.OPEN) return;
  socket.send(JSON.stringify(payload));
}
