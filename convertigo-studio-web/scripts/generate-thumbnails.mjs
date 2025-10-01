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
	{ position: 0, color: '#243b5c' },
	{ position: 0.55, color: '#315f87' },
	{ position: 1, color: '#48b5a1' }
];
const LOGO_SCALE = 0.42;

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

const hexToRgba = (value) => {
	const hex = value.replace('#', '');
	if (![3, 6].includes(hex.length)) {
		throw new Error(`Unsupported color value: ${value}`);
	}
	if (hex.length === 3) {
		const r = parseInt(hex[0].repeat(2), 16);
		const g = parseInt(hex[1].repeat(2), 16);
		const b = parseInt(hex[2].repeat(2), 16);
		return { r, g, b };
	}
	const r = parseInt(hex.substring(0, 2), 16);
	const g = parseInt(hex.substring(2, 4), 16);
	const b = parseInt(hex.substring(4, 6), 16);
	return { r, g, b };
};

const createVerticalGradient = (width, height, stops) => {
	const channels = 4;
	const buffer = Buffer.alloc(width * height * channels);
	for (let y = 0; y < height; y++) {
		const ratio = height <= 1 ? 0 : y / (height - 1);

		let start = stops[0];
		let end = stops[stops.length - 1];
		for (let i = 0; i < stops.length - 1; i++) {
			if (ratio >= stops[i].position && ratio <= stops[i + 1].position) {
				start = stops[i];
				end = stops[i + 1];
				break;
			}
		}

		const range = end.position - start.position;
		const localRatio = range === 0 ? 0 : (ratio - start.position) / range;

		const r = Math.round(start.r + (end.r - start.r) * localRatio);
		const g = Math.round(start.g + (end.g - start.g) * localRatio);
		const b = Math.round(start.b + (end.b - start.b) * localRatio);

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

	// Use dimensions from Bezels.js as the source of truth for scaling
	const scale = THUMBNAIL_HEIGHT / bezel.height;
	const targetWidth = Math.max(1, Math.round(bezel.width * scale));

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

	// Create screen content (gradient + logo)
	const screenComposites = [];
	const gradientStops = THUMBNAIL_GRADIENT.map(({ position, color }) => ({
		position,
		...hexToRgba(color)
	}));
	const screenGradientBuffer = createVerticalGradient(screenWidth, screenHeight, gradientStops);
	screenComposites.push({
		input: screenGradientBuffer,
		raw: { width: screenWidth, height: screenHeight, channels: 4 },
		top: 0,
		left: 0
	});

	if (existsSync(logoPath)) {
		const logoHeight = clamp(Math.round(screenHeight * LOGO_SCALE), 16, screenHeight);
		const maxLogoWidth = Math.max(16, screenWidth - 8);
		const logo = sharp(logoPath).resize({ height: logoHeight, width: maxLogoWidth, fit: 'inside' });

		const resizedLogoBuffer = await logo.toBuffer();
		const { width: logoWidth = 0, height: actualLogoHeight = 0 } =
			await sharp(resizedLogoBuffer).metadata();

		const logoLeft = Math.round((screenWidth - logoWidth) / 2);
		const logoTop = Math.round((screenHeight - actualLogoHeight) / 2);

		screenComposites.push({ input: resizedLogoBuffer, left: logoLeft, top: logoTop });
	}

	const screenContent = await sharp({
		create: {
			width: screenWidth,
			height: screenHeight,
			channels: 4,
			background: { r: 0, g: 0, b: 0, alpha: 0 }
		}
	})
		.composite(screenComposites)
		.raw()
		.toBuffer();

	// Create rounded corner mask
	const scaledRadius = (iframe.borderRadius ?? 0) * scale;
	const mask = Buffer.from(
		`<svg><rect x="0" y="0" width="${screenWidth}" height="${screenHeight}" rx="${scaledRadius}" ry="${scaledRadius}"/></svg>`
	);

	const maskedScreen = await sharp(screenContent, {
		raw: { width: screenWidth, height: screenHeight, channels: 4 }
	})
		.composite([
			{
				input: mask,
				blend: 'dest-in'
			}
		])
		.raw()
		.toBuffer();

	// Composite final image
	const finalComposites = [];
	finalComposites.push({
		input: maskedScreen,
		raw: { width: screenWidth, height: screenHeight, channels: 4 },
		left: screenLeft,
		top: screenTop
	});

	const frameBuffer = await bezelImage.resize({ height: THUMBNAIL_HEIGHT }).toBuffer();
	finalComposites.push({ input: frameBuffer, left: 0, top: 0 });

	const thumbPath = path.join(thumbsDir, `${id}.webp`);
	await base.composite(finalComposites).webp({ quality: 85 }).toFile(thumbPath);

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
