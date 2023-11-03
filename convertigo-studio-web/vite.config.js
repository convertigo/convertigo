import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import Icons from 'unplugin-icons/vite';

let c8oPort = '18080';
try {
	c8oPort = process.argv.filter(s => s.startsWith('--c8oPort=')).map(s => s.substring(10)).join();
} catch (e) {
	console.error('dynamic c8oPort failed', e);
}

function determineProxy() {
	const isWSL = process.platform !== 'win32' && process.env.WSL_DISTRO_NAME != undefined;
	if (isWSL) {
		// Configuration de proxy pour WSL2
		return `http://172.29.80.1:${c8oPort}`;
	}
	// Configuration par défaut pour d'autres environnements
	return `http://localhost:${c8oPort}`;
}

export default defineConfig({
	plugins: [
		sveltekit(),
		purgeCss(),
		Icons({
			compiler: 'svelte',
			autoInstall: true,
			defaultClass: 'ico'
		})
	],
	server: {
		proxy: {
			'/convertigo': {
				target: determineProxy(),
				ws: true
			}
		}
	}
});
