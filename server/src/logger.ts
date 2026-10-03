export function log(message: string): void {
  const now = new Date();
  const stamp = now.toTimeString().slice(0, 8);
  console.log(`[${stamp}] ${message}`);
}
