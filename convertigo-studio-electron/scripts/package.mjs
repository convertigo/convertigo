// Packages the Electron Studio for a target with @electron/packager, from resources/engine and
// resources/<target>/jre, then writes its archive in dist:
// - macOS: signed with the Developer ID of APPLE_SIGN_IDENTITY, then notarized and stapled when
//   APPLE_ID, APPLE_PASSWORD and APPLE_TEAM_ID are set; signed ad hoc otherwise. macOS only.
// - Windows: its executable signed with the certificate of WINDOWS_PFX (WINDOWS_PFX_PASSWORD) when set.
// - Linux: not signed.
//
// Usage: node scripts/package.mjs [<target>... | all]   (the host target by default)
import { packager } from '@electron/packager';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { convertigoVersion, electronDir, filesOf, hasCommand, isMachO, parseTarget, repoDir, resourcesDir, run, targetsOf } from './common.mjs';

const NAME = 'Convertigo Studio';
const version = convertigoVersion();
const distDir = path.join(electronDir, 'dist');
// the entitlements of the Java runtime of the Eclipse Studio: its JIT, and the native libraries the
// engine extracts from its jars, signed by others
const JAVA_ENTITLEMENTS = path.join(repoDir, 'installers', 'entitlements.plist');
const TIMESTAMP_URL = process.env.WINDOWS_TIMESTAMP_URL || 'http://timestamp.digicert.com';

/** @type {Record<string, string>} the archive of each target, as the Eclipse Studio names them */
const ARCHIVES = {
	'darwin-arm64': `convertigo-studio-electron-${version.full}-macosx-arm64.zip`,
	'darwin-x64': `convertigo-studio-electron-${version.full}-macosx-x86_64.zip`,
	'win32-x64': `convertigo-studio-electron-${version.full}-win64.zip`,
	'linux-x64': `convertigo-studio-electron-${version.full}-linux64.tar.gz`
};

/**
 * Signs the macOS native libraries in the jars of the engine, which the notarization inspects too.
 * The jars are not signed, so their entries can change.
 * @param {string} buildPath
 * @param {string} identity
 */
function signJarNatives(buildPath, identity) {
	const [bundle] = fs.readdirSync(buildPath).filter((name) => name.endsWith('.app'));
	const libDir = path.join(buildPath, bundle, 'Contents', 'Resources', 'engine', 'war', 'WEB-INF', 'lib');
	for (const jar of fs.readdirSync(libDir).filter((name) => name.endsWith('.jar'))) {
		const jarPath = path.join(libDir, jar);
		const natives = execFileSync('unzip', ['-Z1', jarPath], { maxBuffer: 256 << 20 })
			.toString()
			.split('\n')
			.filter((entry) => /\.(dylib|jnilib)$/.test(entry));
		if (!natives.length) {
			continue;
		}
		const work = fs.mkdtempSync(path.join(os.tmpdir(), 'convertigo-jar-'));
		try {
			run('unzip', ['-q', jarPath, ...natives, '-d', work]);
			for (const entry of natives) {
				const file = path.join(work, entry);
				if (isMachO(file)) {
					run('codesign', ['--force', '--timestamp', '--options', 'runtime', '--sign', identity, file]);
				}
			}
			run('zip', ['-q', jarPath, ...natives], { cwd: work });
		} finally {
			fs.rmSync(work, { recursive: true, force: true });
		}
	}
}

/**
 * @param {string} target
 * @returns {Partial<import('@electron/packager').Options>} the signature and the notarization of a macOS
 * application with a Developer ID, or nothing for an ad hoc signature
 */
function macSigning(target) {
	const identity = process.env.APPLE_SIGN_IDENTITY;
	if (!identity) {
		console.log(`${target}: no APPLE_SIGN_IDENTITY, the application is signed ad hoc and not notarized`);
		return {};
	}
	const { APPLE_ID, APPLE_PASSWORD, APPLE_TEAM_ID } = process.env;
	const notarize = Boolean(APPLE_ID && APPLE_PASSWORD && APPLE_TEAM_ID);
	if (!notarize) {
		console.log(`${target}: no APPLE_ID, APPLE_PASSWORD or APPLE_TEAM_ID, the application is not notarized`);
	}
	return {
		afterCopyExtraResources: [({ buildPath }) => signJarNatives(buildPath, identity)],
		osxSign: {
			identity,
			continueOnError: false,
			// the Mach-O binaries only, rather than every file of the engine
			ignore: (file) => fs.statSync(file).isFile() && !isMachO(file),
			optionsForFile: (file) =>
				file.includes(`${path.sep}Contents${path.sep}Resources${path.sep}jre${path.sep}`)
					? { hardenedRuntime: true, entitlements: JAVA_ENTITLEMENTS }
					: {}
		},
		...(notarize
			? { osxNotarize: { appleId: APPLE_ID, appleIdPassword: APPLE_PASSWORD, teamId: APPLE_TEAM_ID } }
			: {})
	};
}

/**
 * Signs an application ad hoc, so that macOS runs it once its quarantine is lifted: the Java runtime
 * first, which the application then seals.
 * @param {string} bundle
 */
function signAdHoc(bundle) {
	for (const file of filesOf(path.join(bundle, 'Contents', 'Resources', 'jre')).filter(isMachO)) {
		run('codesign', ['--force', '--sign', '-', file]);
	}
	run('codesign', ['--force', '--deep', '--sign', '-', bundle]);
}

/**
 * Signs the executable of the Windows application, with signtool on Windows and osslsigncode elsewhere.
 * @param {string} exe
 */
function signWindows(exe) {
	const { WINDOWS_PFX, WINDOWS_PFX_PASSWORD = '' } = process.env;
	if (process.platform === 'win32') {
		run('signtool', ['sign', '/f', WINDOWS_PFX, '/p', WINDOWS_PFX_PASSWORD, '/fd', 'sha256', '/tr', TIMESTAMP_URL, '/td', 'sha256', exe]);
		return;
	}
	const signed = `${exe}.signed`;
	run('osslsigncode', ['sign', '-pkcs12', WINDOWS_PFX, '-pass', WINDOWS_PFX_PASSWORD, '-h', 'sha256', '-t', TIMESTAMP_URL, '-in', exe, '-out', signed]);
	fs.renameSync(signed, exe);
}

/**
 * @param {string} target
 */
async function packageTarget(target) {
	const { platform, arch } = parseTarget(target);
	const jre = path.join(resourcesDir, target, 'jre');
	for (const required of [path.join(resourcesDir, 'engine', 'war'), jre, path.join(resourcesDir, 'icon.png')]) {
		if (!fs.existsSync(required)) {
			throw new Error(`${required} is missing: run scripts/engine.mjs and scripts/jre.mjs ${target} first`);
		}
	}
	if (platform === 'darwin' && process.platform !== 'darwin') {
		throw new Error(`${target} is packaged and signed on macOS only`);
	}
	const staging = path.join(distDir, '.packaging');
	fs.rmSync(staging, { recursive: true, force: true });
	const [stagedPath] = await packager({
		dir: electronDir,
		name: NAME,
		platform,
		arch,
		out: staging,
		overwrite: true,
		appVersion: version.number,
		buildVersion: version.number,
		appCopyright: `Copyright © ${new Date().getFullYear()} Convertigo`,
		icon: {
			darwin: path.join(repoDir, 'eclipse-repository', 'images', 'convertigo.icns'),
			win32: path.join(repoDir, 'installers', 'data', 'convertigo.ico'),
			linux: undefined
		}[platform],
		...(platform === 'linux' ? { executableName: 'convertigo-studio' } : {}),
		appBundleId: 'com.convertigo.studio.electron',
		appCategoryType: 'public.app-category.developer-tools',
		win32metadata: { CompanyName: 'Convertigo', ProductName: NAME, FileDescription: NAME, InternalName: NAME },
		extraResource: [path.join(resourcesDir, 'engine'), jre, path.join(resourcesDir, 'icon.png')],
		// the application is main.js and preload.js; its dependencies are the build tools, left out
		ignore: [/^\/(resources|dist|scripts|engine|\.cache)($|\/)/, /^\/(README\.md|\.gitignore)$/, /\.log$/],
		...(platform === 'darwin' ? macSigning(target) : {})
	});
	const appPath = path.join(distDir, path.basename(stagedPath));
	fs.rmSync(appPath, { recursive: true, force: true });
	fs.renameSync(stagedPath, appPath);
	fs.rmSync(staging, { recursive: true, force: true });

	const archive = path.join(distDir, ARCHIVES[target]);
	fs.rmSync(archive, { force: true });
	if (platform === 'darwin') {
		const bundle = path.join(appPath, `${NAME}.app`);
		if (!process.env.APPLE_SIGN_IDENTITY) {
			signAdHoc(bundle);
		}
		run('codesign', ['--verify', '--deep', '--strict', bundle]);
		run('ditto', ['-c', '-k', '--sequesterRsrc', '--keepParent', bundle, archive]);
	} else if (platform === 'win32') {
		if (process.env.WINDOWS_PFX) {
			signWindows(path.join(appPath, `${NAME}.exe`));
		} else {
			console.log(`${target}: no WINDOWS_PFX, the executable is not signed`);
		}
		if (hasCommand('zip')) {
			run('zip', ['-q', '-r', '-y', archive, path.basename(appPath)], { cwd: distDir });
		} else {
			run('tar', ['-a', '-c', '-f', archive, path.basename(appPath)], { cwd: distDir });
		}
	} else {
		run('tar', ['-c', '-z', '-f', archive, path.basename(appPath)], { cwd: distDir });
	}
	console.log(`${appPath}\n${archive} (${Math.round(fs.statSync(archive).size / 1024 / 1024)} MB)`);
}

for (const target of targetsOf(process.argv.slice(2))) {
	await packageTarget(target);
}
