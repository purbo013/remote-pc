import type { Express, NextFunction, Request, Response } from "express";
import { saveConfig } from "../config/load.js";
import { log } from "../logger.js";
import { clientIp, getLanIPv4, isLoopback } from "../network.js";
import { extractBearer, listDevices, registerDevice, verifyToken } from "../security/auth.js";
import { generatePairingCode } from "../security/pairing.js";
import type { RuntimeState } from "../state.js";
import { listConnected } from "../state.js";

export function registerRoutes(app: Express, state: RuntimeState): void {
  app.get("/health", (_req, res) => {
    res.json({
      ok: true,
      status: state.running ? "running" : "stopped",
      uptimeSec: Math.round((Date.now() - state.startedAt) / 1000),
    });
  });

  app.get("/api/status", (req, res) => {
    const lan = getLanIPv4();
    res.json({
      status: "ONLINE",
      port: state.config.port,
      lanIps: lan,
      remoteControl: state.config.remoteControl,
      connected: listConnected(state),
      pairedDevices: listDevices().map((d) => ({
        deviceName: d.deviceName,
        lastSeenAt: d.lastSeenAt,
      })),
      loopback: isLoopback(clientIp(req.ip)),
    });
  });

  app.post("/api/pair", (req, res) => {
    const code = String(req.body?.code ?? "").replace(/\D/g, "");
    const deviceName = String(req.body?.deviceName ?? "Android Phone");
    if (code !== state.pairingCode) {
      log("Pairing rejected: invalid code");
      res.status(401).json({
        error: "Invalid pairing code",
        hint: "Use the 6-digit code shown on the PC console.",
      });
      return;
    }
    const token = registerDevice(deviceName);
    res.json({ token, deviceName });
  });

  app.get("/api/projects", requireAuth, (req, res) => {
    const pcIp = getLanIPv4()[0] ?? "127.0.0.1";
    const projects = state.config.projects.map((project) => ({
      name: project.name,
      url: project.url.replaceAll("{pcIp}", pcIp),
    }));
    res.json({ projects });
  });

  app.get("/api/me", requireAuth, (req, res) => {
    res.json({ device: req.device });
  });

  app.post("/api/console/remote-control", requireLoopback, (req, res) => {
    state.config.remoteControl = Boolean(req.body?.enabled);
    saveConfig(state.config);
    log(`Remote control ${state.config.remoteControl ? "enabled" : "disabled"}`);
    res.json({ remoteControl: state.config.remoteControl });
  });

  app.post("/api/console/regenerate-code", requireLoopback, (_req, res) => {
    state.pairingCode = generatePairingCode();
    log("Pairing code regenerated");
    res.json({ pairingCode: state.pairingCode });
  });

  app.get("/api/console/state", requireLoopback, (_req, res) => {
    res.json({
      status: "Running",
      ips: getLanIPv4(),
      port: state.config.port,
      pairingCode: state.pairingCode,
      remoteControl: state.config.remoteControl,
      connected: listConnected(state),
    });
  });
}

function requireAuth(req: Request, res: Response, next: NextFunction): void {
  const token = extractBearer(req.header("authorization")) ?? String(req.query.token ?? "");
  const device = verifyToken(token);
  if (!device) {
    res.status(401).json({ error: "Unauthorized" });
    return;
  }
  req.device = device;
  next();
}

function requireLoopback(req: Request, res: Response, next: NextFunction): void {
  if (!isLoopback(clientIp(req.ip))) {
    res.status(403).json({ error: "Console API is loopback-only" });
    return;
  }
  next();
}

declare global {
  namespace Express {
    interface Request {
      device?: import("../security/auth.js").DeviceRecord;
    }
  }
}
