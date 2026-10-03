import { randomBytes } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

export interface ProjectEntry {
  name: string;
  url: string;
}

export interface RelayConfig {
  enabled: boolean;
  url: string;
  nodeId: string;
  secret: string;
}

export interface AppConfig {
  port: number;
  host: string;
  remoteControl: boolean;
  screenQuality: number;
  maxFps: number;
  projects: ProjectEntry[];
  relay: RelayConfig;
}

const rootDir = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
export const configPath = join(rootDir, "config.json");
export const tokensPath = join(rootDir, "data", "tokens.json");

const defaultRelay: RelayConfig = {
  enabled: false,
  url: "http://127.0.0.1:8780",
  nodeId: "my-pc",
  secret: "",
};

const defaults: AppConfig = {
  port: 8765,
  host: "0.0.0.0",
  remoteControl: true,
  screenQuality: 70,
  maxFps: 15,
  projects: [{ name: "Default web root", url: "http://{pcIp}/" }],
  relay: { ...defaultRelay },
};

function normalizeRelay(raw: Partial<RelayConfig> | undefined): RelayConfig {
  const relay: RelayConfig = {
    ...defaultRelay,
    ...raw,
  };
  if (!relay.secret) {
    relay.secret = randomBytes(24).toString("hex");
  }
  if (!relay.nodeId.trim()) {
    relay.nodeId = defaultRelay.nodeId;
  }
  return relay;
}

export function loadConfig(): AppConfig {
  if (!existsSync(configPath)) {
    const initial = { ...defaults, relay: normalizeRelay(defaults.relay) };
    writeFileSync(configPath, JSON.stringify(initial, null, 2));
    return { ...initial };
  }
  const raw = JSON.parse(readFileSync(configPath, "utf8")) as Partial<AppConfig>;
  const relay = normalizeRelay(raw.relay);
  const config: AppConfig = {
    ...defaults,
    ...raw,
    projects: Array.isArray(raw.projects) ? raw.projects : defaults.projects,
    relay,
  };
  if (!raw.relay?.secret) {
    writeFileSync(configPath, JSON.stringify(config, null, 2));
  }
  return config;
}

export function saveConfig(config: AppConfig): void {
  writeFileSync(configPath, JSON.stringify(config, null, 2));
}
