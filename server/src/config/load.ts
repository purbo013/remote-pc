import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

export interface ProjectEntry {
  name: string;
  url: string;
}

export interface AppConfig {
  port: number;
  host: string;
  remoteControl: boolean;
  screenQuality: number;
  maxFps: number;
  projects: ProjectEntry[];
}

const rootDir = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
export const configPath = join(rootDir, "config.json");
export const tokensPath = join(rootDir, "data", "tokens.json");

const defaults: AppConfig = {
  port: 8765,
  host: "0.0.0.0",
  remoteControl: true,
  screenQuality: 70,
  maxFps: 15,
  projects: [{ name: "Default web root", url: "http://{pcIp}/" }],
};

export function loadConfig(): AppConfig {
  if (!existsSync(configPath)) {
    writeFileSync(configPath, JSON.stringify(defaults, null, 2));
    return { ...defaults };
  }
  const raw = JSON.parse(readFileSync(configPath, "utf8")) as Partial<AppConfig>;
  return {
    ...defaults,
    ...raw,
    projects: Array.isArray(raw.projects) ? raw.projects : defaults.projects,
  };
}

export function saveConfig(config: AppConfig): void {
  writeFileSync(configPath, JSON.stringify(config, null, 2));
}
