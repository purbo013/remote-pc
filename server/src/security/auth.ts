import { createHash, randomBytes } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";
import { tokensPath } from "../config/load.js";
import { log } from "../logger.js";

export interface DeviceRecord {
  tokenHash: string;
  deviceName: string;
  createdAt: string;
  lastSeenAt: string;
}

export interface AuthStore {
  devices: DeviceRecord[];
}

function emptyStore(): AuthStore {
  return { devices: [] };
}

function readStore(): AuthStore {
  if (!existsSync(tokensPath)) return emptyStore();
  try {
    return JSON.parse(readFileSync(tokensPath, "utf8")) as AuthStore;
  } catch {
    return emptyStore();
  }
}

function writeStore(store: AuthStore): void {
  mkdirSync(dirname(tokensPath), { recursive: true });
  writeFileSync(tokensPath, JSON.stringify(store, null, 2));
}

export function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export function createToken(): string {
  return randomBytes(32).toString("hex");
}

export function registerDevice(deviceName: string): string {
  const token = createToken();
  const store = readStore();
  const now = new Date().toISOString();
  store.devices.push({
    tokenHash: hashToken(token),
    deviceName: deviceName.trim() || "Android Phone",
    createdAt: now,
    lastSeenAt: now,
  });
  writeStore(store);
  log(`Authentication successful for ${deviceName.trim() || "Android Phone"}`);
  return token;
}

export function verifyToken(token: string | undefined): DeviceRecord | null {
  if (!token) return null;
  const hash = hashToken(token);
  const store = readStore();
  const device = store.devices.find((d) => d.tokenHash === hash);
  if (!device) return null;
  device.lastSeenAt = new Date().toISOString();
  writeStore(store);
  return device;
}

export function listDevices(): DeviceRecord[] {
  return readStore().devices;
}

export function extractBearer(header: string | undefined): string | undefined {
  if (!header) return undefined;
  const [scheme, value] = header.split(" ");
  if (scheme?.toLowerCase() !== "bearer" || !value) return undefined;
  return value;
}
