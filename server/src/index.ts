import { exec } from "node:child_process";
import express from "express";
import { createServer } from "node:http";
import { WebSocketServer } from "ws";
import { registerRoutes } from "./api/routes.js";
import { loadConfig } from "./config/load.js";
import { consoleHtml } from "./console/page.js";
import { log } from "./logger.js";
import { clientIp, getLanIPv4, isLoopback } from "./network.js";
import { generatePairingCode } from "./security/pairing.js";
import type { RuntimeState } from "./state.js";
import { startRelayAgent } from "./relay/agent.js";
import { attachWebsocket } from "./websocket/handler.js";

const config = loadConfig();
const state: RuntimeState = {
  config,
  pairingCode: generatePairingCode(),
  startedAt: Date.now(),
  running: true,
  clients: new Map(),
};

const app = express();
app.set("trust proxy", false);
app.use(express.json({ limit: "256kb" }));

app.use("/console", (req, res, next) => {
  if (!isLoopback(clientIp(req.ip))) {
    res.status(403).type("text").send("Console is available on this PC only (127.0.0.1).");
    return;
  }
  next();
});

app.get("/console", (_req, res) => {
  res.type("html").send(consoleHtml);
});

registerRoutes(app, state);

app.use((error: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const message = error instanceof Error ? error.message : "Request failed";
  res.status(400).json({ error: message });
});

const httpServer = createServer(app);
const wss = new WebSocketServer({ server: httpServer, path: "/ws" });
attachWebsocket(wss, state);

const stopRelay = startRelayAgent({ relay: config.relay, localPort: config.port });
process.on("SIGINT", () => stopRelay());

httpServer.listen(config.port, config.host, () => {
  const ips = getLanIPv4();
  log("Server started");
  log(`Listening on ${config.host}:${config.port}`);
  log(`LAN IPs: ${ips.join(", ") || "none detected"}`);
  log("Pairing code is displayed on the local console only");
  log("Open http://127.0.0.1:" + config.port + "/console on this PC");
  if (config.relay.enabled) {
    log(`Internet relay: nodeId=${config.relay.nodeId} → ${config.relay.url}`);
  }
  openConsole(config.port);
});

function openConsole(port: number): void {
  const url = `http://127.0.0.1:${port}/console`;
  exec(`cmd /c start "" "${url}"`);
}
