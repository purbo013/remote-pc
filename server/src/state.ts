import type { WebSocket } from "ws";
import type { AppConfig } from "./config/load.js";
import type { DeviceRecord } from "./security/auth.js";

export interface ConnectedClient {
  socket: WebSocket;
  device: DeviceRecord;
  address: string;
  connectedAt: string;
}

export interface RuntimeState {
  config: AppConfig;
  pairingCode: string;
  startedAt: number;
  running: boolean;
  clients: Map<WebSocket, ConnectedClient>;
}

export function listConnected(state: RuntimeState) {
  return [...state.clients.values()].map((client) => ({
    deviceName: client.device.deviceName,
    address: client.address,
    connectedAt: client.connectedAt,
  }));
}
