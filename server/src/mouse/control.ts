import {
  MOUSEEVENTF_LEFTDOWN,
  MOUSEEVENTF_LEFTUP,
  MOUSEEVENTF_RIGHTDOWN,
  MOUSEEVENTF_RIGHTUP,
  MOUSEEVENTF_HWHEEL,
  MOUSEEVENTF_WHEEL,
  WHEEL_DELTA,
  moveCursor,
  sendMouseFlags,
  virtualScreen,
} from "../win32.js";

export function moveNormalized(x: number, y: number): void {
  const nx = clamp01(x);
  const ny = clamp01(y);
  const screen = virtualScreen();
  moveCursor(screen.x + nx * (screen.width - 1), screen.y + ny * (screen.height - 1));
}

export function click(button: "left" | "right" | "middle", double = false): void {
  const { down, up } = flagsFor(button);
  sendMouseFlags(down);
  sendMouseFlags(up);
  if (double) {
    sendMouseFlags(down);
    sendMouseFlags(up);
  }
}

export function mouseDown(button: "left" | "right" | "middle"): void {
  sendMouseFlags(flagsFor(button).down);
}

export function mouseUp(button: "left" | "right" | "middle"): void {
  sendMouseFlags(flagsFor(button).up);
}

export function scroll(delta: number): void {
  sendMouseFlags(MOUSEEVENTF_WHEEL, Math.round(delta * WHEEL_DELTA));
}

export function scrollHorizontal(delta: number): void {
  sendMouseFlags(MOUSEEVENTF_HWHEEL, Math.round(delta * WHEEL_DELTA));
}

function flagsFor(button: "left" | "right" | "middle"): { down: number; up: number } {
  if (button === "right") {
    return { down: MOUSEEVENTF_RIGHTDOWN, up: MOUSEEVENTF_RIGHTUP };
  }
  if (button === "middle") {
    return { down: 0x0020, up: 0x0040 };
  }
  return { down: MOUSEEVENTF_LEFTDOWN, up: MOUSEEVENTF_LEFTUP };
}

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(1, Math.max(0, value));
}
