let online = false;

export function setRelayAgentOnline(value: boolean): void {
  online = value;
}

export function isRelayAgentOnline(): boolean {
  return online;
}
