#!/usr/bin/env node
import { existsSync } from 'fs';
import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath, pathToFileURL } from 'url';
import sharp from 'sharp';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

const bezelsDir = path.join(rootDir, 'static', 'bezels');
const thumbsDir = path.join(bezelsDir, 'thumbnails');
const logoPath = path.join(rootDir, 'static', 'logo.png');
const bezelsModulePath = pathToFileURL(
	path.join(rootDir, 'src', 'lib', 'dashboard', 'Bezels.js')
).href;

const THUMBNAIL_HEIGHT = 240;
const THUMBNAIL_GRADIENT = [
	{ position: 0, color: '#0f704f' },
	{ position: 1, color: '#7a22b6' }
];
const LOGO_SCALE = 0.42;

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

const hexToRgba = (value) => {
	const hex = value.replace('#', '');
	if (![3, 6].includes(hex.length)) {
		throw new Error(`Unsupported color value: ${value}`);
	}
	const expand = (component) => (hex.length === 3 ? component.repeat(2) : component);
	const r = parseInt(expand(hex[0]), 16);
	const g = parseInt(expand(hex[1]), 16);
	const b = parseInt(expand(hex[2]), 16);
	return { r, g, b };
};

const createVerticalGradient = (width, height, stops) => {
	const channels = 4;
	const buffer = Buffer.alloc(width * height * channels);
	for (let y = 0; y < height; y++) {
		const ratio = height <= 1 ? 0 : y / (height - 1);
		const start = stops[0];
		const end = stops[stops.length - 1];
		const r = Math.round(start.r + (end.r - start.r) * ratio);
		const g = Math.round(start.g + (end.g - start.g) * ratio);
		const b = Math.round(start.b + (end.b - start.b) * ratio);
		for (let x = 0; x < width; x++) {
			const offset = (y * width + x) * channels;
			buffer[offset] = r;
			buffer[offset + 1] = g;
			buffer[offset + 2] = b;
			buffer[offset + 3] = 255;
		}
	}
	return buffer;
};

async function ensureThumbsDir() {
	await fs.mkdir(thumbsDir, { recursive: true });
}

async function loadDevices() {
	const { default: devices } = await import(bezelsModulePath);
	return Object.values(devices).filter(({ id }) => id && id !== 'none');
}

async function generateThumbnail({ id, iframe, bezel }) {
	const bezelPath = path.join(bezelsDir, `${id}.webp`);
	if (!existsSync(bezelPath)) {
		console.warn(`⚠️ Skipping ${id}: bezel image not found at ${bezelPath}`);
		return;
	}

	const bezelImage = sharp(bezelPath);
	const bezelMeta = await bezelImage.metadata();
	if (!bezelMeta.width || !bezelMeta.height) {
		throw new Error(`Missing dimensions for bezel ${id}`);
	}

	const scale = THUMBNAIL_HEIGHT / bezelMeta.height;
	const targetWidth = Math.max(1, Math.round(bezelMeta.width * scale));
	const base = sharp({
		create: {
			width: targetWidth,
			height: THUMBNAIL_HEIGHT,
			channels: 4,
			background: { r: 0, g: 0, b: 0, alpha: 0 }
		}
	});

	const rawScreenWidth = Math.round((iframe?.width ?? 0) * scale);
	const rawScreenHeight = Math.round((iframe?.height ?? 0) * scale);
	const rawScreenLeft = Math.round((iframe?.marginLeft ?? 0) * scale);
	const rawScreenTop = Math.round((iframe?.marginTop ?? 0) * scale);

	const screenLeft = clamp(rawScreenLeft, 0, targetWidth - 1);
	const screenTop = clamp(rawScreenTop, 0, THUMBNAIL_HEIGHT - 1);
	const screenWidth = clamp(rawScreenWidth, 1, targetWidth - screenLeft);
	const screenHeight = clamp(rawScreenHeight, 1, THUMBNAIL_HEIGHT - screenTop);

	const gradientStops = THUMBNAIL_GRADIENT.map(({ position, color }) => ({
		position,
		...hexToRgba(color)
	}));
	const gradientBuffer = createVerticalGradient(screenWidth, screenHeight, gradientStops);

	const composites = [];
	composites.push({
		input: gradientBuffer,
		left: screenLeft,
		top: screenTop,
		raw: { width: screenWidth, height: screenHeight, channels: 4 }
	});

	if (existsSync(logoPath)) {
		const logoHeight = clamp(Math.round(screenHeight * LOGO_SCALE), 16, screenHeight);
		const maxLogoWidth = Math.max(16, screenWidth - 8);
		const logo = sharp(logoPath).resize({ height: logoHeight, width: maxLogoWidth, fit: 'inside' });
		const { width: logoWidth = 0, height: actualLogoHeight = 0 } = await logo.metadata();
		const logoLeft = clamp(
			screenLeft + Math.round((screenWidth - logoWidth) / 2),
			screenLeft,
			screenLeft + screenWidth - logoWidth
		);
		const logoTop = clamp(
			screenTop + Math.round((screenHeight - actualLogoHeight) / 2),
			screenTop,
			screenTop + screenHeight - actualLogoHeight
		);
		composites.push({ input: await logo.toBuffer(), left: logoLeft, top: logoTop });
	}

	const frameBuffer = await bezelImage.resize({ height: THUMBNAIL_HEIGHT }).toBuffer();
	composites.push({ input: frameBuffer, left: 0, top: 0 });

	const thumbPath = path.join(thumbsDir, `${id}.webp`);
	await base.composite(composites).webp({ quality: 85 }).toFile(thumbPath);

	console.log(`✓ Generated thumbnail for ${id}`);
}

async function main() {
	await ensureThumbsDir();
	const devices = await loadDevices();
	for (const device of devices) {
		try {
			await generateThumbnail(device);
		} catch (error) {
			console.error(`✗ Failed to generate thumbnail for ${device.id}:`, error.message);
		}
	}
}

main().catch((error) => {
	console.error(error);
	process.exit(1);
});
