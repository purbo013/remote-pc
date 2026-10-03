import { sendKey, sendUnicodeChar } from "../win32.js";

const VK: Record<string, number> = {
  BACKSPACE: 0x08,
  TAB: 0x09,
  ENTER: 0x0d,
  SHIFT: 0x10,
  CTRL: 0x11,
  CONTROL: 0x11,
  ALT: 0x12,
  PAUSE: 0x13,
  CAPSLOCK: 0x14,
  ESC: 0x1b,
  ESCAPE: 0x1b,
  SPACE: 0x20,
  PAGEUP: 0x21,
  PAGEDOWN: 0x22,
  END: 0x23,
  HOME: 0x24,
  LEFT: 0x25,
  ARROWLEFT: 0x25,
  UP: 0x26,
  ARROWUP: 0x26,
  RIGHT: 0x27,
  ARROWRIGHT: 0x27,
  DOWN: 0x28,
  ARROWDOWN: 0x28,
  DELETE: 0x2e,
  DEL: 0x2e,
  WIN: 0x5b,
  LWIN: 0x5b,
  META: 0x5b,
  F1: 0x70,
  F2: 0x71,
  F3: 0x72,
  F4: 0x73,
  F5: 0x74,
  F6: 0x75,
  F7: 0x76,
  F8: 0x77,
  F9: 0x78,
  F10: 0x79,
  F11: 0x7a,
  F12: 0x7b,
  "`": 0xc0,
  "~": 0xc0,
  OEM3: 0xc0,
  BACKTICK: 0xc0,
};

const EXTENDED = new Set([
  "LEFT",
  "ARROWLEFT",
  "RIGHT",
  "ARROWRIGHT",
  "UP",
  "ARROWUP",
  "DOWN",
  "ARROWDOWN",
  "DELETE",
  "DEL",
  "HOME",
  "END",
  "PAGEUP",
  "PAGEDOWN",
]);

export function pressKey(name: string): void {
  const vk = resolveVk(name);
  if (vk === null) {
    if (name.length === 1) {
      typeText(name);
    }
    return;
  }
  const extended = EXTENDED.has(normalize(name));
  sendKey(vk, true, extended);
  sendKey(vk, false, extended);
}

export function keyCombo(keys: string[]): void {
  const resolved = keys
    .map((key) => ({ key, vk: resolveVk(key), extended: EXTENDED.has(normalize(key)) }))
    .filter((item) => item.vk !== null) as { key: string; vk: number; extended: boolean }[];

  for (const item of resolved) sendKey(item.vk, true, item.extended);
  for (const item of [...resolved].reverse()) sendKey(item.vk, false, item.extended);
}

export function typeText(text: string): void {
  for (const char of text) {
    const code = char.codePointAt(0);
    if (code === undefined) continue;
    if (code <= 0xffff) {
      sendUnicodeChar(code, true);
      sendUnicodeChar(code, false);
    } else {
      const adjusted = code - 0x10000;
      const high = 0xd800 + ((adjusted >> 10) & 0x3ff);
      const low = 0xdc00 + (adjusted & 0x3ff);
      sendUnicodeChar(high, true);
      sendUnicodeChar(high, false);
      sendUnicodeChar(low, true);
      sendUnicodeChar(low, false);
    }
  }
}

function resolveVk(name: string): number | null {
  const key = normalize(name);
  if (VK[key] !== undefined) return VK[key];
  if (key.length === 1) {
    const ch = key.toUpperCase();
    const code = ch.charCodeAt(0);
    if (code >= 0x30 && code <= 0x39) return code;
    if (code >= 0x41 && code <= 0x5a) return code;
  }
  return null;
}

function normalize(name: string): string {
  return name.trim().toUpperCase().replace(/[\s-]+/g, "");
}
