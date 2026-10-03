import { networkInterfaces } from "node:os";

export function getLanIPv4(): string[] {
  const nets = networkInterfaces();
  const addresses: string[] = [];
  for (const entries of Object.values(nets)) {
    if (!entries) continue;
    for (const entry of entries) {
      if (entry.internal || entry.family !== "IPv4") continue;
      if (isPrivateIPv4(entry.address)) {
        addresses.push(entry.address);
      }
    }
  }
  return addresses;
}

export function isPrivateIPv4(ip: string): boolean {
  const parts = ip.split(".").map((n) => Number(n));
  if (parts.length !== 4 || parts.some((n) => Number.isNaN(n))) return false;
  const [a, b] = parts;
  if (a === 10) return true;
  if (a === 172 && b >= 16 && b <= 31) return true;
  if (a === 192 && b === 168) return true;
  return false;
}

export function isLoopback(ip: string): boolean {
  return ip === "127.0.0.1" || ip === "::1" || ip === ":1" || ip.endsWith("127.0.0.1");
}

export function clientIp(raw: string | undefined): string {
  if (!raw) return "";
  if (raw.startsWith("::ffff:")) return raw.slice(7);
  return raw;
}
