import { createHash } from "node:crypto";
import screenshot from "screenshot-desktop";
import sharp from "sharp";

export interface CaptureOptions {
  quality: number;
  maxWidth: number;
}

export interface FrameResult {
  jpeg: Buffer;
  hash: string;
  width: number;
  height: number;
}

export async function captureFrame(options: CaptureOptions): Promise<FrameResult> {
  const raw = (await screenshot({ format: "png" })) as Buffer;
  const image = sharp(raw);
  const meta = await image.metadata();
  const sourceWidth = meta.width ?? options.maxWidth;
  const resized = image.resize({
    width: Math.min(options.maxWidth, sourceWidth),
    withoutEnlargement: true,
  });
  const jpeg = await resized.jpeg({ quality: options.quality, mozjpeg: true }).toBuffer();
  const outMeta = await sharp(jpeg).metadata();
  return {
    jpeg,
    hash: createHash("sha1").update(jpeg).digest("hex"),
    width: outMeta.width ?? 0,
    height: outMeta.height ?? 0,
  };
}
