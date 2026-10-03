import koffi from "koffi";

const user32 = koffi.load("user32.dll");

const MouseInput = koffi.struct("MOUSEINPUT", {
  dx: "int32",
  dy: "int32",
  mouseData: "uint32",
  dwFlags: "uint32",
  time: "uint32",
  dwExtraInfo: "uintptr",
});

const KeybdInput = koffi.struct("KEYBDINPUT", {
  wVk: "uint16",
  wScan: "uint16",
  dwFlags: "uint32",
  time: "uint32",
  dwExtraInfo: "uintptr",
});

const HardwareInput = koffi.struct("HARDWAREINPUT", {
  uMsg: "uint32",
  wParamL: "uint16",
  wParamH: "uint16",
});

const InputUnion = koffi.union("INPUTUNION", {
  mi: MouseInput,
  ki: KeybdInput,
  hi: HardwareInput,
});

const Input = koffi.struct("INPUT", {
  type: "uint32",
  u: InputUnion,
});

const SendInput = user32.func("uint32 SendInput(uint32 nInputs, _In_ INPUT *pInputs, int cbSize)");
const SetCursorPos = user32.func("bool SetCursorPos(int X, int Y)");
const GetSystemMetrics = user32.func("int GetSystemMetrics(int nIndex)");

export const INPUT_MOUSE = 0;
export const INPUT_KEYBOARD = 1;
export const MOUSEEVENTF_MOVED = 0x0001;
export const MOUSEEVENTF_LEFTDOWN = 0x0002;
export const MOUSEEVENTF_LEFTUP = 0x0004;
export const MOUSEEVENTF_RIGHTDOWN = 0x0008;
export const MOUSEEVENTF_RIGHTUP = 0x0010;
export const MOUSEEVENTF_MIDDLEDOWN = 0x0020;
export const MOUSEEVENTF_MIDDLEUP = 0x0040;
export const MOUSEEVENTF_WHEEL = 0x0800;
export const MOUSEEVENTF_HWHEEL = 0x1000;
export const MOUSEEVENTF_ABSOLUTE = 0x8000;
export const KEYEVENTF_EXTENDEDKEY = 0x0001;
export const KEYEVENTF_KEYUP = 0x0002;
export const KEYEVENTF_UNICODE = 0x0004;
export const SM_CXSCREEN = 0;
export const SM_CYSCREEN = 1;
export const SM_XVIRTUALSCREEN = 76;
export const SM_YVIRTUALSCREEN = 77;
export const SM_CXVIRTUALSCREEN = 78;
export const SM_CYVIRTUALSCREEN = 79;
export const WHEEL_DELTA = 120;

export interface ScreenBounds {
  x: number;
  y: number;
  width: number;
  height: number;
}

export function virtualScreen(): ScreenBounds {
  const x = GetSystemMetrics(SM_XVIRTUALSCREEN);
  const y = GetSystemMetrics(SM_YVIRTUALSCREEN);
  const width = GetSystemMetrics(SM_CXVIRTUALSCREEN) || GetSystemMetrics(SM_CXSCREEN);
  const height = GetSystemMetrics(SM_CYVIRTUALSCREEN) || GetSystemMetrics(SM_CYSCREEN);
  return { x, y, width, height };
}

export function moveCursor(x: number, y: number): void {
  SetCursorPos(Math.round(x), Math.round(y));
}

export function sendMouseFlags(flags: number, mouseData = 0): void {
  const input = {
    type: INPUT_MOUSE,
    u: {
      mi: {
        dx: 0,
        dy: 0,
        mouseData,
        dwFlags: flags,
        time: 0,
        dwExtraInfo: 0,
      },
    },
  };
  SendInput(1, [input], koffi.sizeof(Input));
}

export function sendKey(vk: number, down: boolean, extended = false): void {
  let flags = down ? 0 : KEYEVENTF_KEYUP;
  if (extended) flags |= KEYEVENTF_EXTENDEDKEY;
  const input = {
    type: INPUT_KEYBOARD,
    u: {
      ki: {
        wVk: vk,
        wScan: 0,
        dwFlags: flags,
        time: 0,
        dwExtraInfo: 0,
      },
    },
  };
  SendInput(1, [input], koffi.sizeof(Input));
}

export function sendUnicodeChar(codeUnit: number, down: boolean): void {
  const flags = KEYEVENTF_UNICODE | (down ? 0 : KEYEVENTF_KEYUP);
  const input = {
    type: INPUT_KEYBOARD,
    u: {
      ki: {
        wVk: 0,
        wScan: codeUnit,
        dwFlags: flags,
        time: 0,
        dwExtraInfo: 0,
      },
    },
  };
  SendInput(1, [input], koffi.sizeof(Input));
}
