import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import Icons from 'unplugin-icons/vite';

function determineProxy() {
	const isWSL = process.platform !== 'win32' && process.env.WSL_DISTRO_NAME != undefined;
	if (isWSL) {
		// Configuration de proxy pour WSL2
		return 'http://172.29.80.1:18080';
	}
	// Configuration par défaut pour d'autres environnements
	return 'http://localhost:18080';
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
			'/convertigo': determineProxy()
		}
	}
});
