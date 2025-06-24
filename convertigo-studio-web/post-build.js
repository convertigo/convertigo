import { readdir } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import fsExtra from 'fs-extra';

// 📍 Pour obtenir __dirname en ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Dossiers
const tmpDir = path.resolve(__dirname, '../eclipse-plugin-studio/tomcat/webapps/convertigo/tmp');
const targetDir = path.resolve(__dirname, '../eclipse-plugin-studio/tomcat/webapps/convertigo');

// Fichiers et dossiers à **ne pas écraser**
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

const run = async () => {
	const tmpFiles = await readdir(tmpDir);

	for (const file of tmpFiles) {
		if (PRESERVE.has(file)) {
			console.log(`🟡 Skip preserved: ${file}`);
			continue;
		}

		const src = path.join(tmpDir, file);
		const dest = path.join(targetDir, file);

		if (await fsExtra.pathExists(dest)) {
			await fsExtra.remove(dest);
		}

		await fsExtra.move(src, dest);
		console.log(`✅ Moved: ${file}`);
	}

	await fsExtra.remove(tmpDir);
	console.log('🧹 Cleaned tmp directory.');
};

run().catch((err) => {
	console.error('❌ Error during post-build:', err);
	process.exit(1);
});
