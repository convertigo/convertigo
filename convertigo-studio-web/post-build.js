import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import fsExtra from 'fs-extra';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const tmpDir = path.resolve(__dirname, '../eclipse-plugin-studio/tomcat/webapps/convertigo/tmp');
const targetDir = path.resolve(__dirname, '../eclipse-plugin-studio/tomcat/webapps/convertigo');

const PRESERVE = new Set([
	'axis2-web',
	'WEB-INF',
	'swagger',
	'xsl',
	'xsd',
	'oas',
	'admin_',
	'containers',
	'templates',
	'images',
	'css',
	'dtd',
	'licenses',
	'javadoc',
	'scripts'
]);

async function main() {
	if (!fs.existsSync(tmpDir)) {
		throw new Error(`Temporary build directory missing: ${tmpDir}`);
	}

	const entries = await fs.promises.readdir(tmpDir);

	for (const entry of entries) {
		if (PRESERVE.has(entry)) {
			console.log(`🟡 Skip preserved: ${entry}`);
			continue;
		}

		const src = path.join(tmpDir, entry);
		const dest = path.join(targetDir, entry);

		if (await fsExtra.pathExists(dest)) {
			await fsExtra.remove(dest);
		}

		await fsExtra.copy(src, dest, { overwrite: true, dereference: true });
		console.log(`✅ Copied: ${entry}`);
	}

	await fsExtra.remove(tmpDir);
	console.log('🧹 Cleaned tmp directory.');
}

main().catch((err) => {
	console.error('❌ Error during post-build:', err);
	process.exit(1);
});
