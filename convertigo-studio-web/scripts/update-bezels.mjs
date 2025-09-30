#!/usr/bin/env node
import { execSync } from 'child_process';
import { existsSync } from 'fs';
import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import sharp from 'sharp';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

const configPath = path.join(rootDir, 'config', 'device-catalog.json');
const staticDir = path.join(rootDir, 'static', 'bezels');
const thumbsDir = path.join(staticDir, 'thumbnails');
const logoPath = path.join(rootDir, 'static', 'logo.png');
const cacheDir = path.join(rootDir, '.cache');
const repoDir = path.join(cacheDir, 'mockup-device-frames');
const repoUrl = 'https://github.com/jamesjingyi/mockup-device-frames.git';

const OUTPUT_IMAGE_EXT = '.webp';

const THUMBNAIL_GRADIENT = [
	{ position: 0, color: '#02112c' },
	{ position: 0.52, color: '#0f45c0' },
	{ position: 1, color: '#38b8ff' }
];

const identifierRegex = /^[A-Za-z_$][0-9A-Za-z_$]*$/;

const hexToRgba = (color) => {
	const hex = color.replace('#', '');
	if (![3, 6, 8].includes(hex.length)) {
		throw new Error(`Unsupported color format: ${color}`);
	}
	const expand = (value) => (hex.length === 3 ? value.repeat(2) : value);
	const normalized =
		hex.length === 8
			? hex
			: hex.length === 6
				? `${hex}ff`
				: `${expand(hex[0])}${expand(hex[1])}${expand(hex[2])}ff`;
	const numeric = Number.parseInt(normalized, 16);
	return {
		r: (numeric >> 24) & 0xff,
		g: (numeric >> 16) & 0xff,
		b: (numeric >> 8) & 0xff,
		a: numeric & 0xff
	};
};

const createGradientBackground = (width, height, stops) => {
	const sortedStops = stops
		.map(({ position, color }) => ({ position, rgba: hexToRgba(color) }))
		.sort((a, b) => a.position - b.position);
	if (sortedStops.length < 2) {
		throw new Error('At least two color stops are required for the gradient background.');
	}
	const channels = 4;
	const data = Buffer.alloc(width * height * channels);
	for (let y = 0; y < height; y++) {
		const ratio = height <= 1 ? 0 : y / (height - 1);
		let start = sortedStops[0];
		let end = sortedStops[sortedStops.length - 1];
		for (let i = 0; i < sortedStops.length - 1; i++) {
			const current = sortedStops[i];
			const next = sortedStops[i + 1];
			if (ratio >= current.position && ratio <= next.position) {
				start = current;
				end = next;
				break;
			}
			if (ratio < sortedStops[0].position) {
				end = sortedStops[0];
				break;
			}
			if (ratio > sortedStops[sortedStops.length - 1].position) {
				start = sortedStops[sortedStops.length - 1];
				break;
			}
		}
		const span = Math.max(end.position - start.position, 1e-6);
		const localRatio = Math.min(Math.max((ratio - start.position) / span, 0), 1);
		const r = Math.round(start.rgba.r + (end.rgba.r - start.rgba.r) * localRatio);
		const g = Math.round(start.rgba.g + (end.rgba.g - start.rgba.g) * localRatio);
		const b = Math.round(start.rgba.b + (end.rgba.b - start.rgba.b) * localRatio);
		for (let x = 0; x < width; x++) {
			const offset = (y * width + x) * channels;
			data[offset] = r;
			data[offset + 1] = g;
			data[offset + 2] = b;
			data[offset + 3] = 255;
		}
	}
	return sharp(data, { raw: { width, height, channels } });
};

const createGlow = (width, height, color, { opacity = 0.45, exponent = 2.2 } = {}) => {
	const { r, g, b, a } = hexToRgba(color);
	const baseAlpha = ((a ?? 255) / 255) * opacity;
	const channels = 4;
	const data = Buffer.alloc(width * height * channels);
	const centerX = (width - 1) / 2;
	const centerY = (height - 1) / 2;
	const radiusX = Math.max(width / 2, 1);
	const radiusY = Math.max(height / 2, 1);
	for (let y = 0; y < height; y++) {
		for (let x = 0; x < width; x++) {
			const dx = (x - centerX) / radiusX;
			const dy = (y - centerY) / radiusY;
			const distance = Math.sqrt(dx * dx + dy * dy);
			const fade = Math.pow(Math.max(0, 1 - distance), exponent);
			const alpha = Math.max(0, Math.min(1, baseAlpha * fade));
			const offset = (y * width + x) * channels;
			data[offset] = r;
			data[offset + 1] = g;
			data[offset + 2] = b;
			data[offset + 3] = Math.round(alpha * 255);
		}
	}
	return sharp(data, { raw: { width, height, channels } });
};

const toLiteral = (value, depth = 1) => {
	const pad = '\t'.repeat(depth);
	if (Array.isArray(value)) {
		if (!value.length) return '[]';
		const inner = value.map((item) => `${pad}\t${toLiteral(item, depth + 1)}`).join(',\n');
		return `[\n${inner}\n${pad}]`;
	}
	if (value && typeof value === 'object') {
		const entries = Object.entries(value)
			.filter(([, v]) => v !== undefined)
			.map(([key, v]) => {
				const safeKey = identifierRegex.test(key) ? key : `'${key}'`;
				return `${pad}\t${safeKey}: ${toLiteral(v, depth + 1)}`;
			});
		if (!entries.length) return '{}';
		return `{
${entries.join(',\n')}\n${pad}}`;
	}
	if (typeof value === 'string') {
		return `'${value.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`;
	}
	if (typeof value === 'number' && Number.isFinite(value)) {
		return String(value);
	}
	if (typeof value === 'boolean') return value ? 'true' : 'false';
	return 'null';
};

const readCatalog = async () => {
	const raw = await fs.readFile(configPath, 'utf8');
	return JSON.parse(raw);
};

const needsRepo = (catalog) =>
	catalog.some((spec) => !spec.source.startsWith('assets/') && !spec.source.startsWith('static/'));

const ensureRepo = async () => {
	await fs.mkdir(cacheDir, { recursive: true });
	if (!existsSync(repoDir)) {
		console.log('Cloning device frames...');
		execSync(`git clone --depth 1 ${repoUrl} ${repoDir}`, { stdio: 'inherit' });
		return;
	}
	console.log('Updating device frames...');
	execSync('git fetch -q origin main', { cwd: repoDir, stdio: 'inherit' });
	execSync('git reset --hard origin/main', { cwd: repoDir, stdio: 'inherit' });
};

const analyzeTransparency = ({ data, width, height, channels }) => {
	const threshold = 10;
	const mask = new Uint8Array(width * height);
	for (let y = 0; y < height; y++) {
		for (let x = 0; x < width; x++) {
			const alpha = data[(y * width + x) * channels + (channels - 1)];
			if (alpha <= threshold) mask[y * width + x] = 1;
		}
	}

	const visited = new Uint8Array(mask.length);
	const stack = [];
	let bounds = null;
	for (let y = 0; y < height && !bounds; y++) {
		for (let x = 0; x < width && !bounds; x++) {
			const pos = y * width + x;
			if (!mask[pos] || visited[pos]) continue;
			stack.push(pos);
			visited[pos] = 1;
			let minRow = y;
			let maxRow = y;
			let minCol = x;
			let maxCol = x;
			let touchesEdge = false;
			while (stack.length) {
				const current = stack.pop();
				const cy = Math.trunc(current / width);
				const cx = current - cy * width;
				if (cy < minRow) minRow = cy;
				if (cy > maxRow) maxRow = cy;
				if (cx < minCol) minCol = cx;
				if (cx > maxCol) maxCol = cx;
				if (cy === 0 || cx === 0 || cy === height - 1 || cx === width - 1) touchesEdge = true;

				const neighbours = [current - width, current + width, current - 1, current + 1];
				for (const next of neighbours) {
					if (next < 0 || next >= mask.length) continue;
					if (!mask[next] || visited[next]) continue;
					visited[next] = 1;
					stack.push(next);
				}
			}
			if (!touchesEdge) bounds = { minRow, maxRow, minCol, maxCol };
		}
	}
	if (!bounds) throw new Error('Unable to detect screen cut-out');

	const { minRow, maxRow, minCol, maxCol } = bounds;
	const screenWidth = maxCol - minCol + 1;
	const screenHeight = maxRow - minRow + 1;

	let radius = 0;
	const sampleDepth = Math.min(160, Math.floor(screenHeight / 3));
	for (let row = minRow; row < minRow + sampleDepth; row++) {
		let first = -1;
		for (let col = minCol; col <= maxCol; col++) {
			if (mask[row * width + col]) {
				first = col;
				break;
			}
		}
		if (first === -1) continue;
		const offset = first - minCol;
		if (offset > radius) radius = offset;
	}

	return {
		pngWidth: width,
		pngHeight: height,
		screenWidth,
		screenHeight,
		paddingLeft: minCol,
		paddingTop: minRow,
		paddingRight: width - maxCol - 1,
		paddingBottom: height - maxRow - 1,
		radiusPx: radius
	};
};

const readMetrics = async (imagePath) => {
	const { data, info } = await sharp(imagePath)
		.ensureAlpha()
		.raw()
		.toBuffer({ resolveWithObject: true });
	return analyzeTransparency({
		data,
		width: info.width,
		height: info.height,
		channels: info.channels
	});
};

const scaleMetrics = (metrics, targetWidth) => {
	const scale = metrics.screenWidth / targetWidth;
	const round = (value) => Math.round(value / scale);
	return {
		iframeWidth: targetWidth,
		iframeHeight: round(metrics.screenHeight),
		marginTop: round(metrics.paddingTop),
		marginLeft: round(metrics.paddingLeft),
		marginBottom: round(metrics.paddingBottom),
		marginRight: round(metrics.paddingRight),
		bezelWidth: round(metrics.pngWidth),
		bezelHeight: round(metrics.pngHeight),
		borderRadius: round(metrics.radiusPx)
	};
};

const resolveSourcePath = (spec) => {
	if (spec.source.startsWith('assets/') || spec.source.startsWith('static/')) {
		return path.join(rootDir, spec.source);
	}
	return path.join(repoDir, spec.source);
};

async function main() {
	const catalog = await readCatalog();
	await fs.mkdir(staticDir, { recursive: true });
	await fs.mkdir(thumbsDir, { recursive: true });

	if (needsRepo(catalog)) await ensureRepo();

	const usedImages = new Set();
	const usedThumbs = new Set();
	const devices = {};
	let index = 0;

	for (const spec of catalog) {
		const sourcePath = resolveSourcePath(spec);
		if (!existsSync(sourcePath)) {
			throw new Error(`Missing source for ${spec.id}: ${sourcePath}`);
		}

		const metrics = await readMetrics(sourcePath);
		const scaled = scaleMetrics(metrics, spec.viewport.width);

		const targetWebp = path.join(staticDir, `${spec.id}${OUTPUT_IMAGE_EXT}`);
		await sharp(sourcePath).webp({ quality: 95 }).toFile(targetWebp);
		usedImages.add(targetWebp);

		const thumbPath = path.join(thumbsDir, `${spec.id}${OUTPUT_IMAGE_EXT}`);
		const frame = sharp(targetWebp);
		const { width: frameWidth, height: frameHeight } = await frame.metadata();
		const targetHeight = 240;
		const scale = targetHeight / frameHeight;
		const targetWidth = Math.round(frameWidth * scale);
		const canvasWidth = Math.max(targetWidth, Math.round(targetHeight * 0.65));
		const canvasHeight = targetHeight;

		const background = sharp({
			create: {
				width: canvasWidth,
				height: canvasHeight,
				channels: 4,
				background: { r: 0, g: 0, b: 0, alpha: 0 }
			}
		});

		const frameBuffer = await frame.resize({ height: targetHeight }).toBuffer();
		const frameX = Math.round((canvasWidth - targetWidth) / 2);
		const frameComposite = { input: frameBuffer, left: frameX, top: 0 };

		const hasLogo = existsSync(logoPath);
		let screenWidth = Math.max(0, Math.round(scaled.iframeWidth));
		let screenHeight = Math.max(0, Math.round(scaled.iframeHeight));
		let screenLeft = frameX + Math.round(scaled.marginLeft);
		let screenTop = Math.round(scaled.marginTop);

		if (screenLeft < 0) {
			screenWidth += screenLeft;
			screenLeft = 0;
		}
		if (screenTop < 0) {
			screenHeight += screenTop;
			screenTop = 0;
		}
		screenWidth = Math.max(0, Math.min(screenWidth, canvasWidth - screenLeft));
		screenHeight = Math.max(0, Math.min(screenHeight, canvasHeight - screenTop));

		const composites = [];

		if (screenWidth > 0 && screenHeight > 0) {
			const gradientBuffer = await createGradientBackground(
				screenWidth,
				screenHeight,
				THUMBNAIL_GRADIENT
			)
				.png()
				.toBuffer();
			composites.push({ input: gradientBuffer, left: screenLeft, top: screenTop });

			const accentWidth = Math.max(1, Math.min(screenWidth, Math.round(screenWidth * 0.78)));
			const accentHeight = Math.max(1, Math.min(screenHeight, Math.round(screenHeight * 0.62)));
			const accentGlowBuffer = await createGlow(accentWidth, accentHeight, '#46d5ff', {
				opacity: 0.28,
				exponent: 1.8
			})
				.png()
				.toBuffer();
			const accentX = Math.max(
				0,
				Math.min(
					canvasWidth - accentWidth,
					screenLeft + Math.round((screenWidth - accentWidth) / 2)
				)
			);
			const accentY = Math.max(
				0,
				Math.min(canvasHeight - accentHeight, screenTop + Math.round(screenHeight * 0.15))
			);
			composites.push({ input: accentGlowBuffer, left: accentX, top: accentY, blend: 'screen' });

			if (hasLogo) {
				const logoHeight = Math.max(1, Math.round(screenHeight * 0.22));
				const logo = sharp(logoPath).resize({ height: logoHeight, fit: 'inside' });
				const { width: logoW = 0, height: logoH = 0 } = await logo.metadata();
				const logoX = Math.max(
					0,
					Math.min(canvasWidth - logoW, screenLeft + Math.round((screenWidth - logoW) / 2))
				);
				const logoYBase = screenTop + Math.round(screenHeight * 0.68);
				const minLogoY = screenTop + Math.round(screenHeight * 0.42);
				const maxLogoY = Math.min(canvasHeight - logoH, screenTop + screenHeight - logoH);
				const logoY = Math.max(
					screenTop,
					Math.min(maxLogoY, Math.max(minLogoY, logoYBase - Math.round(logoH / 2)))
				);

				const haloWidth = Math.max(1, Math.min(screenWidth, Math.round(Math.max(logoW, 1) * 1.6)));
				const haloHeight = Math.max(
					1,
					Math.min(screenHeight, Math.round(Math.max(logoH, 1) * 1.6))
				);
				const haloBuffer = await createGlow(haloWidth, haloHeight, '#ffffff', {
					opacity: 0.45,
					exponent: 3
				})
					.png()
					.toBuffer();
				const haloX = Math.max(
					0,
					Math.min(canvasWidth - haloWidth, screenLeft + Math.round((screenWidth - haloWidth) / 2))
				);
				const haloY = Math.max(
					screenTop,
					Math.min(
						screenTop + screenHeight - haloHeight,
						Math.max(screenTop, logoY + Math.round((logoH - haloHeight) / 2))
					)
				);
				composites.push({ input: haloBuffer, left: haloX, top: haloY, blend: 'screen' });
				composites.push({ input: await logo.toBuffer(), left: logoX, top: logoY });
			}
		} else if (hasLogo) {
			const fallbackLogo = sharp(logoPath).resize({
				height: Math.round(targetHeight * 0.26),
				fit: 'inside'
			});
			const { width: logoW = 0, height: logoH = 0 } = await fallbackLogo.metadata();
			const logoX = Math.round((canvasWidth - logoW) / 2);
			const logoY = Math.round((canvasHeight - logoH) / 2);
			composites.push({ input: await fallbackLogo.toBuffer(), left: logoX, top: logoY });
		}

		composites.push(frameComposite);

		await background.composite(composites).webp({ quality: 75 }).toFile(thumbPath);
		usedThumbs.add(thumbPath);

		devices[spec.id] = {
			title: spec.title,
			type: spec.type ?? 'phone',
			iframe: {
				width: scaled.iframeWidth,
				height: scaled.iframeHeight,
				marginTop: scaled.marginTop,
				marginLeft: scaled.marginLeft,
				borderRadius: scaled.borderRadius
			},
			bezel: {
				width: scaled.bezelWidth,
				height: scaled.bezelHeight
			},
			id: spec.id,
			index,
			category: spec.category ?? undefined
		};
		index += 1;
	}

	const entries = Object.entries(devices).sort(([, a], [, b]) => a.index - b.index);
	const deviceLines = entries.map(([id, cfg]) => `\t'${id}': ${toLiteral(cfg)}`).join(',\n');

	const bezelsSource = `/*
 * This file is generated by npm run update:bezels.
 * Update config/device-catalog.json then rerun the script.
 */
const devices = {
${deviceLines},
	none: {
		title: 'None',
		iframe: { width: '100%', height: '100%', position: 'relative' },
		bezel: { width: '100%', height: '100%', position: 'relative' },
		type: 'desktop',
		id: 'none',
		index: ${entries.length}
	}
};

export default devices;
`;

	await fs.writeFile(
		path.join(rootDir, 'src', 'lib', 'dashboard', 'Bezels.js'),
		bezelsSource,
		'utf8'
	);

	const existingImages = await fs.readdir(staticDir);
	for (const file of existingImages) {
		if (file === 'thumbnails') continue;
		const fullPath = path.join(staticDir, file);
		if (!usedImages.has(fullPath)) await fs.unlink(fullPath).catch(() => {});
	}

	const existingThumbs = await fs.readdir(thumbsDir);
	for (const file of existingThumbs) {
		const fullPath = path.join(thumbsDir, file);
		if (!usedThumbs.has(fullPath)) await fs.unlink(fullPath).catch(() => {});
	}

	console.log(`Updated bezels for ${entries.length} devices.`);
}

main().catch((err) => {
	console.error(err);
	process.exit(1);
});
