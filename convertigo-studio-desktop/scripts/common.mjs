// Shared helpers of the build scripts of the desktop Studio: the versions the repository pins, the
// targets, downloads checked against their published checksums, archives and commands.
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

/** the folder of the desktop Studio */
export const desktopDir = path.resolve(import.meta.dirname, '..');
/** the root of the Convertigo repository */
export const repoDir = path.resolve(desktopDir, '..');
/** the resources the application packages: the engine, a Java runtime per target and the icon */
export const resourcesDir = path.join(desktopDir, 'resources');
/** the downloads, kept between builds */
export const cacheDir = process.env.CONVERTIGO_STUDIO_DESKTOP_CACHE || path.join(desktopDir, '.cache');

/** the targets the application is packaged for, as `<platform>-<arch>` of Node.js and Electron */
export const TARGETS = ['darwin-arm64', 'darwin-x64', 'win32-x64', 'linux-x64'];

export const hostTarget = `${process.platform}-${process.arch}`;

/**
 * @param {string} target
 * @returns {{ platform: 'darwin' | 'win32' | 'linux', arch: 'arm64' | 'x64' }}
 */
export function parseTarget(target) {
	if (!TARGETS.includes(target)) {
		throw new Error(`Unknown target ${target}, expected one of ${TARGETS.join(', ')}`);
	}
	const [platform, arch] = target.split('-');
	return { platform: /** @type {any} */ (platform), arch: /** @type {any} */ (arch) };
}

/**
 * @param {string[]} args the targets given on the command line, `all`, or none for the host
 * @returns {string[]}
 */
export function targetsOf(args) {
	if (args.includes('all')) {
		return TARGETS;
	}
	return args.length ? args.map((target) => (parseTarget(target), target)) : [hostTarget];
}

/**
 * @param {string} name a `ext.<name> = '<value>'` property of the root build.gradle
 * @returns {string}
 */
export function gradleProperty(name) {
	const text = fs.readFileSync(path.join(repoDir, 'build.gradle'), 'utf8');
	const match = text.match(new RegExp(`^ext\\.${name}\\s*=\\s*'([^']*)'`, 'm'));
	if (!match) {
		throw new Error(`No ext.${name} in build.gradle`);
	}
	return match[1];
}

/** @returns {{ number: string, full: string }} the version of Convertigo, as build.gradle sets it */
export function convertigoVersion() {
	const number = gradleProperty('convertigoVersion');
	const tag = gradleProperty('convertigoTag');
	return { number, full: tag ? `${number}-${tag}` : number };
}

/** @returns {string} the Temurin release of the Studio, as installers/nsis/download-jdk.sh pins it */
export function jdkRelease() {
	const script = fs.readFileSync(path.join(repoDir, 'installers', 'nsis', 'download-jdk.sh'), 'utf8');
	const match = script.match(/^JDK_RELEASE=(\S+)/m);
	if (!match) {
		throw new Error('No JDK_RELEASE in installers/nsis/download-jdk.sh');
	}
	return match[1];
}

/**
 * Runs a command, its output shown.
 * @param {string} command
 * @param {string[]} args
 * @param {import('node:child_process').ExecFileSyncOptions} [options]
 */
export function run(command, args, options = {}) {
	execFileSync(command, args, { stdio: 'inherit', ...options });
}

/**
 * @param {string} command
 * @returns {boolean} whether the command is on the PATH
 */
export function hasCommand(command) {
	try {
		execFileSync(process.platform === 'win32' ? 'where' : 'which', [command], { stdio: 'ignore' });
		return true;
	} catch {
		return false;
	}
}

/**
 * @param {string} file
 * @param {string} algorithm
 * @returns {string} the hexadecimal digest of the file
 */
export function digest(file, algorithm) {
	const hash = crypto.createHash(algorithm);
	const fd = fs.openSync(file, 'r');
	try {
		const buffer = Buffer.alloc(1 << 20);
		let read;
		while ((read = fs.readSync(fd, buffer, 0, buffer.length, null)) > 0) {
			hash.update(buffer.subarray(0, read));
		}
	} finally {
		fs.closeSync(fd);
	}
	return hash.digest('hex');
}

/**
 * @param {string} url
 * @returns {Promise<string>}
 */
export async function fetchText(url) {
	const response = await fetch(url);
	if (!response.ok) {
		throw new Error(`${url}: HTTP ${response.status}`);
	}
	return response.text();
}

/**
 * Downloads a file once, checked against the digest published next to it. A file already in the
 * cache is checked again rather than downloaded.
 * @param {string} url
 * @param {string} file
 * @param {{ algorithm: string, url: string }} checksum the digest file of the download
 */
export async function download(url, file, checksum) {
	const expected = (await fetchText(checksum.url)).trim().split(/\s+/)[0].toLowerCase();
	if (!/^[0-9a-f]{32,128}$/.test(expected)) {
		throw new Error(`${checksum.url} holds no ${checksum.algorithm} digest`);
	}
	if (fs.existsSync(file) && digest(file, checksum.algorithm) === expected) {
		return;
	}
	console.log(`Downloading ${url}`);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	const response = await fetch(url);
	if (!response.ok || !response.body) {
		throw new Error(`${url}: HTTP ${response.status}`);
	}
	const partial = `${file}.part`;
	await fs.promises.writeFile(partial, response.body);
	const actual = digest(partial, checksum.algorithm);
	if (actual !== expected) {
		fs.rmSync(partial, { force: true });
		throw new Error(`${url}: ${checksum.algorithm} ${actual} instead of ${expected}`);
	}
	fs.renameSync(partial, file);
}

/**
 * Extracts a zip (war, jar) or tar.gz archive into a folder, emptied first.
 * @param {string} archive
 * @param {string} dir
 */
export function extract(archive, dir) {
	fs.rmSync(dir, { recursive: true, force: true });
	fs.mkdirSync(dir, { recursive: true });
	if (/\.(zip|war|jar)$/.test(archive) && hasCommand('unzip')) {
		run('unzip', ['-q', '-o', archive, '-d', dir]);
	} else {
		// the tar of macOS and Windows reads the zip archives too
		run('tar', ['-xf', archive, '-C', dir]);
	}
}

/**
 * @param {string} dir
 * @returns {string} the only folder in a folder, as the root of an extracted JDK
 */
export function onlyChild(dir) {
	const children = fs.readdirSync(dir).filter((name) => !name.startsWith('.'));
	if (children.length !== 1) {
		throw new Error(`${dir} holds ${children.length} entries instead of one folder`);
	}
	return path.join(dir, children[0]);
}

/**
 * @param {string} file
 * @returns {boolean} whether the file is a Mach-O binary, thin or fat (a Java class shares the magic
 * number of the fat ones, but not their small count of architectures)
 */
export function isMachO(file) {
	const header = Buffer.alloc(8);
	const fd = fs.openSync(file, 'r');
	let read;
	try {
		read = fs.readSync(fd, header, 0, 8, 0);
	} finally {
		fs.closeSync(fd);
	}
	if (read < 8) {
		return false;
	}
	const magic = header.readUInt32BE(0);
	if ([0xfeedface, 0xfeedfacf, 0xcefaedfe, 0xcffaedfe].includes(magic)) {
		return true;
	}
	return (magic === 0xcafebabe || magic === 0xcafebabf) && header.readUInt32BE(4) < 20;
}

/**
 * @param {string} dir
 * @returns {string[]} the files of a folder and its subfolders, symbolic links left out
 */
export function filesOf(dir) {
	return fs
		.readdirSync(dir, { recursive: true, withFileTypes: true })
		.filter((entry) => entry.isFile())
		.map((entry) => path.join(entry.parentPath, entry.name));
}
