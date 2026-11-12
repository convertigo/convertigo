// Links resources/<target>/jre, the Java runtime of the desktop Studio for a target, with the jlink
// of a Temurin JDK and the jmods of the target: one machine links the runtimes of every target. The
// Temurin release is the one of the Eclipse Studio (installers/nsis/download-jdk.sh); jlink needs
// the JDK and the jmods of the same release.
//
// Usage: node scripts/jre.mjs [<target>... | all]   (the host target by default)
// JLINK_JDK may name a JDK of that release to link with, rather than the downloaded one.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

import { cacheDir, download, extract, hostTarget, jdkRelease, onlyChild, parseTarget, resourcesDir, run, targetsOf } from './common.mjs';

// the modules of the runtime (jdk.crypto.ec is gone since Java 24, its algorithms are in java.base)
const MODULES = [
	'java.se',
	'jdk.accessibility',
	'jdk.attach',
	'jdk.charsets',
	'jdk.compiler',
	'jdk.crypto.cryptoki',
	'jdk.dynalink',
	'jdk.httpserver',
	'jdk.internal.jvmstat',
	'jdk.internal.opt',
	'jdk.jdi',
	'jdk.jdwp.agent',
	'jdk.jfr',
	'jdk.jsobject',
	'jdk.localedata',
	'jdk.management',
	'jdk.management.agent',
	'jdk.naming.dns',
	'jdk.naming.rmi',
	'jdk.net',
	'jdk.security.auth',
	'jdk.security.jgss',
	'jdk.unsupported',
	'jdk.xml.dom',
	'jdk.zipfs'
];
// the certificates of Windows, as the full JDK of the Eclipse Studio has them
const PLATFORM_MODULES = { win32: ['jdk.crypto.mscapi'] };

const release = jdkRelease();
const [version, build] = release.split('+');
const major = version.split('.')[0];
const baseUrl = `https://github.com/adoptium/temurin${major}-binaries/releases/download/jdk-${encodeURIComponent(release)}`;

/**
 * @param {string} target
 * @returns {{ os: string, arch: string, ext: string }} the target as Temurin names it
 */
function temurinTarget(target) {
	const { platform, arch } = parseTarget(target);
	return {
		os: { darwin: 'mac', win32: 'windows', linux: 'linux' }[platform],
		arch: { arm64: 'aarch64', x64: 'x64' }[arch],
		ext: platform === 'win32' ? 'zip' : 'tar.gz'
	};
}

/**
 * Downloads and extracts a Temurin archive of the release, once.
 * @param {'jdk' | 'jmods'} type
 * @param {string} target
 * @returns {Promise<string>} the folder it extracted
 */
async function temurin(type, target) {
	const { os, arch, ext } = temurinTarget(target);
	const name = `OpenJDK${major}U-${type}_${arch}_${os}_hotspot_${version}_${build}.${ext}`;
	const dir = path.join(cacheDir, 'temurin', release);
	const archive = path.join(dir, name);
	const extracted = path.join(dir, name.replace(/\.(zip|tar\.gz)$/, ''));
	const stamp = path.join(extracted, '.extracted');
	await download(`${baseUrl}/${name}`, archive, { algorithm: 'sha256', url: `${baseUrl}/${name}.sha256.txt` });
	if (!fs.existsSync(stamp) || fs.statSync(stamp).mtimeMs < fs.statSync(archive).mtimeMs) {
		extract(archive, extracted);
		fs.writeFileSync(stamp, '');
	}
	return extracted;
}

/** @returns {Promise<string>} the jlink of the release, for this machine */
async function jlink() {
	const exe = process.platform === 'win32' ? 'jlink.exe' : 'jlink';
	if (process.env.JLINK_JDK) {
		return path.join(process.env.JLINK_JDK, 'bin', exe);
	}
	const root = onlyChild(await temurin('jdk', hostTarget));
	const home = process.platform === 'darwin' ? path.join(root, 'Contents', 'Home') : root;
	return path.join(home, 'bin', exe);
}

/**
 * Replaces the symbolic links of the runtime by copies of their files: jlink links the legal notices
 * shared by the modules with absolute paths, which break once moved and which codesign refuses.
 * @param {string} dir
 */
function copySymbolicLinks(dir) {
	for (const entry of fs.readdirSync(dir, { recursive: true, withFileTypes: true })) {
		const file = path.join(entry.parentPath, entry.name);
		if (entry.isSymbolicLink()) {
			const real = fs.realpathSync(file);
			fs.rmSync(file);
			fs.cpSync(real, file, { recursive: true });
		}
	}
}

const linker = await jlink();
for (const target of targetsOf(process.argv.slice(2))) {
	const { platform } = parseTarget(target);
	const jmods = await temurin('jmods', target);
	const modulePath = path.dirname(
		fs.readdirSync(jmods, { recursive: true }).map((name) => path.join(jmods, String(name))).find((file) => path.basename(file) === 'java.base.jmod') ??
			(() => {
				throw new Error(`No java.base.jmod in ${jmods}`);
			})()
	);
	const output = path.join(resourcesDir, target, 'jre');
	fs.rmSync(output, { recursive: true, force: true });
	fs.mkdirSync(path.dirname(output), { recursive: true });
	console.log(`Linking the Java runtime ${release} of ${target}`);
	run(linker, [
		'--module-path',
		modulePath,
		'--add-modules',
		[...MODULES, ...(PLATFORM_MODULES[platform] ?? [])].join(','),
		'--no-header-files',
		'--no-man-pages',
		// the classes compressed, their debug information kept for the stack traces
		'--compress=zip-6',
		'--output',
		output
	]);
	copySymbolicLinks(output);
	if (platform !== 'win32') {
		// the commands and the helper that starts the processes of the engine (git, node of the NGX builds)
		const executables = [
			...fs.readdirSync(path.join(output, 'bin')).map((name) => path.join(output, 'bin', name)),
			path.join(output, 'lib', 'jspawnhelper')
		];
		for (const file of executables.filter((file) => fs.existsSync(file))) {
			fs.chmodSync(file, 0o755);
		}
	}
	if (target === hostTarget) {
		execFileSync(path.join(output, 'bin', platform === 'win32' ? 'java.exe' : 'java'), ['-version'], { stdio: 'inherit' });
	}
	console.log(output);
}
